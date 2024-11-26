package org.gorpipe.oci.driver;

import com.oracle.bmc.Region;
import org.apache.commons.lang3.StringUtils;
import org.gorpipe.exceptions.GorParsingException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.table.util.PathUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Handle OCI urls.
 * <p>
 * Supported formats:
 * <pre>
 *  OCI:
 *      - OCI native  {@literal https://<oci-endpoint>/n/<namespace>/b/<bucket>/o/<path>/<to>/<file>}
 *      - s3 style
 *          - Short {@literal s3://<bucket>/<path>/<to>/<file>}
 *          - Standard - path based {@literal s3://<oci-endpoint>/<bucket>/<path>/<to>/<file>}
 *          - Standard - virtual hosted {@literal s3://<bucket>.<oci-endpoint>/<path>/<to>/<file>}
 *          - https - path based {@literal https://<oci-endpoint>/<bucket>/<path>/<to>/<file>}
 *          - https - virtual hosted {@literal https://<bucket>.<oci-endpoint>/<path>/<to>/<file>}
 *       - GOR: {@literal oci://<bucket>/<path>/<to>/<file>} - with the endpoint defined
 *
 *      Note:
 *          - We can not distinguish between the path and virtual hosted based urls, os we must pass in the type.
 *          - <oci-endpoint> is always {@literal <namespace>.objectstorage.<region>.oci.customer-oci.com}
 *  See:
 *      - https://docs.oracle.com/en-us/iaas/autonomous-database/doc/cloud-storage-uris.html#GUID-26978C37-BFCE-4E0B-8C39-8AF399F2067B
 *      - https://docs.oracle.com/en-us/iaas/Content/Object/Concepts/dedicatedendpoints.htm
 *      - https://community.aws/content/2biM1C0TkMkvJ2BLICiff8MKXS9/format-and-parse-amazon-s3-url
 *      - https://www.ietf.org/rfc/rfc3986.txt
 *
 *  Note:  There are a few types of s3 urls, The difference is mainly where the bucket is defined:
 *          - global url: s3://<bucket>/<key>
 *          - path style: https://s3.<region>.amazonaws.com/<bucket>/<key>
 *          - virtual host style: https://<bucket>.<region>.s3.amazonaws.com/<key>
 *
 *          Path style is currently deprecated but it is the only style supported by OCI compat layer.
 *
 *          NOTE:  Now we support mix of global and our variant of the path style urls.
 * </pre>
 * <p>
 */
public class OCIUrl {

    public static final String DEFAULT_OCI_ENDPOINT =
            System.getProperty("gor.oci.endpoint",
                    "https://id5mlxoq0dmt.objectstorage.us-ashburn-1.oci.customer-oci.com");
    public static final Region DEFAULT_REGION =
            Region.valueOf(System.getProperty("gor.oci.region",
                    Region.US_ASHBURN_1.toString()));
    private static final Pattern OCI_URL_PATTERN =
            Pattern.compile(System.getProperty("gor.oci.endpoint.pattern",
                    ".*\\.objectstorage\\..*\\.oci\\.customer-oci\\.com.*"));
    public static final String PATH_DELIMITER = "/";

    private String bucket;
    private String lookupKey;
    private String path;
    private String endpoint;
    private URI originalUrl;
    private String namespace;

    public static OCIUrl parse(String url) throws MalformedURLException {
        URI uri = URI.create(url);
        return parse(uri);
    }

    public static OCIUrl parse(SourceReference sourceRef) throws MalformedURLException {
        var uri = PathUtils.resolve(sourceRef.commonRoot,sourceRef.getUrl());
        return parse(uri);
    }

    public static OCIUrl parse(URI uri) throws MalformedURLException {
        OCIUrl result = new OCIUrl();
        result.originalUrl = uri;

        if (OCI_URL_PATTERN.matcher(uri.toString()).matches()) {
            return parseHttpUrl(uri);
        } else {
            return parseGlobalUrl(uri);
        }
    }

    private static OCIUrl parseHttpUrl(URI uri) throws MalformedURLException {
        if (uri.getAuthority() == null) {
            throw new MalformedURLException("Expected authority in OCI objectstore url: " + uri);
        }

        if (!uri.getAuthority().contains(".objectstorage.")) {
            throw new MalformedURLException("Invalid OCI objectstore url: " + uri);
        }

        if (uri.getPath().contains("/n/")) {
            return parseOCIHttpUrl(uri);
        } else {
            return parseS3HttpUrl(uri);
        }
    }

    //  {@literal https://<oci-endpoint>/n/<namespace>/b/<bucket>/o/<path>/<to>/<file>}
    private static OCIUrl parseOCIHttpUrl(URI uri) throws MalformedURLException {
        if (!Arrays.asList("http", "https", "s3").contains(uri.getScheme())) {
            throw new MalformedURLException("S3 url must have http[s]/s3 scheme: " + uri);
        }

        var result = new OCIUrl();

        result.endpoint = uri.getAuthority();
        var pathElements = uri.getPath().split(PATH_DELIMITER, 7);
        result.bucket = pathElements[4];
        result.path = pathElements[6];

        result.originalUrl = uri;
        result.lookupKey = result.bucket;
        result.namespace = pathElements[2];

        return result;
    }

    private static OCIUrl parseS3HttpUrl(URI uri) throws MalformedURLException {
        if (!Arrays.asList("http", "https").contains(uri.getScheme())) {
            throw new MalformedURLException("OCI native url must have http[s] scheme: " + uri);
        }

        var result = new OCIUrl();

        // Matches  <bucket>.<namespace>.objectstorage... (oci) but not <bucket>.<region>.s3.. (s3) .
        var isVirtualHostStyle = uri.getAuthority()
                .substring(0, uri.getAuthority().indexOf(".objectstorage.")).contains(".");

        if (isVirtualHostStyle) {
            var firstDot = uri.getAuthority().indexOf(".");
            result.endpoint = uri.getAuthority().substring(firstDot + 1);
            result.bucket = uri.getAuthority().substring(0, firstDot);
            result.path = uri.getPath().substring(1);
        } else {
            result.endpoint = uri.getAuthority();
            var elmentSplitPos = uri.getPath().indexOf(PATH_DELIMITER, 1);
            result.bucket = uri.getPath().substring(1, elmentSplitPos);
            result.path = uri.getPath().substring(elmentSplitPos +1);
        }

        result.originalUrl = uri;
        result.lookupKey = result.bucket;
        result.namespace = result.endpoint.split("\\.", 2)[0];

        return result;
    }

    private static OCIUrl parseGlobalUrl(URI uri) throws MalformedURLException {
        if (!Arrays.asList("s3", "oci").contains(uri.getScheme())) {
            throw new MalformedURLException("Global url must have s3/oci scheme: " + uri);
        }

        var result = new OCIUrl();

        result.endpoint = DEFAULT_OCI_ENDPOINT;
        result.bucket = uri.getAuthority();
        result.path = uri.getPath().substring(1);

        result.originalUrl = uri;
        result.lookupKey = result.bucket;
        result.namespace = result.endpoint.substring(8).split("\\.", 2)[0];

        return result;
    }

    public String getBucket() {
        return bucket;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public String getPath() {
        return path;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public URI getOriginalUrl() {
        return originalUrl;
    }

    public String getNamespace() {
        return namespace;
    }
}
