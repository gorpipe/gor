package org.gorpipe.gor.model;

import org.gorpipe.gor.auth.GeneralAuthInfo;
import org.gorpipe.gor.auth.GorAuthInfo;
import org.gorpipe.gor.auth.SecurityPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class to old security context for secure file reading/writing.
 *
 * NOTE:  In many ways it would make sense that these fields were just part of the ProjectContext (some of them
 *        already are) and we would use the ProjectContext instead of this object.  But, the ProjectContext is
 *        defined in GOR and there we have no notion of access control (userRoles, writeLocations etc).
 *
 *        We have some options of fixing thi:  1. Move auth code to GOR, 2. Create SecureProjectContext and SecureGorSession
 *        objects to use with Secure file reader.
 *
 *        For now we are just keeping this as separate object.
 */
public class AccessControlContext {

    private final GorAuthInfo authInfo;
    private final List<String> writeLocations;
    private final boolean allowAbsolutePath;
    private final SecurityPolicy securityPolicy;

    public AccessControlContext(GorAuthInfo authInfo, List<String> writeLocations, boolean allowAbsolutePath,
                                SecurityPolicy securityPolicy) {
        this.authInfo = authInfo != null ? authInfo : new GeneralAuthInfo("", "", null);
        this.writeLocations =  writeLocations != null ? writeLocations : new ArrayList<>();
        this.allowAbsolutePath = allowAbsolutePath;
        this.securityPolicy = securityPolicy != null ? securityPolicy : SecurityPolicy.JWT;
    }

    public GorAuthInfo getAuthInfo() {
        return authInfo;
    }

    public List<String> getWriteLocations() {
        return writeLocations;
    }

    public boolean isAllowAbsolutePath() {
        return allowAbsolutePath;
    }

    public SecurityPolicy getSecurityPolicy() {
        return securityPolicy;
    }

    public String getProject() {
        return authInfo.getProject();
    }

    public String getUser() {
        return authInfo.getUsername();
    }

    public List<String> getUserRoles() {
        return authInfo.getUserRoles();
    }

    public static SecureFileReaderContextBuilder builder() {
        return new SecureFileReaderContextBuilder();
    }

    public static final class SecureFileReaderContextBuilder {
        private GorAuthInfo authInfo;
        private List<String> writeLocations;
        private boolean allowAbsolutePath = false;
        private SecurityPolicy securityPolicy;

        private SecureFileReaderContextBuilder() {
        }

        public SecureFileReaderContextBuilder withAuthInfo(GorAuthInfo authInfo) {
            this.authInfo = authInfo;
            return this;
        }

        public SecureFileReaderContextBuilder withWriteLocations(List<String> writeLocations) {
            this.writeLocations = writeLocations;
            return this;
        }

        public SecureFileReaderContextBuilder withAllowAbsolutePath(boolean allowAbsolutePath) {
            this.allowAbsolutePath = allowAbsolutePath;
            return this;
        }

        public SecureFileReaderContextBuilder withSecurityPolicy(SecurityPolicy securityPolicy) {
            this.securityPolicy = securityPolicy;
            return this;
        }

        public AccessControlContext build() {
            return new AccessControlContext(authInfo, writeLocations, allowAbsolutePath, securityPolicy);
        }
    }
}
