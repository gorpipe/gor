package org.gorpipe.gor.auth;

import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gorpipe.gor.auth.utils.OAuthHandler;
import org.gorpipe.security.cred.CsaApiService;
import org.apache.commons.lang.ArrayUtils;
import org.gorpipe.exceptions.GorSystemException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class UTestAuth {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final static String accessToken = "eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiIxZjM5YjgyOC0zN2ZiLTQ0NTQtODBmNC0yYTYxMTJkYTVjZjEiLCJleHAiOjE1MjkyMjc0MjUsIm5iZiI6MCwiaWF0IjoxNTI2NjM1NDI1LCJpc3MiOiJodHRwOi8vZWMyLTM0LTI0NS04Ni0yMzcuZXUtd2VzdC0xLmNvbXB1dGUuYW1hem9uYXdzLmNvbTo0NDMvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoic2Vjb25kYXJ5LXBpcGVsaW5lIiwic3ViIjoiZWRlOTQ2ZjYtMGI4MS00MmQ4LTgyYjctMTE1MWEzNWZmYWI5IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoic2Vjb25kYXJ5LXBpcGVsaW5lIiwic2Vzc2lvbl9zdGF0ZSI6ImVlNDA2NTJiLWI0NjEtNDU1MS05OTcwLTgwODk3ODdlMmI2YiIsImNsaWVudF9zZXNzaW9uIjoiZTIxNDhkOTYtZmYzYi00YmU0LWI0ZWMtODYwNmU4YjhkZmM4IiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHA6Ly9sb2NhbGhvc3QiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50Iiwidmlldy1wcm9maWxlIl19fSwibmFtZSI6IlNlcnZpY2UgVXNlciIsInByZWZlcnJlZF91c2VybmFtZSI6InBpcGVsaW5lcy1zZXJ2aWNlLXVzZXIiLCJnaXZlbl9uYW1lIjoiU2VydmljZSIsImZhbWlseV9uYW1lIjoiVXNlciIsImVtYWlsIjoicGlwZWxpbmVzLXNlcnZpY2UtdXNlckB3dXhpbmV4dGNvZGUuY29tIn0.bczar3EiSajnu5EO9UySh4wdSRmZd42FXhHKonTzC9qTWZecFZ13UHTUqTu9RHqpxpoJk1VhvNLmZ_2CW_wouYj2GNHs7RyfpZgXt9ERTTQDi3nMcu05ATzpFg6sQhHTO5ylkvUITk8nPX5iwjXfWvB80LFQXydXXegvxRqFi7xBvX0MRvBKT7ez6mcgSofxibDELLqwAShJGyzeMGwcSXE_XhJJ-vC8sxDSmcSniafCIx0idsLepNLRDQbiiBHFnG0D8Rdl7xBRor-b5IMsUCxftznB4dxNjaog-j8s0nidSn-u4PaHmVwAPyMwP2ukRLzk6td-vfSoK-DQSR-UuQ";
    private final static String expiredAccessToken = "eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiI3MmY2OGIwNi1mZjQ5LTQxYjQtOWY2Zi1hY2RlODMwMGUyZmEiLCJleHAiOjE1MjI3NjIxNjMsIm5iZiI6MCwiaWF0IjoxNTIyNzYyMTAzLCJpc3MiOiJodHRwOi8vZWMyLTM0LTI0NS04Ni0yMzcuZXUtd2VzdC0xLmNvbXB1dGUuYW1hem9uYXdzLmNvbTo0NDMvYXV0aC9yZWFsbXMvcGxhdGZvcm0tc2VydmljZXMiLCJhdWQiOiJuZXh0Y29kZS1jbGkiLCJzdWIiOiJlMmFjZGZlNi0zZjFiLTRmY2MtOThhNC1kYmYzOGMxMzcwOTEiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJuZXh0Y29kZS1jbGkiLCJzZXNzaW9uX3N0YXRlIjoiZTljMDdhZDMtNTcwMi00ODU3LTllODgtMGY0OTk3MmVhYWJiIiwiY2xpZW50X3Nlc3Npb24iOiIwYjA2MGI2ZS0zZDQ1LTQ4MGUtYTBhMC03ZjU1ZDkwNjA1ZTkiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJ2aWV3LXByb2ZpbGUiXX19LCJuYW1lIjoiRmphbGFyIFNpZ3Vyw7BhcnNvbiIsInByZWZlcnJlZF91c2VybmFtZSI6ImZqYWxhckB3dXhpbmV4dGNvZGUuY29tIiwiZ2l2ZW5fbmFtZSI6IkZqYWxhciIsImZhbWlseV9uYW1lIjoiU2lndXLDsGFyc29uIiwiZW1haWwiOiJmamFsYXJAd3V4aW5leHRjb2RlLmNvbSJ9.EVomyNZ35V6kT0EO2TaYRJAa3qrwLkxQ9BMUuuj-02wYUzuqiWdoQRZ6WAFOmRd5lsEE6zQBCuY9_07UHy0nzJ9IQlCQWsD0D-J84qWfgD-_y1DD-DRoOV9RdVY_0exBLeR8LOVs8lQczHCQVrV-RIbmdN7CZmrpYh2yU-hLS9B3UBhC1b1OSDWcHP53130qx4W3C5Ed8-sHR4_pfXLqBnbQssBxYc5qm1yuDJ2M3-VvfHNdersYHa1bXkPEAk8TAML4ey3WqHCDVOu6HKlEOjls2ptjx_7FkkfpOrTKsSWysJ6sBMnUsTcljzSs8EPb98qcN_OuOWtwfKJgN59J9A";
    private final static String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoeTOsEOSR99RaY3TIVA/6oQUQZCsshOZXi7pJkfQcNZwxkTdTu35cwt3FaPILTbnq4gGXqPDYM0uZrD5vztghJQv6FGQAvqfPVmuzYIp+3TVtwY06165rz6BzwlCZp/KbFMSKP0/JBA21P5BD6i5SowLLbzyTO0hGbgGAq7eNAiBpYWZFc8MlSseULsAIkCC6PKBaV5HWS5iL8LPQbLDjWmlXmyU5YfPZZoa/ADCPT+iXyD5PdIwEDfWxHzAxVPHSs5bfN6baJiYDSKTQEFWfEq5jIzxi8GSK7eiiQ2ZwBaav7Djq3ByrRONCgoyBrN+GzpdfnX4l6cp57IUOLpryQIDAQAB";
    private final static String securityPolicy = "PLATFORM";
    private final static String source = "Sequence Miner";
    private final static String project = "h19";
    private final static String platformSessionKey = "{"
            + "                \"security-policy\": \"" + securityPolicy + "\","
            + "                \"source\": \"" + source + "\","
            + "                \"project\" : \"" + project + "\","
            + "                \"access-token\" : \"" + accessToken + "\""
            + "}";

    private final static String plainSessionKey = "{\"security-policy\" : \"PLAIN\"," +
            "\"project\" : \"project1\"," +
            "\"username\" : \"user2\"," +
            "\"userRole\" : \"role3\"," +
            "\"userId\" : \"2\"," +
            "\"projectId\" : \"1\"}";
    private final static String plainSessionKey2 = "{\"security-policy\" : \"PLAIN\"," +
            "\"project\" : \"project1\"," +
            "\"username\" : \"user2\"," +
            "\"userRole\" : \"role3\"," +
            "\"userId\" : \"2\"," +
            "\"projectId\" : \"-1\"}";
    private final static String userKey = "preferred_username";
    private final static String username = "pipelines-service-user";
    private final static String expiredUsername = "fjalar@wuxinextcode.com";
    private final static String authorizationProject = "test-proj";
    private final static String authorizationPlatformSessionKey = "{"
            + "                \"security-policy\": \"" + securityPolicy + "\","
            + "                \"source\": \"" + source + "\","
            + "                \"project\" : \"" + authorizationProject + "\","
            + "                \"access-token\" : \"" + accessToken + "\""
            + "}";


    @Test
    public void testPlatformSessionKey() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        PlatformSessionKey platformSessionKey = objectMapper.readValue(UTestAuth.platformSessionKey, PlatformSessionKey.class);
        Assert.assertEquals(securityPolicy, platformSessionKey.getSecurityPolicy());
        Assert.assertEquals(source, platformSessionKey.getSource());
        Assert.assertEquals(project, platformSessionKey.getProject());
        Assert.assertEquals(accessToken, platformSessionKey.getAccessToken());
    }

    @Test
    public void testDecoding() {
        OAuthHandler oAuthOHandler = new OAuthHandler(publicKey);
        DecodedJWT jwt = oAuthOHandler.decodeToken(accessToken);
        Assert.assertEquals(username, jwt.getClaim(userKey).asString());
    }

    /**
     * Ignoring this unit test for now since this access token expires on June 17.
     * If this unit test is run anytime before june 17 it will run. Anytime after
     * June 17 it will fail.
     */
    @Ignore
    @Test
    public void testVerifyingToken() {
        OAuthHandler oAuthOHandler = new OAuthHandler(publicKey);
        DecodedJWT jwt = oAuthOHandler.verifyAccessToken(accessToken);
        Assert.assertEquals(username, jwt.getClaim(userKey).asString());
    }

    @Test
    public void testExpiredDecoding() {
        OAuthHandler oAuthOHandler = new OAuthHandler(publicKey);
        DecodedJWT jwt = oAuthOHandler.decodeToken(expiredAccessToken);
        Assert.assertEquals(expiredUsername, jwt.getClaim(userKey).asString());
    }

    @Test(expected = SignatureVerificationException.class)
    public void testVerifyingExpiredToken() {
        OAuthHandler oAuthOHandler = new OAuthHandler(publicKey);
        DecodedJWT jwt = oAuthOHandler.verifyAccessToken(expiredAccessToken);
        Assert.assertEquals(expiredUsername, jwt.getClaim(userKey).asString());
    }

    @Test
    public void testSecurityPoliceParsingSingles() {
        GorAuthFactory gorAuthFactory;

        // None
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"NONE"};
            }
        });
        Assert.assertEquals("[NONE]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(true, gorAuthFactory.isNoneSecurityPolicy());

        //Plain
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN"};
            }
        });
        Assert.assertEquals("[PLAIN]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //CSA
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"CSA"};
            }
        });
        Assert.assertEquals("[CSA]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        // Non existing
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"NOTEXISTING"};
            }
        });
        Assert.assertEquals("[NOTEXISTING]", gorAuthFactory.getAndValidatePolices().toString());

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"  PLAIN   "};
            }
        });

        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        // Nothing set
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{};
            }
        });
        try {
            gorAuthFactory.getAndValidatePolices().toString();
            Assert.fail("Expected exception for missing securty policy");
        } catch (GorSystemException e) {
            // Expected
        }
    }

    @Test
    public void testSecurityPoliceParsingMultiples() {
        GorAuthFactory gorAuthFactory;

        //NONE, PLAIN
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"NONE", "PLAIN"};
            }
        });
        Assert.assertEquals("[NONE, PLAIN]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //PLAIN, NONE
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN", "NONE"};
            }
        });
        Assert.assertEquals("[PLAIN, NONE]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //PLAIN and non existing
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN", "NOT_EXISTING"};
            }
        });
        Assert.assertEquals("[PLAIN, NOT_EXISTING]", gorAuthFactory.getAndValidatePolices().toString());

        //PLAIN, PLATFORM
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN", "PLATFORM"};
            }
        });
        Assert.assertEquals("[PLAIN, PLATFORM]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //PLAIN, PLATFORM, CSA
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN", "PLATFORM", "CSA"};
            }
        });
        Assert.assertEquals("[PLAIN, PLATFORM, CSA]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //Empty and PLATFORM
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"", "PLATFORM"};
            }
        });
        Assert.assertEquals("[PLATFORM]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //PLATFORM and empty
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM", ""};
            }
        });
        Assert.assertEquals("[PLATFORM]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //PLAIN, empty, PLATFORM and CSA
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN", "", "PLATFORM", "CSA"};
            }
        });
        Assert.assertEquals("[PLAIN, PLATFORM, CSA]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //PLAIN and PLATFORM with spaces
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"  PLAIN ", "    PLATFORM   "};
            }
        });
        Assert.assertEquals("[PLAIN, PLATFORM]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //NONE, PLAIN
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"NONE;PLAIN"};
            }
        });
        Assert.assertEquals("[NONE, PLAIN]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //PLAIN, NONE
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN;NONE"};
            }
        });
        Assert.assertEquals("[PLAIN, NONE]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //PLAIN and non existing
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN;NOT_EXISTING"};
            }
        });
        Assert.assertEquals("[PLAIN, NOT_EXISTING]", gorAuthFactory.getAndValidatePolices().toString());

        //PLAIN, PLATFORM
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN;PLATFORM"};
            }
        });
        Assert.assertEquals("[PLAIN, PLATFORM]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //; and PLATFORM
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{";PLATFORM"};
            }
        });
        Assert.assertEquals("[PLATFORM]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM;"};
            }
        });
        Assert.assertEquals("[PLATFORM]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        //PLAIN, PLATFORM, CSA
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN;PLATFORM;CSA"};
            }
        });
        Assert.assertEquals("[PLAIN, PLATFORM, CSA]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

        // PLAIN and PLATFORM with spaces
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"  PLAIN ;    PLATFORM   "};
            }
        });
        Assert.assertEquals("[PLAIN, PLATFORM]", gorAuthFactory.getAndValidatePolices().toString());
        Assert.assertEquals(false, gorAuthFactory.isNoneSecurityPolicy());

    }

    @Test
    public void testGorAuthFactoryGetGorAuth() {
        GorAuthFactory gorAuthFactory;

        // Singles:  Note if single policy that policy is always used (session not used to find it).
        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"NONE"};
            }
        });

        Assert.assertEquals(SecurityPolicy.NONE, gorAuthFactory.getGorAuth(null).getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.NONE, gorAuthFactory.getGorAuth("").getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.NONE, gorAuthFactory.getGorAuth("dummy").getSecurityPolicy());

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN"};
            }
        });

        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth(null).getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("").getSecurityPolicy());

        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("invalid").getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("{\"username\": \"user1\", \"project\":\"project1\"}").getSecurityPolicy());

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"NOTEXISTING"};
            }
        });
        try {
            gorAuthFactory.getGorAuth(null);
            Assert.fail("Expected unknown security policy error");
        } catch (GorSystemException e) {
            Assert.assertTrue(e.getMessage().contains("Error: Unknown security policy NOTEXISTING"));
            // Expected
        }

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{""};
            }
        });
        try {
            gorAuthFactory.getGorAuth(null);
            Assert.fail("Expected security policy must be set error");
        } catch (GorSystemException e) {
            Assert.assertTrue(e.getMessage().contains("Security policy must not be empty"));
            // Expected
        }

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return null;
            }
        });
        try {
            gorAuthFactory.getGorAuth(null);
            Assert.fail("Expected security policy must be set error");
        } catch (GorSystemException e) {
            Assert.assertTrue(e.getMessage().contains("Security policy config must be set"));
            // Expected
        }

        // Multiple

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"NONE", "PLAIN"};
            }
        });

        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth(null).getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("").getSecurityPolicy());


        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN", "NONE"};
            }
        });

        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth(null).getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("").getSecurityPolicy());


        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM", "PLAIN"};
            }
        });

        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth(null).getSecurityPolicy());

        try {
            gorAuthFactory.getGorAuth("dummy");
            Assert.fail("Expected session key invalid security policy error");
        } catch (GorSystemException e) {
            Assert.assertTrue(e.getMessage().contains("Error:  Session key (dummy) contains invalid security policy"));
            // Expected
        }

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN", "NOT_EXISTING"};
            }
        });

        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth(null).getSecurityPolicy());


        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN", "PLATFORM", "CSA"};
            }

            @Override
            public String sessioncheckerDbUrl() {
                return "someUrl";
            }

            @Override
            public String sessioncheckerUsername() {
                return "someUser";
            }

            @Override
            public String sessioncheckerPassword() {
                return "somePass";
            }

            @Override
            public String publicAuthorizationKey() {
                return publicKey;
            }
        });

        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth(null).getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("{}").getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("{\"user\": \"user1\", \"project\":\"project1\"}").getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("{\"security-policy\":\"PLAIN\", \"user\": \"user1\", \"project\":\"project1\"}").getSecurityPolicy());

        try {
            gorAuthFactory.getGorAuth("{\"security-policy\":\"unknown\", \"user\": \"user1\", \"project\":\"project1\"}");
            Assert.fail("Expected session key invalid security policy error");
        } catch (GorSystemException e) {
            Assert.assertTrue(e.getMessage().contains("contains invalid security policy (unknown)"));
            // Expected
        }

        Assert.assertEquals(SecurityPolicy.PLATFORM, gorAuthFactory.getGorAuth("{\"security-policy\":\"PLATFORM\", \"user\": \"user1\", \"project\":\"project1\"}").getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("{\"security-policy\":\"PLAIN\", \"user\": \"user1\", \"project\":\"project1\"}").getSecurityPolicy());

    }

    @Test
    public void testNoAuthInfo() {
        GorAuthFactory gorAuthFactory;
        GorAuthInfo info;

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"NONE"};
            }
        });

        info = gorAuthFactory.getGorAuthInfo(null);
        Assert.assertEquals(GeneralAuthInfo.class, info.getClass());
        Assert.assertEquals("", info.getProject());
        Assert.assertEquals("", info.getUsername());
        Assert.assertEquals("", info.getUserRole());
        Assert.assertEquals(0, info.getProjectId());
        Assert.assertEquals("", info.getUserId());

        info = gorAuthFactory.getGorAuthInfo("Sometest");
        Assert.assertEquals(GeneralAuthInfo.class, info.getClass());
        Assert.assertEquals("", info.getProject());
        Assert.assertEquals("", info.getUsername());
        Assert.assertEquals("", info.getUserRole());
        Assert.assertEquals(0, info.getProjectId());
        Assert.assertEquals("", info.getUserId());

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"NONE"};
            }

            @Override
            public String sessioncheckerUsername() {
                return "sessionUser";
            }

            @Override
            public String sessioncheckerUserrole() {
                return "sessionRole";
            }

        });

        info = gorAuthFactory.getGorAuthInfo("Sometest");
        Assert.assertEquals(GeneralAuthInfo.class, info.getClass());
        Assert.assertEquals("", info.getProject());
        Assert.assertEquals("sessionUser", info.getUsername());
        Assert.assertEquals("sessionRole", info.getUserRole());
        Assert.assertEquals(0, info.getProjectId());
        Assert.assertEquals("", info.getUserId());

    }

    @Test
    public void testPlainAuthInfo() {
        GorAuthFactory gorAuthFactory;
        GorAuthInfo info;

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN"};
            }
        });

        info = gorAuthFactory.getGorAuthInfo(null);
        Assert.assertEquals(GeneralAuthInfo.class, info.getClass());
        Assert.assertEquals("", info.getProject());
        Assert.assertEquals("", info.getUsername());
        Assert.assertEquals("", info.getUserRole());
        Assert.assertEquals(0, info.getProjectId());
        Assert.assertEquals("", info.getUserId());

        info = gorAuthFactory.getGorAuthInfo("");
        Assert.assertEquals(GeneralAuthInfo.class, info.getClass());
        Assert.assertEquals("", info.getProject());
        Assert.assertEquals("", info.getUsername());
        Assert.assertEquals("", info.getUserRole());
        Assert.assertEquals(0, info.getProjectId());
        Assert.assertEquals("", info.getUserId());

        info = gorAuthFactory.getGorAuthInfo("{}");
        Assert.assertEquals(GeneralAuthInfo.class, info.getClass());
        Assert.assertEquals("", info.getProject());
        Assert.assertEquals("", info.getUsername());
        Assert.assertEquals("", info.getUserRole());
        Assert.assertEquals(0, info.getProjectId());
        Assert.assertEquals("", info.getUserId());

        info = gorAuthFactory.getGorAuthInfo(
                "{\"project\":\"project1\"," +
                        "\"projectId\":1}");
        Assert.assertEquals(GeneralAuthInfo.class, info.getClass());
        Assert.assertEquals("project1", info.getProject());
        Assert.assertEquals("", info.getUsername());
        Assert.assertEquals("", info.getUserRole());
        Assert.assertEquals(1, info.getProjectId());
        Assert.assertEquals("", info.getUserId());

        info = gorAuthFactory.getGorAuthInfo(
                "{\"security-policy\" : \"PLAIN\"," +
                        "\"project\" : \"project1\"," +
                        "\"projectId\" : 1}");
        Assert.assertEquals(GeneralAuthInfo.class, info.getClass());
        Assert.assertEquals("project1", info.getProject());
        Assert.assertEquals("", info.getUsername());
        Assert.assertEquals("", info.getUserRole());
        Assert.assertEquals(1, info.getProjectId());
        Assert.assertEquals("", info.getUserId());

        info = gorAuthFactory.getGorAuthInfo(
                "{\"security-policy\" : \"PLAIN\"," +
                        "\"project\" : \"project1\"," +
                        "\"username\" : \"user2\"," +
                        "\"userRoles\" : [\"role3\"]," +
                        "\"userId\" : \"2\"," +
                        "\"projectId\" : \"1\"}");
        Assert.assertEquals(GeneralAuthInfo.class, info.getClass());
        Assert.assertEquals("project1", info.getProject());
        Assert.assertEquals("user2", info.getUsername());
        Assert.assertEquals("role3", info.getUserRole());
        Assert.assertEquals(1, info.getProjectId());
        Assert.assertEquals("2", info.getUserId());
    }


    @Test
    public void testGorAuthHasAccessNone() {
        GorAuthInfo emptyGorAuth = new GeneralAuthInfo(0, "", "", "0", null, 0, 0);
        GorAuthInfo stdGorAuth = new GeneralAuthInfo(1, "project1", "user2", "2", Arrays.asList("role3"), 0, 0);

        GorAuthInfo stdGorAuthDiffUser = new GeneralAuthInfo(1, "project1", "user4", "4", Arrays.asList("role3"), 0, 0);
        GorAuthInfo stdGorAuthDiffProject = new GeneralAuthInfo(5, "project5", "user2", "2", Arrays.asList("role6"), 0, 0);
        GorAuthInfo otherGorAuth = new GeneralAuthInfo(10, "otherProject", "otherUser", "20", Arrays.asList("otherRole6"), 0, 0);
        GorAuthInfo otherGorAuthNoProject = new GeneralAuthInfo(0, "", "user7", "7", Arrays.asList("role8"), 0, 0);

        // None

        GorAuth gorAuth = new NoAuth(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"NONE"};
            }
        }, "user2", "project1", 1, "2", Arrays.asList("role3"), 0);

        Assert.assertEquals(true, gorAuth.hasQueryAccess(emptyGorAuth, "", ""));
        Assert.assertEquals(true, gorAuth.hasQueryAccess(emptyGorAuth, "project1", "user2"));

        Assert.assertEquals(true, gorAuth.hasQueryAccess(stdGorAuth, "", ""));
        Assert.assertEquals(true, gorAuth.hasQueryAccess(stdGorAuth, "project1", "user2"));
        Assert.assertEquals(true, gorAuth.hasQueryAccess(stdGorAuth, "otherProject", "otherUser"));

        Assert.assertEquals(true, gorAuth.hasQueryAccess(otherGorAuthNoProject, "", ""));
        Assert.assertEquals(true, gorAuth.hasQueryAccess(otherGorAuthNoProject, "project1", "user2"));
        Assert.assertEquals(true, gorAuth.hasQueryAccess(otherGorAuthNoProject, "otherProject", "otherUser"));
    }

    @Test
    public void testGorAuthHasAccessPlain() {
        GorAuth gorAuth = new PlainAuth(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN"};
            }
        }, null);

        GorAuthInfo emptyGorAuth = new GeneralAuthInfo(0, "", "", "0", null, 0, 0);
        GorAuthInfo stdGorAuth = new GeneralAuthInfo(1, "project1", "user2", "2", Arrays.asList("role3"), 0, 0);
        GorAuthInfo otherGorAuthNoProject = new GeneralAuthInfo(0, "", "user7", "7", Arrays.asList("role8"), 0, 0);

        Assert.assertEquals(true, gorAuth.hasBasicAccess(emptyGorAuth, "", ""));
        Assert.assertEquals(true, gorAuth.hasBasicAccess(emptyGorAuth, "project1", "user2"));

        Assert.assertEquals(true, gorAuth.hasBasicAccess(stdGorAuth, "", ""));
        Assert.assertEquals(true, gorAuth.hasBasicAccess(stdGorAuth, "project1", "user2"));
        Assert.assertEquals(true, gorAuth.hasBasicAccess(stdGorAuth, "otherProject", "otherUser"));

        Assert.assertEquals(true, gorAuth.hasBasicAccess(otherGorAuthNoProject, "", ""));
        Assert.assertEquals(true, gorAuth.hasBasicAccess(otherGorAuthNoProject, "project1", "user2"));
        Assert.assertEquals(true, gorAuth.hasBasicAccess(otherGorAuthNoProject, "otherProject", "otherUser"));
    }

    @Test
    public void testGorAuthHasQueryAccess() {
        GorAuth gorAuth = new PlatformAuth(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM"};
            }

            @Override
            public boolean userRolesFromToken() {
                return true;
            }

        }, null, null);

        List<String> noQueryRoles = new ArrayList();
        noQueryRoles.add("offline_access");
        noQueryRoles.add("prj:test-proj:researcher");
        noQueryRoles.add("prj:test-proj:file:read");
        noQueryRoles.add("prj:*:file:read");

        List<String>  queryRoles = new ArrayList();
        queryRoles.add("offline_access");
        queryRoles.add("prj:test-proj:researcher");
        queryRoles.add("prj:project1:query");
        queryRoles.add("prj:project1:file:write:user_data");
        queryRoles.add("prj:*:file:read");

        List<String>  queryProjectRoles = new ArrayList();
        queryProjectRoles.add("offline_access");
        queryProjectRoles.add("prj:test-proj:researcher");
        queryProjectRoles.add("prj:*:query");
        queryProjectRoles.add("prj:*:file:read");

        List<String> projectAllRoles = new ArrayList<>();
        projectAllRoles.add("offline_access");
        projectAllRoles.add("prj:test-proj:researcher");
        projectAllRoles.add("prj:test-proj:file:read");
        projectAllRoles.add("prj:project1:*");
        projectAllRoles.add("prj:*:file:read");

        GorAuthInfo gorAuth1 = new GeneralAuthInfo(0, "project1", "user1", "1", noQueryRoles, 0, 0);
        GorAuthInfo gorAuth2 = new GeneralAuthInfo(0, "project1", "user1", "1", queryRoles, 0, 0);
        GorAuthInfo gorAuth3 = new GeneralAuthInfo(0, "project1", "user1", "1", queryProjectRoles, 0, 0);
        GorAuthInfo gorAuth4 = new GeneralAuthInfo(0, "project1", "user1", "1", projectAllRoles, 0, 0);

        Assert.assertFalse(gorAuth.hasQueryAccess(gorAuth1, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasQueryAccess(gorAuth1, "project2", "user1"));
        Assert.assertFalse(gorAuth.hasQueryAccess(gorAuth1, "", "user1"));

        Assert.assertTrue(gorAuth.hasQueryAccess(gorAuth2, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasQueryAccess(gorAuth2, "project2", "user1"));
        Assert.assertFalse(gorAuth.hasQueryAccess(gorAuth2, "", "user1"));

        Assert.assertTrue(gorAuth.hasQueryAccess(gorAuth3, "project1", "user1"));
        Assert.assertTrue(gorAuth.hasQueryAccess(gorAuth3, "project2", "user1"));
        Assert.assertFalse(gorAuth.hasQueryAccess(gorAuth3, "", "user1"));

        Assert.assertTrue(gorAuth.hasQueryAccess(gorAuth4, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasQueryAccess(gorAuth4, "project2", "user1"));
        Assert.assertFalse(gorAuth.hasQueryAccess(gorAuth4, "", "user1"));
    }

    @Test
    public void testGorAuthHasReadAccess() {
        GorAuth gorAuth = new PlatformAuth(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM"};
            }

            @Override
            public String publicAuthorizationKey() {
                return publicKey;
            }

            @Override
            public boolean userRolesFromToken() {
                return true;
            }
        }, null, new OAuthHandler(publicKey));

        List<String> noReadRoles = new ArrayList();
        noReadRoles.add("offline_access");
        noReadRoles.add("prj:test-proj:researcher");
        noReadRoles.add("prj:test-proj:file:read");

        List<String> readRoles = new ArrayList();
        readRoles.add("offline_access");
        readRoles.add("prj:test-proj:researcher");
        readRoles.add("prj:project1:file:read");

        List<String> readInAllProjectsRoles = new ArrayList();
        readInAllProjectsRoles.add("offline_access");
        readInAllProjectsRoles.add("prj:test-proj:researcher");
        readInAllProjectsRoles.add("prj:*:file:read");

        List<String> allRolesInProject = new ArrayList();
        allRolesInProject.add("offline_access");
        allRolesInProject.add("prj:test-proj:researcher");
        allRolesInProject.add("prj:project1:*");

        GorAuthInfo gorAuth1 = new GeneralAuthInfo(1, "project1", "user1", "1", noReadRoles, 1, 0);
        GorAuthInfo gorAuth2 = new GeneralAuthInfo(1, "project1", "user1", "1", readRoles, 1, 0);
        GorAuthInfo gorAuth3 = new GeneralAuthInfo(1, "project1", "user1", "1", readInAllProjectsRoles, 1, 0);
        GorAuthInfo gorAuth4 = new GeneralAuthInfo(1, "project1", "user1", "1", allRolesInProject, 1, 0);

        Assert.assertFalse(gorAuth.hasReadAccess(gorAuth1, "project1"));
        Assert.assertFalse(gorAuth.hasReadAccess(gorAuth1, "project2"));
        Assert.assertTrue(gorAuth.hasReadAccess(gorAuth1, "test-proj"));

        Assert.assertTrue(gorAuth.hasReadAccess(gorAuth2, "project1"));
        Assert.assertFalse(gorAuth.hasReadAccess(gorAuth2, "project2"));
        Assert.assertFalse(gorAuth.hasReadAccess(gorAuth2, "test-proj"));

        Assert.assertTrue(gorAuth.hasReadAccess(gorAuth3, "project1"));
        Assert.assertTrue(gorAuth.hasReadAccess(gorAuth3, "project2"));
        Assert.assertTrue(gorAuth.hasReadAccess(gorAuth3, "test-proj"));

        Assert.assertTrue(gorAuth.hasReadAccess(gorAuth4, "project1"));
        Assert.assertFalse(gorAuth.hasReadAccess(gorAuth4, "project2"));
        Assert.assertFalse(gorAuth.hasReadAccess(gorAuth4, "test-proj"));
    }

    @Test
    public void testGorAuthHasWriteAccess() {
        GorAuth gorAuth = new PlatformAuth(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM"};
            }

            @Override
            public String publicAuthorizationKey() {
                return publicKey;
            }

            @Override
            public boolean userRolesFromToken() {
                return true;
            }
        }, null, new OAuthHandler(publicKey));

        List<String> noWriteRoles = new ArrayList<>();
        noWriteRoles.add("offline_access");
        noWriteRoles.add("prj:test-proj:researcher");
        noWriteRoles.add("prj:test-proj:file:read");
        noWriteRoles.add("prj:*:file:read");

        List<String> userDataWriteRoles = new ArrayList<>();
        userDataWriteRoles.add("offline_access");
        userDataWriteRoles.add("prj:test-proj:researcher");
        userDataWriteRoles.add("prj:test-proj:file:read");
        userDataWriteRoles.add("prj:project1:file:write:user_data");
        userDataWriteRoles.add("prj:*:file:read");

        List<String> writeRoles = new ArrayList<>();
        writeRoles.add("offline_access");
        writeRoles.add("prj:test-proj:researcher");
        writeRoles.add("prj:test-proj:file:read");
        writeRoles.add("prj:project1:file:write");
        writeRoles.add("prj:*:file:read");

        GorAuthInfo gorAuth1 = new GeneralAuthInfo(1, "project1", "user1", "1", noWriteRoles, 1, 0);
        GorAuthInfo gorAuth2 = new GeneralAuthInfo(1, "project1", "user1", "1", userDataWriteRoles, 1, 0);
        GorAuthInfo gorAuth3 = new GeneralAuthInfo(1, "project1", "user1", "1", writeRoles, 1, 0);

        Assert.assertFalse(gorAuth.hasWriteAccess("user_data/outfile.gor", gorAuth1, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasWriteAccess("folder1/outfile.gor", gorAuth1, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasWriteAccess("", gorAuth1, "project1", "user1"));

        Assert.assertTrue(gorAuth.hasWriteAccess("user_data/outfile.gor", gorAuth2, "project1", "user1"));
        Assert.assertTrue(gorAuth.hasWriteAccess("user_data/folder1/outfile.gor", gorAuth2, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasWriteAccess("user_data/../outfile.gor", gorAuth2, "project1", "user1"));
        Assert.assertTrue(gorAuth.hasWriteAccess("user_data/folder1/../outfile.gor", gorAuth2, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasWriteAccess("folder1/outfile.gor", gorAuth2, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasWriteAccess("folder1/user_data/outfile.gor", gorAuth2, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasWriteAccess("folder1_user_data/outfile.gor", gorAuth2, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasWriteAccess("", gorAuth2, "project1", "user1"));

        Assert.assertTrue(gorAuth.hasWriteAccess("user_data/outfile.gor", gorAuth3, "project1", "user1"));
        Assert.assertTrue(gorAuth.hasWriteAccess("folder1/outfile.gor", gorAuth3, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasWriteAccess("../folder1/outfile.gor", gorAuth3, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasWriteAccess("", gorAuth2, "project1", "user1"));
    }

    @Test
    public void testGorAuthHasLordSubmitAccess() {
        GorAuth gorAuth = new PlatformAuth(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM"};
            }

            @Override
            public boolean userRolesFromToken() {
                return true;
            }

        }, null, null);

        List<String> noLordRoles = new ArrayList<>();
        noLordRoles.add("offline_access");
        noLordRoles.add("prj:test-proj:researcher");
        noLordRoles.add("prj:test-proj:file:read");

        List<String> lordRoles = new ArrayList<>();
        lordRoles.add("offline_access");
        lordRoles.add("prj:test-proj:researcher");
        lordRoles.add("prj:project1:lord:submit");
        lordRoles.add("prj:project1:file:write:user_data");

        List<String> lordProjectRoles = new ArrayList<>();
        lordProjectRoles.add("offline_access");
        lordProjectRoles.add("prj:test-proj:researcher");
        lordProjectRoles.add("prj:*:lord:submit");

        List<String> projectAllRoles = new ArrayList<>();
        projectAllRoles.add("offline_access");
        projectAllRoles.add("prj:test-proj:researcher");
        projectAllRoles.add("prj:test-proj:file:file:read");
        projectAllRoles.add("prj:project1:*");

        GorAuthInfo gorAuth1 = new GeneralAuthInfo(0, "project1", "user1", "1", noLordRoles, 0, 0);
        GorAuthInfo gorAuth2 = new GeneralAuthInfo(0, "project1", "user1", "1", lordRoles, 0, 0);
        GorAuthInfo gorAuth3 = new GeneralAuthInfo(0, "project1", "user1", "1", lordProjectRoles, 0, 0);
        GorAuthInfo gorAuth4 = new GeneralAuthInfo(0, "project1", "user1", "1", projectAllRoles, 0, 0);

        Assert.assertFalse(gorAuth.hasLordSubmitAccess(gorAuth1, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasLordSubmitAccess(gorAuth1, "project2", "user1"));
        Assert.assertFalse(gorAuth.hasLordSubmitAccess(gorAuth1, "", "user1"));

        Assert.assertTrue(gorAuth.hasLordSubmitAccess(gorAuth2, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasLordSubmitAccess(gorAuth2, "project2", "user1"));
        Assert.assertFalse(gorAuth.hasLordSubmitAccess(gorAuth2, "", "user1"));

        Assert.assertTrue(gorAuth.hasLordSubmitAccess(gorAuth3, "project1", "user1"));
        Assert.assertTrue(gorAuth.hasLordSubmitAccess(gorAuth3, "project2", "user1"));
        Assert.assertFalse(gorAuth.hasLordSubmitAccess(gorAuth3, "", "user1"));

        Assert.assertTrue(gorAuth.hasLordSubmitAccess(gorAuth4, "project1", "user1"));
        Assert.assertFalse(gorAuth.hasLordSubmitAccess(gorAuth4, "project2", "user1"));
        Assert.assertFalse(gorAuth.hasLordSubmitAccess(gorAuth4, "", "user1"));
    }


    @Test
    public void testGorAuthHasRoleAccessPlatformJWT() {
        GorAuth gorAuth = new PlatformJWTAuth(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"JWT"};
            }

            @Override
            public String publicAuthorizationKey() {
                return publicKey;
            }
        }, null);

        List<String> roles = new ArrayList<>();
        roles.add("prj:project1:query");
        roles.add("prj:project2:*");

        Assert.assertEquals(true, gorAuth.hasAccessBasedOnRoles(roles, AuthorizationAction.QUERY, "project1"));
        Assert.assertEquals(true, gorAuth.hasAccessBasedOnRoles(roles, AuthorizationAction.QUERY, "project2"));
        Assert.assertEquals(false, gorAuth.hasAccessBasedOnRoles(roles, AuthorizationAction.QUERY, null));
        Assert.assertEquals(false, gorAuth.hasAccessBasedOnRoles(roles, AuthorizationAction.QUERY, ""));
        Assert.assertEquals(false, gorAuth.hasAccessBasedOnRoles(roles, AuthorizationAction.QUERY, "project3"));
    }

    @Test
    public void testValidateUserProject() {

        GorAuth gorAuth = new GorAuth(null, null) {
            @Override
            public GorAuthInfo getGorAuthInfo(String sessionKey) {
                return null;
            }
        };

        Assert.assertFalse(gorAuth.validateUserProject(null, null));
        Assert.assertFalse(gorAuth.validateUserProject("user", null));
        Assert.assertFalse(gorAuth.validateUserProject(null, "project"));
        Assert.assertFalse(gorAuth.validateUserProject("", "project"));
        Assert.assertTrue(gorAuth.validateUserProject("user", "project"));
    }

    @Test
    public void testHasQueryAccess() {

        GorAuth gorAuth = new GorAuth(null, null) {
            @Override
            public GorAuthInfo getGorAuthInfo(String sessionKey) {
                return null;
            }
        };

        Assert.assertFalse(gorAuth.validateUserProject(null, null));
        Assert.assertFalse(gorAuth.validateUserProject("user", null));
        Assert.assertFalse(gorAuth.validateUserProject(null, "project"));
        Assert.assertFalse(gorAuth.validateUserProject("", "project"));
        Assert.assertTrue(gorAuth.validateUserProject("user", "project"));
    }


    @Test
    public void testSecurityPolicy() {
        String[] securityPolicies = {"CSA", "PLATFORM"};

        Assert.assertTrue(!ArrayUtils.contains(securityPolicies, SecurityPolicy.PLAIN.toString()));
    }

    @Test
    public void testUpdateGorAuthInfo() throws IOException {
        CsaApiService csaApiService = mock(CsaApiService.class);
        Map projectMap = new LinkedHashMap<String, Object>();
        projectMap.put("id", 5);
        doReturn(projectMap).when(csaApiService).getProject("project1");

        Map userMap = new LinkedHashMap<String, Object>();
        userMap.put("id", 10);
        doReturn(userMap).when(csaApiService).getUserByEmail("user@email.com");

        AuthConfig config = mock(AuthConfig.class);
        doReturn("CSA").when(config).updateAuthInfoPolicy();

        GorAuth gorAuth = new PlainAuth(config, csaApiService);
        GorAuth gorAuth2 = new PlainAuth(config, null);

        GorAuthFactory gorAuthFactory;
        GorAuthInfo info;

        gorAuthFactory = new GorAuthFactory(new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLAIN"};
            }
        });
        // We use CSA API
        info = gorAuthFactory.getGorAuthInfo(plainSessionKey2);
        GorAuthInfo gorAuthInfoUpdate = gorAuth.updateGorAuthInfo(info);
        Assert.assertEquals(5, gorAuthInfoUpdate.getProjectId());

        // We do not use CSA API due csaApiService being null
        info = gorAuthFactory.getGorAuthInfo(plainSessionKey2);
        GorAuthInfo gorAuthInfoUpdate2 = gorAuth2.updateGorAuthInfo(info);
        Assert.assertEquals(-1, gorAuthInfoUpdate2.getProjectId());

        // We do not use CSA API due to config being none
        doReturn("NONE").when(config).updateAuthInfoPolicy();

        info = gorAuthFactory.getGorAuthInfo(plainSessionKey2);
        GorAuthInfo gorAuthInfoUpdate4 = gorAuth.updateGorAuthInfo(info);
        Assert.assertEquals(-1, gorAuthInfoUpdate4.getProjectId());
    }

    @Test
    public void testPlatformGetGorAuthInfoExpired() {
        AuthConfig authConfig = new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM"};
            }

            @Override
            public String publicAuthorizationKey() {
                return publicKey;
            }

            @Override
            public String getPlatformUserKey() {
                return "email";
            }

        };

        GorAuth gorAuth = new GorAuthFactory(authConfig).getGorAuth(authorizationPlatformSessionKey);
        Assert.assertNull(gorAuth.getGorAuthInfo(authorizationPlatformSessionKey));
    }

    @Test
    public void testPlatformGetGorAuthInfo() {
        AuthConfig authConfig = new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM"};
            }

            @Override
            public String publicAuthorizationKey() {
                return publicKey;
            }

            @Override
            public String getPlatformUserKey() {
                return "email";
            }

            @Override
            public boolean userRolesFromToken() {
                return true;
            }

        };

        OAuthHandler oAuthOHandler = mock(OAuthHandler.class);
        DecodedJWT jwt = mock(DecodedJWT.class);
        Claim claim = mock(Claim.class);
        doReturn(jwt).when(oAuthOHandler).verifyAccessToken(accessToken);
        doReturn(claim).when(jwt).getClaim("email");
        doReturn(claim).when(jwt).getClaim("realm_access");
        doReturn(Calendar.getInstance().getTime()).when(jwt).getExpiresAt();
        doReturn("dummy@dummies.com").when(claim).asString();

        Map rolesMap = new LinkedHashMap();
        List<String> rolesList = new ArrayList<>();
        rolesList.add("offline_access");
        rolesList.add("prj:test-proj:researcher");
        rolesList.add("prj:test-proj:file:read");
        rolesList.add("prj:*:file:read");
        rolesMap.put("roles", rolesList);

        doReturn(rolesMap).when(claim).asMap();

        GorAuth gorAuth = new PlatformAuth(authConfig, null, oAuthOHandler);
        GorAuthInfo gorAuthInfo = gorAuth.getGorAuthInfo(authorizationPlatformSessionKey);
        Assert.assertEquals(authorizationProject, gorAuthInfo.getProject());
        Assert.assertEquals("dummy@dummies.com", gorAuthInfo.getUsername());

        Assert.assertEquals(rolesList, gorAuthInfo.getUserRoles());
    }

    @Test(expected = GorSystemException.class)
    public void testPlatformGetGorAuthInfoNoPublic() {
        AuthConfig authConfig = new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM"};
            }

            @Override
            public String publicAuthorizationKey() {
                return "";
            }

            @Override
            public String getPlatformUserKey() {
                return "";
            }

        };

        GorAuth gorAuth = new GorAuthFactory(authConfig).getGorAuth(authorizationPlatformSessionKey);
        Assert.assertNull(gorAuth.getGorAuthInfo(authorizationPlatformSessionKey));
    }

    @Test
    public void testSessionKey() {

        AuthConfig authConfig = new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM", "PLAIN", "CSA"};
            }

            @Override
            public String publicAuthorizationKey() {
                return publicKey;
            }

            @Override
            public String getPlatformUserKey() {
                return "email";
            }

            @Override
            public String sessioncheckerUsername() {
                return "user";
            }

            @Override
            public String sessioncheckerPassword() {
                return "pass";
            }

            @Override
            public String sessioncheckerDbUrl() {
                return "db:url";
            }

        };

        GorAuthFactory gorAuthFactory = new GorAuthFactory(authConfig);

        //PLATFORM
        Assert.assertEquals(SecurityPolicy.PLATFORM, gorAuthFactory.getGorAuth(platformSessionKey).getSecurityPolicy());

        //PLAIN
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth(plainSessionKey).getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("").getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth(null).getSecurityPolicy());
        // If we can't find security-policy then we default to PLAIN:
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("{\"security\": \"abcdef\"}").getSecurityPolicy());
        Assert.assertEquals(SecurityPolicy.PLAIN, gorAuthFactory.getGorAuth("{\n" +
                "\t\"securities\": {\n" +
                "\t\t\"items\": [{\n" +
                "\t\t\t\"security\": \"abc\"\n" +
                "\t\t}, {\n" +
                "\t\t\t\"security\": \"def\"\n" +
                "\t\t}]\n" +
                "\t}\n" +
                "}").getSecurityPolicy());

    }


    @Test
    public void testPlatformGetGorAuthInfoNoIntegration() {
        AuthConfig authConfig = new TestAuthConfig() {
            @Override
            public String[] securityPolicies() {
                return new String[]{"PLATFORM"};
            }

            @Override
            public String publicAuthorizationKey() {
                return publicKey;
            }

            @Override
            public String getPlatformUserKey() {
                return "email";
            }
        };

        GorAuth gorAuth = new GorAuthFactory(authConfig).getGorAuth(authorizationPlatformSessionKey);
        GorAuthInfo gorAuthInfo = gorAuth.getGorAuthInfo(authorizationPlatformSessionKey);
        Assert.assertEquals(null, gorAuthInfo);
    }

    // Helper class to make simpler to extend AuthConfig.
    private static class TestAuthConfig implements AuthConfig {

        @Override
        public String[] securityPolicies() {
            return new String[]{"PLATFORM"};
        }

        @Override
        public String sessioncheckerDbUrl() {
            return null;
        }

        @Override
        public String sessioncheckerUsername() {
            return null;
        }

        @Override
        public String sessioncheckerUserrole() {
            return null;
        }

        @Override
        public String sessioncheckerPassword() {
            return null;
        }

        @Override
        public String publicAuthorizationKey() {
            return null;
        }

        @Override
        public String getPlatformUserKey() {
            return null;
        }

        @Override
        public String updateAuthInfoPolicy() {
            return "CSA";
        }

        @Override
        public boolean userRolesFromToken() {
            return false;
        }

        @Override
        public String projectRoot() {
            return "/mnt/csa/env/dev/projects";
        }
    }
}
