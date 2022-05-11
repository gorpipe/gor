package org.gorpipe.gor.auth;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.gorpipe.gor.auth.GorAuthRoleMatcher.SYSTEM_ADMIN_ROLE;

public class UTestGorAuthRoleMatcher {


    @Test
    public void testGetRolesThatGiveAccess() {
        //static List<String> getRolesThatGiveAccess(String project, String subject, AuthorizationAction... actions) {

        Assert.assertEquals(Arrays.asList("prj:p1:file:write", "prj:p1:project_admin", "system_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", null, AuthorizationAction.WRITE));

        Assert.assertEquals(Arrays.asList("prj:p1:file:write", "prj:p1:project_admin", "system_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "", AuthorizationAction.WRITE));

        Assert.assertEquals(Arrays.asList("prj:p1:file:write", "prj:p1:file:write:s1", "prj:p1:project_admin", "system_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "s1", AuthorizationAction.WRITE));

        Assert.assertEquals(Arrays.asList("prj:p1:file:write", "prj:p1:file:write:s1/*", "prj:p1:project_admin", "system_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "s1/*", AuthorizationAction.WRITE));

        Assert.assertEquals(Arrays.asList("prj:p1:file:write", "prj:p1:file:write:folder1/file1", "prj:p1:project_admin", "system_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "folder1/file1", AuthorizationAction.WRITE));

        Assert.assertEquals(Arrays.asList("system_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("", null, AuthorizationAction.WRITE));

        Assert.assertEquals(Arrays.asList("system_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("", "s1", AuthorizationAction.WRITE));

        Assert.assertEquals(Arrays.asList("prj:p1:file:write", "prj:p1:file:write:user_data/user1/file1", "prj:p1:file:write:user_data", "prj:p1:project_admin", "system_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE));


    }

    @Test
    public void testMatchRolePatterns() {
        //static boolean matchRolePatterns(List<String> userRolesPatterns, List<String> allowAccessRoles) {

        // Allow

        Assert.assertEquals(true, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("prj:p1:file:write"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));

        Assert.assertEquals(true, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("prj:p1:file:write:*"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));

        Assert.assertEquals(true, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("prj:p1:file:write:user_data/*"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));


        Assert.assertEquals(true, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("prj:p1:project_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));

        Assert.assertEquals(true, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("system_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));


        Assert.assertEquals(true, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("prj:p1:project_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "root/file1", AuthorizationAction.WRITE)));

        Assert.assertEquals(true, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("system_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "root/file1", AuthorizationAction.WRITE)));

        Assert.assertEquals(true, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("system_admin"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "/root/file1", AuthorizationAction.WRITE)));

        // Deny


        Assert.assertEquals(false, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList(""),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));

        Assert.assertEquals(false, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("some_other_folder"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));

        Assert.assertEquals(false, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("some_other_folder/*"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));

        Assert.assertEquals(false, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("user_data/user2/*"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));

        Assert.assertEquals(false, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("user_data/user1/file2"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));

        Assert.assertEquals(false, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("user_data/user1/file2"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));


        // User data special

        Assert.assertEquals(true, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("prj:p1:file:write:user_data"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE)));

        Assert.assertEquals(true, GorAuthRoleMatcher.matchRolePatterns(Arrays.asList("prj:p1:file:write:user_data"),
                GorAuthRoleMatcher.getRolesThatGiveAccess("p1", "user_data/user1/file1", AuthorizationAction.WRITE_TO_USER_DATA)));


    }

    @Test
    public void testSystemAdminAcccess() {
        GorAuthInfo authInfo = new GeneralAuthInfo("p1", "u1", Arrays.asList("role1", SYSTEM_ADMIN_ROLE, "rol2"));
        Assert.assertEquals(true, GorAuthRoleMatcher.hasRolebasedSystemAdminAccess(authInfo));
    }


}
