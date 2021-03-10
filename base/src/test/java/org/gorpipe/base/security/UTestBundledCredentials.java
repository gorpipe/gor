package org.gorpipe.base.security;

import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.Credentials;
import org.junit.Assert;
import org.junit.Test;

import java.util.Base64;
import java.util.List;
import java.util.Set;

public class UTestBundledCredentials {

    @Test
    public void getCredentials_EmptyCredentials() {
        final BundledCredentials credentials = BundledCredentials.emptyCredentials();
        final List<Credentials> credentialsList = credentials.getCredentials("bingo", "bongo");
        Assert.assertTrue(credentialsList.isEmpty());
    }

    @Test
    public void getCredentials_NonEmptyCredentials() {
        final BundledCredentials credentials = getBundledCredentialsForService("bingo");
        final List<Credentials> credentialsList = credentials.getCredentials("bingo", "bongo");
        Assert.assertEquals(1, credentialsList.size());
    }

    @Test
    public void getCredentialsForService_EmptyCredentials() {
        final BundledCredentials credentials = BundledCredentials.emptyCredentials();
        final List<Credentials> credentialsList = credentials.getCredentialsForService("bingo");
        Assert.assertTrue(credentialsList.isEmpty());
    }

    @Test
    public void getCredentialsForService_NonEmptyCredentials() {
        final BundledCredentials credentials = getBundledCredentialsForService("bingo");
        final List<Credentials> credentialsList = credentials.getCredentialsForService("bingo");
        Assert.assertEquals(1, credentialsList.size());
    }

    @Test
    public void services_emptyCredentials() {
        final BundledCredentials credentials = BundledCredentials.emptyCredentials();
        final Set<String> services = credentials.services();
        Assert.assertTrue(services.isEmpty());
    }

    @Test
    public void fromSecurityContext_WhenContextEmptyReturnsEmptyCredentials() {
        final BundledCredentials credentials = BundledCredentials.fromSecurityContext("");
        final Set<String> services = credentials.services();
        Assert.assertTrue(services.isEmpty());
    }

    @Test
    public void fromSecurityContext_WhenContextNotEmpty() {
        final BundledCredentials credentials = getBundledCredentialsForService("bingo");
        final Set<String> services = credentials.services();
        Assert.assertTrue(services.contains("bingo"));
        Assert.assertEquals(1, services.size());
    }

    @Test
    public void addToSecurityContext_WhenNull() {
        final BundledCredentials credentials = BundledCredentials.emptyCredentials();
        final String s = credentials.addToSecurityContext(null);

        // Not really sure what to test here, but at least this ensures the method is called.
        Assert.assertTrue(!s.isEmpty());

        final BundledCredentials context = BundledCredentials.fromSecurityContext(s);
        final Set<String> services = credentials.services();
        Assert.assertTrue(services.isEmpty());
    }

    @Test
    public void addToSecurityContext_WhenNotNull() {
        final BundledCredentials credentials = BundledCredentials.emptyCredentials();
        final String forService = getSecurityContextForService("bingo");
        final String s = credentials.addToSecurityContext(forService);

        // Not really sure what to test here, but at least this ensures the method is called.
        Assert.assertTrue(!s.isEmpty());

        final BundledCredentials bundledCredentials = BundledCredentials.fromSecurityContext(s);
        final Set<String> services = bundledCredentials.services();
        Assert.assertTrue(services.contains("bingo"));
        Assert.assertEquals(1, services.size());
    }

    @Test
    public void mergeBundledCredentials() {
        final BundledCredentials credentials1 = getBundledCredentialsForService("bingo");
        final BundledCredentials credentials2 = getBundledCredentialsForService("bongo");
        final BundledCredentials merged = BundledCredentials.mergeBundledCredentials(credentials1, credentials2);
        final Set<String> services = merged.services();
        Assert.assertTrue(services.contains("bingo"));
        Assert.assertTrue(services.contains("bongo"));
        Assert.assertEquals(2, services.size());
    }

    @Test
    public void mergeBundledCredentials_WhenFirstIsNull() {
        final BundledCredentials credentials2 = getBundledCredentialsForService("bongo");
        final BundledCredentials merged = BundledCredentials.mergeBundledCredentials(null, credentials2);
        Assert.assertEquals(credentials2, merged);
    }

    @Test
    public void mergeBundledCredentials_WhenSecondIsNull() {
        final BundledCredentials credentials1 = getBundledCredentialsForService("bongo");
        final BundledCredentials merged = BundledCredentials.mergeBundledCredentials(credentials1, null);
        Assert.assertEquals(credentials1, merged);
    }

    private BundledCredentials getBundledCredentialsForService(String service) {
        final String securityContext = getSecurityContextForService(service);
        return BundledCredentials.fromSecurityContext(securityContext);
    }

    private String getSecurityContextForService(String service) {
        final String input = "{\"credentials\": [{\"service\": \"" + service + "\", \"lookup_key\": \"bongo\", \"credential_attributes\": {}}]}";
        final String encoded = Base64.getUrlEncoder().encodeToString(input.getBytes());
        return String.format("cred_bundle=%s", encoded);
    }
}