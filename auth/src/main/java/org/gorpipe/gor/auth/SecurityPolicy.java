package org.gorpipe.gor.auth;

public enum SecurityPolicy {
    /**
     * No security policy
     */
    NONE,
    /**
     * Plain text (json GorAUthInfo)
     */
    PLAIN,
    /**
     * Security is through Platform Services
     */
    PLATFORM,
    /**
     * Security is through CSA API
     */
    CSA,
    /**
     * Security through keycloak JWT
     */
    JWT
}