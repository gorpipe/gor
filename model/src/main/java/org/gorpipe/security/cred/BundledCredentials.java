/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
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

package org.gorpipe.security.cred;

import org.gorpipe.util.string.JsonUtil;
import org.gorpipe.util.string.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Immutable class holding bundled credentials
 */
public class BundledCredentials implements CredentialsProvider {
    private final static Logger log = LoggerFactory.getLogger(BundledCredentials.class);

    private final ConcurrentMap<String, ConcurrentMap<String, Credentials>> svcToLookupMap;
    private final ConcurrentMap<String, Credentials> serviceToDefaultMap;
    private static BundledCredentials emptyCredentials = new BundledCredentials();

    private BundledCredentials(ConcurrentMap<String, ConcurrentMap<String, Credentials>> svcMap, ConcurrentMap<String, Credentials> defMap) {
        this.svcToLookupMap = svcMap;
        this.serviceToDefaultMap = defMap;
    }

    private BundledCredentials() {
        this(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    @Override
    public List<Credentials> getCredentials(String service, String lookupKey) {
        service = service.toLowerCase();
        Credentials cred = null;
        if (lookupKey != null) {
            lookupKey = lookupKey.toLowerCase();
            if (svcToLookupMap.get(service) != null) {
                Map<String, Credentials> lookup = svcToLookupMap.get(service);
                if (lookup != null) {
                    cred = lookup.get(lookupKey);
                }
            }
        }
        Credentials defaultCreds = serviceToDefaultMap.get(service);
        if (cred == null && defaultCreds == null) {
            return Collections.emptyList();
        }
        List<Credentials> result = new ArrayList<>();
        if (cred != null) result.add(cred);
        if (defaultCreds != null) result.add(defaultCreds);
        return result;
    }

    /**
     * Returns a list of credentials for a given service
     *
     * @param service The service name
     * @return a list of credentials
     */
    public List<Credentials> getCredentialsForService(String service) {
        service = service.toLowerCase();
        List<Credentials> result = new ArrayList<>();
        if (svcToLookupMap.get(service) != null) {
            Map<String, Credentials> lookup = svcToLookupMap.get(service);
            result.addAll(lookup.values());
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BundledCredentials) {
            BundledCredentials other = (BundledCredentials) obj;
            return other.svcToLookupMap.equals(svcToLookupMap) && other.serviceToDefaultMap.equals(serviceToDefaultMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(svcToLookupMap, serviceToDefaultMap);
    }

    public Set<String> services() {
        TreeSet<String> svc = new TreeSet<>();
        svc.addAll(svcToLookupMap.keySet());
        svc.addAll(serviceToDefaultMap.keySet());
        return svc;
    }

    private Map<String, Object> toMap() {
        Map<String, Object> bundle = new HashMap<>();
        List<Map<String, Object>> creds = new ArrayList<>();
        for (Map<String, Credentials> map : svcToLookupMap.values()) {
            for (Credentials cred : map.values()) {
                creds.add(cred.toMap());
            }
        }
        bundle.put("credentials", creds);
        List<Map<String, Object>> defaults = new ArrayList<>();
        for (Credentials cred : serviceToDefaultMap.values()) {
            defaults.add(cred.toMap());
        }
        bundle.put("default_credentials", defaults);
        return bundle;
    }

    public String toJson() {
        return JsonUtil.toJson(toMap());
    }

    /**
     * Get string representation with summary only without revealing any secrets
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(super.toString());
        b.append("{");
        for (String service : svcToLookupMap.keySet()) {
            b.append(service);
            b.append(":(");
            ConcurrentMap<String, Credentials> map = svcToLookupMap.get(service);
            if (map != null) {
                b.append(StringUtils.join(map.keySet(), ","));
            }
            if (serviceToDefaultMap.get(service) != null) {
                b.append(",[Default]");
            } else {
                b.append(",[No Default]");
            }
            b.append(") ");
        }
        b.append("}");
        return b.toString();
    }

    /**
     * Clone credentials
     */
    public BundledCredentials clone() {
        return new BundledCredentials.Builder().addCredentials(this).build();
    }

    public static BundledCredentials emptyCredentials() {
        return emptyCredentials;
    }

    public static BundledCredentials fromSecurityContext(String securityContext) {
        log.debug("Creating bundle from securityContext: {}", securityContext);
        if (securityContext != null && securityContext.contains("cred_bundle=")) {
            String credString = StringUtil.substring(securityContext, "cred_bundle=", "|||");
            // It looks like the 'securityContext' can include other options after it(e.g. -H1)
            // We need to assume that context is enclosed in quotes (trailing ')
            credString = credString.replaceAll(" .*|'", "");
            BundledCredentials result = fromBase64String(credString);
            log.debug("Got credentials {}", result);
            return result;
        }
        log.debug("No bundle found");
        return emptyCredentials();
    }

    private static BundledCredentials fromBase64String(String base64) {
        byte[] decoded = Base64.getUrlDecoder().decode(base64);
        return new CredentialsParser().parseBundle(JsonUtil.parseJson(new String(decoded)));
    }

    public String toBase64String() {
        return Base64.getUrlEncoder().encodeToString(toJson().getBytes());
    }

    public String addToSecurityContext(String securityContext) {
        log.debug("Adding {} to securityContext {}", this, securityContext);
        Builder b = new Builder().addCredentials(fromSecurityContext(securityContext));
        b.addCredentials(this);
        String bundleCtx = "cred_bundle=" + b.build().toBase64String();

        String result;
        if (securityContext == null) {
            result = " -Z " + bundleCtx;
        } else {
            result = securityContext + "|||" + bundleCtx;
        }
        log.debug("Result: {}", result);
        return result;
    }

    /**
     * Method to merge a pair of BundledCredentials. If the same service exists in both credentials then
     * the credentials in the second parameter take precedence.
     * @param credentials1
     * @param credentials2
     * @return
     */
    public static BundledCredentials mergeBundledCredentials(BundledCredentials credentials1, BundledCredentials credentials2) {
        if (credentials1 == null) {
            return credentials2;
        }
        if (credentials2 == null) {
            return credentials1;
        }

        ConcurrentMap<String, ConcurrentMap<String, Credentials>> svcToLookupMap1 = credentials1.svcToLookupMap;
        ConcurrentMap<String, ConcurrentMap<String, Credentials>> svcToLookupMap2 = credentials2.svcToLookupMap;

        ConcurrentMap<String, ConcurrentMap<String, Credentials>> svcToLookupMapMerged = new ConcurrentHashMap<>();
        svcToLookupMapMerged.putAll(svcToLookupMap1);
        svcToLookupMapMerged.putAll(svcToLookupMap2);

        ConcurrentMap<String, Credentials> serviceToDefaultMap1 = credentials1.serviceToDefaultMap;
        ConcurrentMap<String, Credentials> serviceToDefaultMap2 = credentials2.serviceToDefaultMap;

        ConcurrentMap<String, Credentials> serviceToDefaultMapMerged = new ConcurrentHashMap<>();
        serviceToDefaultMapMerged.putAll(serviceToDefaultMap1);
        serviceToDefaultMapMerged.putAll(serviceToDefaultMap2);

        return new BundledCredentials(svcToLookupMapMerged, serviceToDefaultMapMerged);
    }

    public static class Builder {
        private ConcurrentMap<String, ConcurrentMap<String, Credentials>> svcToLookupMap = new ConcurrentHashMap<>();
        private ConcurrentMap<String, Credentials> serviceToDefaultMap = new ConcurrentHashMap<>();

        /**
         * Add credentials to bundle to be looked up by service and lookup key.
         * Will replace any existing credentials that belong to the same service and lookup key.
         */
        public Builder addCredentials(Credentials... creds) {
            for (Credentials cred : creds) {
                Map<String, Credentials> m = getServiceMap(cred.getService());
                m.put(cred.getLookupKey(), cred);
                if (cred.isUserDefault()) {
                    addDefaultCredentials(cred);
                }
            }
            return this;
        }

        /**
         * Add default credentials to the bundle.
         * Will replace any existing default credentials that belong to the same service
         */
        public Builder addDefaultCredentials(Credentials creds) {
            serviceToDefaultMap.putIfAbsent(creds.getService(), creds);
            return this;
        }


        /**
         * Add all credentials from an existing bundle.
         * Same effect as calling addCredentials/addDefaultCredentials for each of the credentials in the source bundle.
         */
        public Builder addCredentials(BundledCredentials creds) {
            for (ConcurrentMap<String, Credentials> credMap : creds.svcToLookupMap.values()) {
                for (Credentials cred : credMap.values()) {
                    addCredentials(cred);
                }
            }
            for (Credentials cred : creds.serviceToDefaultMap.values()) {
                addDefaultCredentials(cred);
            }
            return this;
        }

        private ConcurrentMap<String, Credentials> getServiceMap(String service) {
            return svcToLookupMap.computeIfAbsent(service, (String s) -> new ConcurrentHashMap<>());
        }

        /**
         * Build bundled credentials (clears current credentials)
         */
        public BundledCredentials build() {
            BundledCredentials built = new BundledCredentials(svcToLookupMap, serviceToDefaultMap);
            svcToLookupMap = new ConcurrentHashMap<>();
            serviceToDefaultMap = new ConcurrentHashMap<>();
            return built;
        }

    }
}
