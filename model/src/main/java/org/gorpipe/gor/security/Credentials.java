/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.gor.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Credentials object - represents e.g. S3 keys
 * Immutable
 * <p>
 * Created by villi on 07/03/16.
 */
public class Credentials {
    public static Credentials NULL = new Credentials();

    /**
     * Type of credential owners - starting with the most specific ownership
     */
    public enum OwnerType {
        User,
        Project,
        System,
        Organization;

        public static OwnerType resolve(String s) {
            if (s == null) return null;
            return valueOf(s);
        }
    }

    /**
     * Some common/useful attributes
     */
    public enum Attr {
        /**
         * May be used to differentiate between different services or api endpoints .
         * E.g. http host
         */
        REALM,
        /**
         * Scope within SourceType/Realm - normally identifies the access control unit.
         * E.g. S3 bucket
         */
        SCOPE,
        /**
         * Name associated with these credentials - e.g. username or key name
         */
        NAME,
        /**
         * Non secret part of key - e.g. public key.
         */
        KEY,
        /**
         * Secret key - e.g. password, secret key, bearer token etc.
         */
        SECRET,
        /**
         * Temporary security credentials require a Token
         */
        SESSION_TOKEN,
        /**
         * Api endpoint url/server
         */
        API_ENDPOINT,
        /**
         * Authentication endpoint url/server
         */
        AUTH_ENDPOINT;

        public String jsonKey() {
            return toString().toLowerCase();
        }
    }

    public static class Builder {
        Credentials cred;

        public Builder() {
            this.cred = new Credentials();
            cred.data = new HashMap<>();
        }

        /**
         * @param service Service type - e.g. s3 ...
         */
        public Builder service(String service) {
            if (service != null) service = service.toLowerCase();
            cred.service = service;
            return this;
        }

        /**
         * @param lookupKey Key to lookup credentials - unique within service.
         */
        public Builder lookupKey(String lookupKey) {
            if (lookupKey != null) lookupKey = lookupKey.toLowerCase();
            cred.lookupKey = lookupKey;
            return this;
        }

        /**
         * @param ownerType Type of owner (Project, System or User)
         */
        public Builder ownerType(OwnerType ownerType) {
            cred.ownerType = ownerType;
            return this;
        }

        /**
         * @param ownerId Owner id
         */
        public Builder ownerId(String ownerId) {
            cred.ownerId = ownerId;
            return this;
        }

        /**
         * @param expires Expiry time
         */
        public Builder expires(Instant expires) {
            cred.expires = expires;
            return this;
        }

        /**
         * Set attribute
         */
        public Builder set(Attr attr, String value) {
            cred.data.put(attr.jsonKey(), value);
            return this;
        }

        /**
         * Set attribute
         */
        public Builder set(String attr, String value) {
            cred.data.put(attr, value);
            return this;
        }

        /**
         * Flag as default credentials for user (for that service)
         */
        public Builder setUserDefault(boolean flag) {
            cred.userDefault = flag;
            return this;
        }

        /**
         * Build credentials object.
         * After build -  builder will retain all values already set - and can be mutated further.
         */
        public Credentials build() {
            return new Credentials(cred.service, cred.lookupKey, cred.ownerType, cred.ownerId, cred.expires, cred.userDefault, cred.data);
        }
    }

    private String service;
    private String lookupKey;
    private OwnerType ownerType;
    private String ownerId;
    private Instant expires;
    private Map<String, String> data;
    private boolean userDefault;

    /**
     * Create credentials object
     *
     * @param service   Service type - e.g. s3, dx ...
     * @param lookupKey Key to lookup credentials - unique within service.
     * @param ownerType Type of owner (Project, System or User)
     * @param ownerId   Owner id
     * @param expires   Optional expiry time
     * @param keyVals   Key value pairs - containing credential data
     */
    public Credentials(String service, String lookupKey, OwnerType ownerType, String ownerId, Instant expires, boolean userDefault, String... keyVals) {
        this(service, lookupKey, ownerType, ownerId, expires, userDefault, makeMap(keyVals));
    }

    Credentials() {
        // Empty constructor for builder
    }

    public boolean isNull() {
        return (this.data == null);
    }

    private static Map<String, String> makeMap(String... keyVals) {
        Map<String, String> data = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            data.put(keyVals[i], keyVals[i + 1]);
        }
        return data;
    }

    public Credentials(String service, String lookupKey, OwnerType ownerType, String ownerId, Instant expires, boolean userDefault, Map<String, String> data) {
        this.service = service;
        this.lookupKey = lookupKey;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.expires = expires;

        this.data = new HashMap<>();
        this.data.putAll(data);
        this.userDefault = userDefault;
    }

    public String getService() {
        return service;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public OwnerType getOwnerType() {
        return ownerType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Get attribute by name. Use lowercase/underscored format.
     */
    public String get(String key) {
        if (isNull()) return null;
        return data.get(key);
    }

    public String get(Attr key) {
        return get(key.jsonKey());
    }

    public Set<String> keys() {
        return data.keySet();
    }

    public boolean containsKey(Attr key) {
        return data.containsKey(key.jsonKey());
    }

    public Instant expires() {
        return expires;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Credentials) {
            return Objects.equals(data, ((Credentials) obj).data);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    public boolean isValidFor(long value, TemporalUnit unit) {
        if (isNull()) return false;
        if (expires == null) return true;
        return expires.isAfter(Instant.now().plus(value, unit));
    }

    public boolean isValid() {
        return isValidFor(1, ChronoUnit.SECONDS);
    }

    public boolean isUserDefault() {
        return userDefault;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(super.toString());
        append(result, "(Service: ", service, ",LookupKey: ", lookupKey, ",OwnerType: ", ownerType, ",OwnerId: ", ownerId, ",Expires: ", expires);
        append(result, ",Attributes: ", data, ")");
        return result.toString();
    }

    private void append(StringBuilder builder, Object... stuffs) {
        for (Object stuff : stuffs) {
            builder.append(stuff);
        }
    }

    /**
     * Create map representation
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("owner_type", ownerType == null ? null : ownerType.toString());
        result.put("owner_id", ownerId);
        result.put("service", service);
        result.put("lookup_key", lookupKey);
        result.put("expires", expires == null ? null : expires.toString());
        result.put("credential_attributes", data);
        result.put("user_default", userDefault);
        return result;
    }
}
