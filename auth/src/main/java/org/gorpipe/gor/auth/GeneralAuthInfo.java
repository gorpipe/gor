package org.gorpipe.gor.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)  // So we can have additional properties in the json.
public final class GeneralAuthInfo implements GorAuthInfo {

    public final int projectId;
    public final String project;
    public final String username;
    public final String userId;
    public final List<String> userRoles;
    public final int organizationId;
    public final long expiration;

    @JsonCreator
    public GeneralAuthInfo(@JsonProperty("projectId") int projectId,
                           @JsonProperty("project") String project,
                           @JsonProperty("username") String username,
                           @JsonProperty("userId") String userId,
                           @JsonProperty("userRoles") List<String> userRoles,
                           @JsonProperty("organizationId") int organizationId,
                           @JsonProperty("expiration") long expiration) {
        this.projectId = projectId;
        this.project = project != null ? project : "";
        this.username = username != null ? username : "";
        this.userId = userId != null ? userId : "";
        this.userRoles = userRoles != null ? userRoles : new ArrayList<>();
        this.organizationId = organizationId;
        this.expiration = expiration;
    }

    public GeneralAuthInfo(String project,
                           String username,
                           List<String> userRoles,
                           int organizationId,
                           long expiration) {
        this(0, project, username, "", userRoles, organizationId, expiration);
    }

    public GeneralAuthInfo(String project,
                           String username,
                           List<String> userRoles) {
        this(0, project, username, "", userRoles, -1, 0);
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getProject() {
        return this.project;
    }

    @Override
    public int getProjectId() {
        return this.projectId;
    }

    @Override
    public String getUserId() {
        return this.userId;
    }

    @Override
    public int getOrganizationId() {
        return this.organizationId;
    }

    @Override
    public long getExpiration() {
        return this.expiration;
    }

    @Override
    public List<String> getUserRoles() {
        return this.userRoles;
    }

    public static GeneralAuthInfoBuilder builder() {
        return new GeneralAuthInfoBuilder();
    }

    public static final class GeneralAuthInfoBuilder {
        public int projectId;
        public String project;
        public String username;
        public String userId;
        public List<String> userRoles;
        public int organizationId;
        public long expiration;

        private GeneralAuthInfoBuilder() {
        }

        public GeneralAuthInfoBuilder withProjectId(int projectId) {
            this.projectId = projectId;
            return this;
        }

        public GeneralAuthInfoBuilder withProject(String project) {
            this.project = project;
            return this;
        }

        public GeneralAuthInfoBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public GeneralAuthInfoBuilder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public GeneralAuthInfoBuilder withUserRoles(List<String> userRoles) {
            this.userRoles = userRoles;
            return this;
        }

        public GeneralAuthInfoBuilder withOrganizationId(int organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public GeneralAuthInfoBuilder withExpiration(long expiration) {
            this.expiration = expiration;
            return this;
        }

        public GeneralAuthInfo build() {
            return new GeneralAuthInfo(projectId, project, username, userId, userRoles, organizationId, expiration);
        }
    }
}