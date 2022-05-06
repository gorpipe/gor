package org.gorpipe.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UTestBase64Utils {


    /**
     * Test if base64 encoding and decoding of a key value pair map works.
     */
    @Test
    public void testBase64() {
        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("projectRoot", "/mnt/csa_local/env/dev/projects/clinex85");
        stringMap.put("projectname", "clinex85");
        stringMap.put("query", "gor #dbsnp# | top 10123");
        stringMap.put("flags", "H");
        stringMap.put("securityContextKey", "5C5F5F9EC86B74AE1067C42AC0B01F44");
        stringMap.put("request-id", "a0f8acb5-afc3-4538-ba66-0b3a6f3e80e2");
        stringMap.put("userName", "system_admin");
        stringMap.put("querySource", "queryService");
        stringMap.put("fingerprint", "j77ipg5p14ia02nicl6eigf2f25");
        stringMap.put("originalQuery", "gor #dbsnp# | top 10123");
        stringMap.put("time", "1538481250");
        stringMap.put("project-id", "2");
        stringMap.put("project-id2", "");
        stringMap.put("project-id3", null);
        Set<String> keys = new HashSet<>();
        keys.add("query");
        keys.add("originalQuery");
        Map<String, String> encodedStringMap = Base64Utils.base64Encode(stringMap, keys);
        Map<String, String> decodeStringMap = Base64Utils.base64Decode(encodedStringMap, keys);
        Map<String, String> decodeUnencodedStringMap = Base64Utils.base64Decode(stringMap, keys);
        Assert.assertEquals(stringMap, decodeStringMap);
        Assert.assertNotEquals(encodedStringMap, decodeStringMap);
        Assert.assertNotEquals(encodedStringMap, stringMap);
        Assert.assertEquals(stringMap, decodeUnencodedStringMap);
    }
}