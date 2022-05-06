package org.gorpipe.gor.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PlatformSessionKey {

    @JsonProperty("security-policy")
    private String securityPolicy;
    private String source;
    private String project;
    @JsonProperty("access-token")
    private String accessToken;

    @JsonCreator
    public PlatformSessionKey(@JsonProperty("security-policy") String securityPolicy,
                              @JsonProperty("source") String source,
                              @JsonProperty("project") String project,
                              @JsonProperty("access-token") String accessToken) {
        this.securityPolicy = securityPolicy;
        this.source = source;
        this.project = project;
        this.accessToken = accessToken;
    }


    public String getSecurityPolicy() {
        return securityPolicy;
    }

    public void setSecurityPolicy(String securityPolicy) {
        this.securityPolicy = securityPolicy;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}