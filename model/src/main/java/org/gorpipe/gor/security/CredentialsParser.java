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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CredentialsParser {

    public Credentials parseFromJson(Map<String, Object> cred) {
        Instant expires = parseIso8601Timestamp((String) cred.get("expires"));
        Credentials.Builder builder = new Credentials.Builder();
        builder.service((String) cred.get("service"));
        builder.lookupKey(nullBlank((String) cred.get("lookup_key")));
        builder.ownerType(Credentials.OwnerType.resolve(nullBlank((String) cred.get("owner_type"))));
        if (cred.get("user_default") instanceof Boolean) {
            builder.setUserDefault((Boolean) cred.get("user_default"));
        }
        Object ownerId = cred.get("owner_id");
        if (ownerId != null) ownerId = ownerId.toString();
        builder.ownerId(nullBlank((String) ownerId));
        builder.expires(expires);
        @SuppressWarnings("unchecked")
        Map<String, String> attributes = (Map<String, String>) cred.get("credential_attributes");
        for (String key : attributes.keySet()) {
            String val = nullBlank(attributes.get(key));
            if (val != null) {
                builder.set(key, val);
            }
        }
        return builder.build();
    }

    private String nullBlank(String s) {
        if (s != null && s.equals("")) return null;
        return s;
    }

    public List<Credentials> parseFromJson(List<Map<String, Object>> list) {
        List<Credentials> credList = new ArrayList<>();
        for (Map<String, Object> cred : list) {
            Credentials newCred = parseFromJson(cred);
            if (newCred.isValid()) {
                credList.add(newCred);
            }
        }
        return credList;
    }

    @SuppressWarnings("unchecked")
    public BundledCredentials parseBundle(Map<String, Object> bundle) {
        BundledCredentials.Builder builder = new BundledCredentials.Builder();
        List<Map<String, Object>> credList = (List<Map<String, Object>>) bundle.get("credentials");
        if (credList != null) {
            for (Credentials cred : parseFromJson(credList)) {
                builder.addCredentials(cred);
            }
        }
        credList = (List<Map<String, Object>>) bundle.get("default_credentials");
        if (credList != null) {
            for (Credentials cred : parseFromJson(credList)) {
                builder.addDefaultCredentials(cred);
            }
        }
        return builder.build();
    }


    public static Instant parseIso8601Timestamp(CharSequence stamp) {
        if (stamp != null) {
            return Instant.from(ZonedDateTime.parse(stamp));
        }
        return null;
    }

}
