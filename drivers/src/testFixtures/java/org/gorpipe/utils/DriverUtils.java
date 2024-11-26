package org.gorpipe.utils;

import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static org.gorpipe.oci.driver.OCIUrl.DEFAULT_OCI_ENDPOINT;

public class DriverUtils {

    private static final Logger log = LoggerFactory.getLogger(DriverUtils.class);

    public static String SECRETS_FILE_NAME = "../tests/config/secrets.env";

    public static Properties getDriverProperties() {
        Properties prop = new Properties();
        if (Files.exists(Paths.get(SECRETS_FILE_NAME))) {
            log.debug("Loading env: " + SECRETS_FILE_NAME);
            try (InputStream inputStream = new FileInputStream(SECRETS_FILE_NAME)) {
                prop.load(inputStream);
            } catch (IOException e) {
                // Do nothing
                log.warn("Error reading secrets file", e);
            }
        } else {
            log.warn("No secrets file found {}", SECRETS_FILE_NAME);
        }

        prop.putAll(System.getenv());

        return prop;
    }

    public static String createSecurityContext(String service, Credentials.OwnerType ownerType, String owner, String S3_KEY, String S3_SECRET) {
        Credentials creds = new Credentials.Builder()
                .service(service)
                .lookupKey("gdb-unit-test-data")
                .ownerType(ownerType)
                .ownerId(owner)
                .set(Credentials.Attr.KEY, S3_KEY)
                .set(Credentials.Attr.SECRET, S3_SECRET)
                .set(Credentials.Attr.REGION, "eu-west-1")
                .build();
        BundledCredentials bundleCreds = new BundledCredentials.Builder().addCredentials(creds).build();
        return bundleCreds.addToSecurityContext("");
    }

    public static String createSecurityContext(String service, String lookupKey, Credentials.OwnerType ownerType,
                                               String owner, String key, String secret, String apiEndpoint,
                                               String scope) {
        Credentials creds = new Credentials.Builder()
                .service(service)
                .lookupKey(lookupKey)
                .ownerType(ownerType)
                .ownerId(owner)
                .set(Credentials.Attr.KEY, key)
                .set(Credentials.Attr.SECRET, secret)
                .set(Credentials.Attr.API_ENDPOINT, apiEndpoint)
                .set(Credentials.Attr.SCOPE, scope)
                .build();
        BundledCredentials bundleCreds = new BundledCredentials.Builder().addCredentials(creds).build();
        return bundleCreds.addToSecurityContext("");
    }

    public static String awsSecurityContext(String key, String secret) {
        // Credentials for gor_unittest user in nextcode AWS account
        Credentials cred = new Credentials.Builder().service("s3")
                .lookupKey("gdb-unit-test-data")
                //.set(Credentials.Attr.REGION, "eu-west-1")
                .set(Credentials.Attr.API_ENDPOINT, "https://s3-eu-west-1.amazonaws.com/")
                .set(Credentials.Attr.KEY, key)
                .set(Credentials.Attr.SECRET, secret).build();
        Credentials bogus = new Credentials.Builder().service("s3").lookupKey("bla").set(Credentials.Attr.KEY, "DummyKey").set(Credentials.Attr.SECRET, "DummySecret").build();
        BundledCredentials creds = new BundledCredentials.Builder().addCredentials(bogus, cred).build();
        return creds.addToSecurityContext(null);
    }

    public static String ociSecurityContext(String tenant, String user, String secret, String fingerprint) {
        // Credentials for gor_unittest user in nextcode OCI account
        Credentials cred = new Credentials.Builder().service("oci")
                .lookupKey("gdb_gor_test_data_dev")
                .set(Credentials.Attr.API_ENDPOINT, DEFAULT_OCI_ENDPOINT)
                .set(Credentials.Attr.REALM, tenant)
                .set(Credentials.Attr.KEY, fingerprint)
                .set(Credentials.Attr.SECRET, secret)
                .set(Credentials.Attr.SCOPE, user)
                .build();
        Credentials bogus = new Credentials.Builder().service("oci")
                .lookupKey("bla")
                .set(Credentials.Attr.REALM, "DummyTenant")
                .set(Credentials.Attr.KEY, "DummyFingerprint")
                .set(Credentials.Attr.SECRET, "DummySecret")
                .set(Credentials.Attr.SCOPE, "DummyUser")
                .build();
        BundledCredentials creds = new BundledCredentials.Builder().addCredentials(bogus, cred).build();
        return creds.addToSecurityContext(null);
    }
}
