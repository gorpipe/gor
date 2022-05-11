package org.gorpipe.security.cred;

import org.gorpipe.security.cred.CredentialsHelperMain;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by villi on 07/12/16.
 */
public class UTestCredentialsHelperMain {

    static class MapEnv extends CredentialsHelperMain.Env {
        Map<String, String> myEnv = new HashMap<>();

        @Override
        public String getenv(String env) {
            return myEnv.get(env);
        }
    }

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();


    @Test
    public void testForUserOption() {
        List<String> unparsed = new ArrayList<>();
        CredentialsHelperMain.Options options;

        options = CredentialsHelperMain.Options.parse( new String[]{"--for-user", "123"}, unparsed);
        Assert.assertEquals("123", options.forUser);

        options = CredentialsHelperMain.Options.parse( new String[]{"--for-user", "abc"}, unparsed);
        Assert.assertEquals("abc", options.forUser);

        options = CredentialsHelperMain.Options.parse( new String[]{}, unparsed);
        Assert.assertEquals(null, options.forUser);
    }

    @Test
    public void testInvalidForUserOption() {
        List<String> unparsed = new ArrayList<>();
        exit.expectSystemExitWithStatus(-1);
        CredentialsHelperMain.Options.parse(new String[]{"--for-user"}, unparsed);
    }

    @Test
    public void testParseApiVarsFromEnvironment() {
        MapEnv mine = new MapEnv();
        mine.myEnv.put("CSA_API_ENDPOINT", "http://nextcode.dev/csa/api/v1");
        mine.myEnv.put("CSA_API_USER", "test");
        mine.myEnv.put("CSA_API_PASSWORD", "password");
        CredentialsHelperMain.setEnv(mine);
        CredentialsHelperMain.Options opts = CredentialsHelperMain.Options.processArgs(new String[]{"--for-project", "clinical2"});
        Assert.assertEquals("http://nextcode.dev/csa/", opts.apiUrl);
        Assert.assertEquals("test", opts.apiUser);
        Assert.assertEquals("password", opts.apiPassword);

        mine.myEnv.put("CSA_API_ENDPOINT", "http://csa.dev/csa/api/");
        opts = CredentialsHelperMain.Options.processArgs(new String[]{"--for-project", "clinical2"});
        Assert.assertEquals("http://csa.dev/csa/", opts.apiUrl);
    }

}
