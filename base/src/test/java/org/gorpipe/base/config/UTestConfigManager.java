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

package org.gorpipe.base.config;

import org.gorpipe.test.SystemPropertyHelper;
import org.gorpipe.base.config.annotations.ConfigComponent;
import org.aeonbits.owner.Config;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.gorpipe.base.config.ConfigManager.getKeysForConfig;
import static org.junit.Assert.*;

public class UTestConfigManager {

    public static final String ANNOTATION_TEST_PREFIX = "annotationprefix";

    private static final String TEST_PROPERTY_DEFAULT_VALUE_A = "";

    private static final String TEST_PROPERTY_B = "test.property.b";
    private static final String TEST_PROPERTY_C = "test.property.c";
    private static final String TEST_PROPERTY_D = "test.property.d";
    private static final String TEST_PROPERTY_E = "test.property.e";
    private static final String TEST_PROPERTY_F = "test.property.f";
    private static final String TEST_PROPERTY_G = "test.property.g";
    private static final String TEST_PROPERTY_H = "test.property.h";
    private static final String TEST_PROPERTY_I = "test.property.i";
    private static final String TEST_PROPERTY_J = "test.property.j";
    private static final String TEST_PROPERTY_K = "test.property.k";

    private static final String SYSTEM_PROPERTY_E = "system.property.E";
    private static final String FILE_ENDING_PROPS = ".props";
    private static final String COMMENT_TEST_PROPERTY_FILES = "test properties file";

    @ConfigComponent(ANNOTATION_TEST_PREFIX)
    interface TestConfig extends Config {
        @Key("test.property.a")
        @DefaultValue(TEST_PROPERTY_DEFAULT_VALUE_A)
        String getTestPropertyA();

        @Key(TEST_PROPERTY_B)
        @DefaultValue("test.property.default.value.b")
        String getTestPropertyB();

        @Key(TEST_PROPERTY_C)
        @DefaultValue("test.property.default.value.c")
        String getTestPropertyC();

        @Key(TEST_PROPERTY_D)
        @DefaultValue("test.property.default.value.d")
        String getTestPropertyD();

        @Key(TEST_PROPERTY_E)
        @DefaultValue("test.property.default.value.e")
        String getTestPropertyE();

        @Key(TEST_PROPERTY_F)
        @DefaultValue("test.property.default.value.f")
        String getTestPropertyF();

        @Key(TEST_PROPERTY_G)
        @DefaultValue("test.property.default.value.g")
        String getTestPropertyG();

        @Key(TEST_PROPERTY_H)
        @DefaultValue("test.property.default.value.h")
        String getTestPropertyH();

        @Key(TEST_PROPERTY_I)
        @DefaultValue("test.property.default.value.i")
        String getTestPropertyI();

        @Key(TEST_PROPERTY_J)
        @DefaultValue("test.property.default.value.j")
        String getTestPropertyJ();

        @Key(TEST_PROPERTY_K)
        @DefaultValue("test.property.default.value.k")
        String getTestPropertyK();
    }

    interface MinimalTestConfig extends Config {
        @Key("minimal.test.property.a")
        @DefaultValue("test.property.minimal.default.value.a")
        String getTestPropertyA();

        @Key("minimal.test.property.b")
        @DefaultValue("test.property.minimal.default.value.b")
        String getTestPropertyB();

        @Key("minimal.test.property.c")
        @DefaultValue("test.property.minimal.default.value.c")
        String getTestPropertyE();
    }

    private static File rootDir;

    static {
        try {
            rootDir = Files.createTempDirectory("nconfig_test").toFile();
            rootDir.deleteOnExit();
        } catch (IOException e) {
            fail("Could not create temporary directory for test files.");
        }
    }

    private SystemPropertyHelper sysprops = new SystemPropertyHelper();
    private File configDir;

    @Before
    public void before() {
        configDir = new File(rootDir, "config");
        configDir.mkdirs();
        configDir.deleteOnExit();

        ConfigManager.setConfigRootPath(configDir.getAbsolutePath());
    }

    @After
    public void after() {
        sysprops.reset();
    }

    @AfterClass
    public static void afterClass() {
        try {
            FileUtils.deleteDirectory(rootDir);
        } catch (IOException e) {
            // Do nothing
        }
    }

    @Test
    public void testGetKeysForConfig() {
        Set<String> actualKeys = new HashSet<>(Arrays.asList("minimal.test.property.a", "minimal.test.property.b", "minimal.test.property.c"));

        String[] keysFromClass = getKeysForConfig(MinimalTestConfig.class);
        assertEquals("Number of class keys should be the same as the original keys.", actualKeys.size(), keysFromClass.length);
        assertTrue("Class keys should be the same as the original keys.", actualKeys.containsAll(Arrays.asList(keysFromClass)));
    }

    @Test
    public void testConfigPrecedence() {
        Properties p1 = new Properties();
        p1.setProperty(TEST_PROPERTY_B, "p1.B");
        p1.setProperty(TEST_PROPERTY_C, "p1.C");
        p1.setProperty(TEST_PROPERTY_D, "p1.D");
        p1.setProperty(TEST_PROPERTY_E, "p1.E");

        Properties p2 = new Properties();
        p2.setProperty(TEST_PROPERTY_C, "p2.C");
        p2.setProperty(TEST_PROPERTY_E, "p2.E");

        Properties p3 = new Properties();
        p3.setProperty(TEST_PROPERTY_D, "p3.D");
        p3.setProperty(TEST_PROPERTY_E, "p3.E");

        sysprops.setSystemProperty(TEST_PROPERTY_E, SYSTEM_PROPERTY_E);

        TestConfig config = ConfigManager.createConfig(TestConfig.class, System.getProperties(), p3, p2, p1);

        assertEquals(TEST_PROPERTY_DEFAULT_VALUE_A, config.getTestPropertyA());
        assertEquals("p1.B", config.getTestPropertyB());
        assertEquals("p2.C", config.getTestPropertyC());
        assertEquals("p3.D", config.getTestPropertyD());
        assertEquals(SYSTEM_PROPERTY_E, config.getTestPropertyE());
    }

    @Test
    public void testPrefixConfigFileLoading() throws IOException {
        String prefix = "testprefix";

        Properties p1 = new Properties();
        p1.setProperty(TEST_PROPERTY_B, "p1.B");
        p1.setProperty(TEST_PROPERTY_C, "p1.C");
        p1.setProperty(TEST_PROPERTY_D, "p1.D");
        p1.setProperty(TEST_PROPERTY_E, "p1.E");
        p1.store(new FileOutputStream(new File(configDir, prefix + ".props.defaults")), "test default properties file");

        Properties p2 = new Properties();
        p2.setProperty(TEST_PROPERTY_C, "p2.C");
        p2.setProperty(TEST_PROPERTY_E, "p2.E");
        p2.store(new FileOutputStream(new File(configDir, prefix + FILE_ENDING_PROPS)), "test user properties file");

        sysprops.setSystemProperty(TEST_PROPERTY_E, SYSTEM_PROPERTY_E);

        // Create config instance and supply a specific prefix
        TestConfig config = ConfigManager.createPrefixConfig(prefix, TestConfig.class);

        assertEquals(TEST_PROPERTY_DEFAULT_VALUE_A, config.getTestPropertyA());
        assertEquals("p1.B", config.getTestPropertyB());
        assertEquals("p2.C", config.getTestPropertyC());
        assertEquals(SYSTEM_PROPERTY_E, config.getTestPropertyE());
    }

    @Test
    public void testConfigComponentAnnotation() throws IOException {
        // Test ConfigComponent annotation
        Properties p1 = new Properties();
        p1.setProperty("test.property.a", "p1.A");
        p1.setProperty(TEST_PROPERTY_B, "p1.B");
        p1.setProperty(TEST_PROPERTY_C, "p1.C");
        p1.store(new FileOutputStream(new File(configDir, ANNOTATION_TEST_PREFIX + FILE_ENDING_PROPS)), "test annotated prefix properties file");

        sysprops.setSystemProperty(TEST_PROPERTY_E, SYSTEM_PROPERTY_E);

        // Create config instance and get prefix from config interface annotation
        TestConfig config = ConfigManager.createPrefixConfig(TestConfig.class);

        assertEquals("p1.A", config.getTestPropertyA());
        assertEquals("p1.B", config.getTestPropertyB());
        assertEquals("p1.C", config.getTestPropertyC());
        assertEquals("test.property.default.value.d", config.getTestPropertyD());
        assertEquals(SYSTEM_PROPERTY_E, config.getTestPropertyE());

        sysprops.setSystemProperty("minimal.test.property.e", "system.property.E.minimal");

        // Test how a config interface with no annotation is handled when no prefix is specified
        Properties p2 = new Properties();
        p2.setProperty("minimal.test.property.b", "p2.B");

        // Create a config instance where there is no annotation on the config interface and no prefix specified.
        // The configuration should only come from p2 (which is supplied) and system properties
        MinimalTestConfig cfg = ConfigManager.createPrefixConfig(MinimalTestConfig.class, p2);

        assertEquals("test.property.minimal.default.value.a", cfg.getTestPropertyA());
        assertEquals("p2.B", cfg.getTestPropertyB());
        assertEquals(SYSTEM_PROPERTY_E, config.getTestPropertyE());
    }

    @Test
    public void testPropertyHierarchy() throws IOException {
        String prefix = "test.property.hierarchy";

        Properties p0 = new Properties();
        p0.setProperty(TEST_PROPERTY_B, "p0.B");
        p0.setProperty(TEST_PROPERTY_C, "p0.C");
        p0.setProperty(TEST_PROPERTY_D, "p0.D");
        p0.setProperty(TEST_PROPERTY_E, "p0.E");
        p0.setProperty(TEST_PROPERTY_F, "p0.F");
        p0.setProperty(TEST_PROPERTY_G, "p0.G");
        p0.setProperty(TEST_PROPERTY_H, "p0.H");
        p0.setProperty(TEST_PROPERTY_I, "p0.I");
        p0.setProperty(TEST_PROPERTY_J, "p0.J");
        p0.setProperty(TEST_PROPERTY_K, "p0.K");
        // Should override all default values except A.
        ConfigManager.addGlobalPropertiesSource(p0);

        Properties p1 = new Properties();
        p1.setProperty(TEST_PROPERTY_C, "p1.C");
        p1.setProperty(TEST_PROPERTY_D, "p1.D");
        p1.setProperty(TEST_PROPERTY_E, "p1.E");
        p1.setProperty(TEST_PROPERTY_F, "p1.F");
        p1.setProperty(TEST_PROPERTY_G, "p1.G");
        p1.setProperty(TEST_PROPERTY_H, "p1.H");
        p1.setProperty(TEST_PROPERTY_I, "p1.I");
        p1.setProperty(TEST_PROPERTY_J, "p1.J");
        p1.setProperty(TEST_PROPERTY_K, "p1.K");
        p1.store(new FileOutputStream(new File(configDir, "test.props.defaults")), COMMENT_TEST_PROPERTY_FILES);

        Properties p2 = new Properties();
        p2.setProperty(TEST_PROPERTY_D, "p2.D");
        p2.setProperty(TEST_PROPERTY_E, "p2.E");
        p2.setProperty(TEST_PROPERTY_F, "p2.F");
        p2.setProperty(TEST_PROPERTY_G, "p2.G");
        p2.setProperty(TEST_PROPERTY_H, "p2.H");
        p2.setProperty(TEST_PROPERTY_I, "p2.I");
        p2.setProperty(TEST_PROPERTY_J, "p2.J");
        p2.setProperty(TEST_PROPERTY_K, "p2.K");
        p2.store(new FileOutputStream(new File(configDir, "test.props")), COMMENT_TEST_PROPERTY_FILES);

        Properties p3 = new Properties();
        p3.setProperty(TEST_PROPERTY_E, "p3.E");
        p3.setProperty(TEST_PROPERTY_F, "p3.F");
        p3.setProperty(TEST_PROPERTY_G, "p3.G");
        p3.setProperty(TEST_PROPERTY_H, "p3.H");
        p3.setProperty(TEST_PROPERTY_I, "p3.I");
        p3.setProperty(TEST_PROPERTY_J, "p3.J");
        p3.setProperty(TEST_PROPERTY_K, "p3.K");
        p3.store(new FileOutputStream(new File(configDir, "test.property.props.defaults")), COMMENT_TEST_PROPERTY_FILES);

        Properties p4 = new Properties();
        p4.setProperty(TEST_PROPERTY_F, "p4.F");
        p4.setProperty(TEST_PROPERTY_G, "p4.G");
        p4.setProperty(TEST_PROPERTY_H, "p4.H");
        p4.setProperty(TEST_PROPERTY_I, "p4.I");
        p4.setProperty(TEST_PROPERTY_J, "p4.J");
        p4.setProperty(TEST_PROPERTY_K, "p4.K");
        p4.store(new FileOutputStream(new File(configDir, "test.property.props")), COMMENT_TEST_PROPERTY_FILES);

        Properties p5 = new Properties();
        p5.setProperty(TEST_PROPERTY_G, "p5.G");
        p5.setProperty(TEST_PROPERTY_H, "p5.H");
        p5.setProperty(TEST_PROPERTY_I, "p5.I");
        p5.setProperty(TEST_PROPERTY_J, "p5.J");
        p5.setProperty(TEST_PROPERTY_K, "p5.K");
        p5.store(new FileOutputStream(new File(configDir, "test.property.hierarchy.props.defaults")), COMMENT_TEST_PROPERTY_FILES);

        Properties p6 = new Properties();
        p6.setProperty(TEST_PROPERTY_H, "p6.H");
        p6.setProperty(TEST_PROPERTY_I, "p6.I");
        p6.setProperty(TEST_PROPERTY_J, "p6.J");
        p6.setProperty(TEST_PROPERTY_K, "p6.K");
        p6.store(new FileOutputStream(new File(configDir, "test.property.hierarchy.props")), COMMENT_TEST_PROPERTY_FILES);

        sysprops.setSystemProperty(TEST_PROPERTY_I, "system.property.I");

        TestConfig config = ConfigManager.createPrefixConfig(prefix, TestConfig.class);
        assertEquals(TEST_PROPERTY_DEFAULT_VALUE_A, config.getTestPropertyA());
        assertEquals("p0.B", config.getTestPropertyB());
        assertEquals("p1.C", config.getTestPropertyC());
        assertEquals("p2.D", config.getTestPropertyD());
        assertEquals("p3.E", config.getTestPropertyE());
        assertEquals("p4.F", config.getTestPropertyF());
        assertEquals("p5.G", config.getTestPropertyG());
        assertEquals("p6.H", config.getTestPropertyH());
        assertEquals("system.property.I", config.getTestPropertyI());
    }

    @Test
    public void testConfigFileEnvVar() throws IOException {
        String prefix = "envvarcfgfiletest";

        Properties p1 = new Properties();
        p1.setProperty("test.property.a", "p1.A");
        p1.setProperty(TEST_PROPERTY_B, "p1.B");
        p1.setProperty(TEST_PROPERTY_E, "p1.E");
        p1.store(new FileOutputStream(new File(configDir, prefix + FILE_ENDING_PROPS)), COMMENT_TEST_PROPERTY_FILES);

        Properties p2 = new Properties();
        p2.setProperty(TEST_PROPERTY_B, "p2.B");
        p2.setProperty(TEST_PROPERTY_C, "p2.C");
        p2.setProperty(TEST_PROPERTY_E, "p2.E");
        File configFile = new File(configDir, "arbitrary_config_file.conf");
        p2.store(new FileOutputStream(configFile), "test env var specified properties file");

        sysprops.setSystemProperty(TEST_PROPERTY_E, "system.property.E.envvarcfgfiletest");
        sysprops.setSystemProperty(prefix + ".config.file", configFile.getAbsolutePath());

        TestConfig config = ConfigManager.createPrefixConfig(prefix, TestConfig.class);
        assertEquals("p1.A", config.getTestPropertyA());
        assertEquals("p2.B", config.getTestPropertyB());
        assertEquals("p2.C", config.getTestPropertyC());
        assertEquals("test.property.default.value.d", config.getTestPropertyD());
        assertEquals("system.property.E.envvarcfgfiletest", config.getTestPropertyE());
    }

    @Test
    public void testGetDisplayVersion() {
        String expectedDisplayVersion = "7.0";

        String rawVersion = "7.0";
        assertEquals("7.0 => 7.0", expectedDisplayVersion, ConfigManager.getMajorMinorVersionFromString(rawVersion));

        rawVersion = "7.0-RC1";
        assertEquals("7.0-RC1 => 7.0", expectedDisplayVersion, ConfigManager.getMajorMinorVersionFromString(rawVersion));

        rawVersion = "7.0-SNAPSHOT";
        assertEquals("7.0-SNAPSHOT => 7.0", expectedDisplayVersion, ConfigManager.getMajorMinorVersionFromString(rawVersion));

        rawVersion = "7.0.1";
        assertEquals("7.0.1 => 7.0", expectedDisplayVersion, ConfigManager.getMajorMinorVersionFromString(rawVersion));

        rawVersion = "7.0.1-SNAPSHOT";
        assertEquals("7.0.1-SNAPSHOT => 7.0", expectedDisplayVersion, ConfigManager.getMajorMinorVersionFromString(rawVersion));

        rawVersion = "bar7.0foo";
        assertEquals("bar7.0foo => 7.0", expectedDisplayVersion, ConfigManager.getMajorMinorVersionFromString(rawVersion));

        rawVersion = "7.0sdgsfdr6.0";
        assertEquals("7.0sdgsfdr6.0 => 7.0", expectedDisplayVersion, ConfigManager.getMajorMinorVersionFromString(rawVersion));

        rawVersion = "7.0f1x4 ";
        assertEquals("7.0f1x4 => 7..", expectedDisplayVersion, ConfigManager.getMajorMinorVersionFromString(rawVersion));

        rawVersion = "1x5x7.0f1x4 ";
        assertEquals("1x5x7.0f1x4 => 7.0", expectedDisplayVersion, ConfigManager.getMajorMinorVersionFromString(rawVersion));
    }

}

