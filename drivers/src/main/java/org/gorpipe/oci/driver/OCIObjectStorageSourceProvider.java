package org.gorpipe.oci.driver;

import com.google.auto.service.AutoService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.*;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageAsync;
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.StreamSourceProvider;
import org.gorpipe.gor.driver.utils.CredentialClientCache;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;
import org.gorpipe.gor.util.StringUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@AutoService(SourceProvider.class)
public class OCIObjectStorageSourceProvider extends StreamSourceProvider {
    private static Logger log = org.slf4j.LoggerFactory.getLogger(OCIObjectStorageSourceProvider.class);

    public static final String OCI_AUTH_TYPE_INSTANCE_PRINCIPAL = "INSTANCE_PRINCIPAL";
    public static final String OCI_AUTH_TYPE_AUTH_TOKEN = "AUTH_TOKEN";
    public static final String OCI_AUTH_TYPE_CONFIG_FILE = "CONFIG_FILE";
    public static final String OCI_AUTH_TYPE_SIMPLE = "SIMPLE";

    public static final String OCI_AUTH_TYPE = System.getProperty("gor.oci.auth.type", OCI_AUTH_TYPE_SIMPLE);

    private static final String OCI_CONFIG_FILE = System.getProperty("gor.oci.config.file", "~/.oci/config");
    private static final String OCI_CONFIG_PROFILE = System.getProperty("gor.oci.config.profile", "DEFAULT");

    private static final String OCI_TENANT = System.getProperty("gor.oci.tenant", "");
    private static final String OCI_USER = System.getProperty("gor.oci.user", "");

    private static final String OCI_AUTH_TOKEN_KEY_FILE =
            System.getProperty("gor.oci.auth.key.file", "~/.oci/sessions/DEFAULT/oci_api_key.pem").replaceAll("\\\\n", "\n");
    private static final String OCI_AUTH_TOKEN_KEY = System.getProperty("gor.oci.auth.key", "");
    private static final String OCI_AUTH_TOKEN_JWT = System.getProperty("gor.oci.auth.jwt", "");

    private static final String OCI_SIMPLE_PRIVATE_KEY =
            System.getProperty("gor.oci.simple.privatekey","");
    private static final String OCI_SIMPLE_FINGERPRINT = System.getProperty("gor.oci.simple.fingerprint","");

    private static final Cache<String, ObjectStorage> clientCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    private static final CredentialClientCache<ObjectStorageAsync> clientCredCache =
            new CredentialClientCache<>(OCIObjectStorageSourceType.OCI_OBJECT_STORAGE.getName(),
                    OCIObjectStorageSourceProvider::createClientAsync);


    public OCIObjectStorageSourceProvider() {
    }

    public OCIObjectStorageSourceProvider(GorDriverConfig config, FileCache cache,
                                          Set<StreamSourceIteratorFactory> initialFactories) {
        super(config, cache, initialFactories);
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return new SourceType[]{OCIObjectStorageSourceType.OCI_OBJECT_STORAGE};
    }

    @Override
    public OCIObjectStorageSource resolveDataSource(SourceReference sourceReference)
            throws IOException {
        ObjectStorageAsync client = getClient(sourceReference.getSecurityContext(), sourceReference.getUrl());
        return new OCIObjectStorageSource(client, sourceReference);
    }

    @Override
    protected RetryHandlerBase getRetryHandler() {
        if (retryHandler == null) {
            retryHandler = new OCIObjectStorageRetryHandler(config.retryInitialSleep().toMillis(), config.retryMaxSleep().toMillis());
        }
        return retryHandler;
    }

    protected ObjectStorageAsync getClient(String securityContext, String url) throws IOException {
        BundledCredentials creds = BundledCredentials.fromSecurityContext(securityContext);
        OCIUrl ociUrl = OCIUrl.parse(url);
        return clientCredCache.getClient(creds, ociUrl.getLookupKey());
    }

    public static ObjectStorageAsync createClientAsync(Credentials cred)  {
        AuthData authData = AuthData.from(cred);
        AbstractAuthenticationDetailsProvider provider = findAuthenticationProvider(authData);

        ClientConfiguration clientConfig
                = ClientConfiguration.builder()
                .connectionTimeoutMillis(3000)
                .readTimeoutMillis(60000)
                .maxAsyncThreads(10000)
                .build();

        return ObjectStorageAsyncClient.builder()
                    .endpoint(authData.endPoint)
                    .region(authData.region)
                    .configuration(clientConfig)
                    .build(provider);
    }

    public static ObjectStorage createClientSync(Credentials cred)  {
        AuthData authData = AuthData.from(cred);
        AbstractAuthenticationDetailsProvider provider = findAuthenticationProvider(authData);

        ClientConfiguration clientConfig
                = ClientConfiguration.builder()
                .connectionTimeoutMillis(3000)
                .readTimeoutMillis(60000)
                .maxAsyncThreads(10000)
                .build();

        return ObjectStorageClient.builder()
                .endpoint(authData.endPoint)
                .region(authData.region)
                .configuration(clientConfig)
                .build(provider);
    }

    private static AbstractAuthenticationDetailsProvider findAuthenticationProvider(AuthData auth) {
        return switch (OCI_AUTH_TYPE) {
            case OCI_AUTH_TYPE_INSTANCE_PRINCIPAL -> getInstancePrincipalAuthenticationProvider();
            case OCI_AUTH_TYPE_AUTH_TOKEN -> getAuthTokenAuthenticationProvider();
            case OCI_AUTH_TYPE_CONFIG_FILE -> getConfigFileAuthenticationProvider();
            case OCI_AUTH_TYPE_SIMPLE -> getSimpleAuthenticationProvider(auth);
            default -> throw new GorSystemException("Unknown OCI authentication type: " + OCI_AUTH_TYPE);
        };
    }

    private static AbstractAuthenticationDetailsProvider getConfigFileAuthenticationProvider() {
        final ConfigFileReader.ConfigFile configFile;
        try {
            configFile = ConfigFileReader.parse(OCI_CONFIG_FILE, OCI_CONFIG_PROFILE);
        } catch (IOException e) {
            throw new GorSystemException(e);
        }

        final AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);
        return provider;
    }

    private static AbstractAuthenticationDetailsProvider getAuthTokenAuthenticationProvider() {
        var keyFile = OCI_AUTH_TOKEN_KEY_FILE;
        if (!StringUtil.isEmpty(OCI_AUTH_TOKEN_KEY)) {
            Path keyFilePath = null;
            try {
                keyFilePath = Files.createTempFile("oci_api_key", ".pem").toAbsolutePath();
                Files.writeString(keyFilePath, OCI_AUTH_TOKEN_KEY);
                keyFile = keyFilePath.toString();
            } catch (IOException e) {
                log.warn("Could not create temporary key file for OCI auth token, fallback to key file", e);
            }
        }

        SessionTokenAuthenticationDetailsProvider provider = null;
        try {
            provider = SessionTokenAuthenticationDetailsProvider.builder()
                    .region(OCIUrl.DEFAULT_REGION)
                    .tenantId(OCI_TENANT)
                    .privateKeyFilePath(keyFile)
                    .sessionToken(OCI_AUTH_TOKEN_JWT)
                    .sessionTokenFilePath( // Sometimes needed, otherwise we get null pointer ex.
                            Files.createTempFile("oci_session_token", ".jwt").toAbsolutePath().toString())
                    .build();
        } catch (IOException e) {
            throw new GorResourceException("Failed to create authentication provider", "", e);
        }

        // Stop the token update thread.
        //provider.close();

        return provider;
    }

    private static AbstractAuthenticationDetailsProvider getSimpleAuthenticationProvider(AuthData authData) {
        return SimpleAuthenticationDetailsProvider.builder()
                .tenantId(authData.tenantId)
                .userId(authData.userId)
                .fingerprint(authData.fingerprint)
                .privateKeySupplier(new StringPrivateKeySupplier(authData.privateKey))
                .passPhrase("")
                .build();
    }

    private static AbstractAuthenticationDetailsProvider getInstancePrincipalAuthenticationProvider() {
        final InstancePrincipalsAuthenticationDetailsProvider provider;
        try {
            provider = InstancePrincipalsAuthenticationDetailsProvider.builder().build();
        } catch (Exception e) {
            if (e.getCause() instanceof SocketTimeoutException || e.getCause() instanceof ConnectException) {
            }
            throw new GorResourceException("Failed to create instance principal authentication provider", "", e);
        }
        return provider;
    }

    private record AuthData(Region region, String userId, String tenantId, String privateKey, String fingerprint, String endPoint) {
        private static final Pattern REGION_PATTERN = Pattern.compile(".*?\\.objectstorage\\.(.*?)\\..*");
        public static AuthData from(Credentials creds) {
            if (creds != null && !creds.isNull()) {
                var endPoint = getOrDefault(creds, Credentials.Attr.API_ENDPOINT, OCIUrl.DEFAULT_OCI_ENDPOINT);
                var region = getRegion(creds, endPoint);
                var tenantId = getOrDefault(creds, Credentials.Attr.REALM, OCI_TENANT);
                var userId = creds.get(Credentials.Attr.SCOPE);
                var fingerprint = getOrDefault(creds, Credentials.Attr.KEY, OCI_SIMPLE_FINGERPRINT);
                var privateKey = getOrDefault(creds, Credentials.Attr.SECRET, OCI_SIMPLE_PRIVATE_KEY).replaceAll("\\\\n", "\n");

                return new AuthData(region, userId, tenantId, privateKey, fingerprint, endPoint);
            } else {
                return new AuthData(Region.US_ASHBURN_1, OCI_USER, OCI_TENANT, OCI_SIMPLE_PRIVATE_KEY, OCI_SIMPLE_FINGERPRINT, OCIUrl.DEFAULT_OCI_ENDPOINT);
            }
        }

        static Region getRegion(Credentials creds, String endpoint) {
            var regionStr = creds.getOrDefault(Credentials.Attr.REGION, "");

            // Extract region from OCI endpoint
            if (StringUtil.isEmpty(regionStr) && !StringUtil.isEmpty(endpoint)) {
                var m = REGION_PATTERN.matcher(endpoint);
                regionStr = m.matches() ? m.group(1) : "";
            }

            return StringUtil.isEmpty(regionStr) ? OCIUrl.DEFAULT_REGION : Region.valueOf(regionStr);
        }
    }

    static String getOrDefault(Credentials cred, Credentials.Attr attr, String defaultValue) {
        var credValue = cred != null && !cred.isNull() ? cred.get(attr) : null;
        return StringUtil.isEmpty(credValue) ? defaultValue : credValue;
    }
}

