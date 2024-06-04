package org.gorpipe.gor.driver.providers.stream.sources;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.DataSource;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Common files tests.
 *
 * Based on the files in: csa_test_data/data_sets/bvl_min
 */
public abstract class CommonFilesTests {

    private static final Logger log = LoggerFactory.getLogger(CommonStreamTests.class);


    protected abstract String getDataName(String name);

    protected abstract DataSource createSource(String name) throws IOException;

    protected String securityContext() throws IOException {
        return null;
    }

    @Test
    public void testExists() throws IOException {

        try (DataSource fs = createSource(getDataName("csa_manifest.txt"))) {
            Assert.assertTrue(fs.exists());
        }

        try (DataSource fs = createSource(getDataName("no_such_file"))) {
            Assert.assertFalse(fs.exists());
        } catch (FileNotFoundException fnfe) {
            // Get this for http, ignore.
        }

        try (DataSource fs = createSource(getDataName("bam/"))) {
            Assert.assertTrue(fs.exists());
        }
    }

    @Test
    public void testIsDirectory() throws IOException {

        try (DataSource fs = createSource(getDataName("bam"))) {
            Assert.assertTrue(fs.isDirectory());
        }

        try (DataSource fs = createSource(getDataName("no_such_file"))) {
            Assert.assertFalse(fs.isDirectory());
        }

        try (DataSource fs = createSource(getDataName("README"))) {
            Assert.assertFalse(fs.isDirectory());
        }
    }

    @Test
    public void testCreateDeleteListEmtpyDirectory() throws IOException {
        try (DataSource fs = createSource(getDataName("empty_dir/"))) {
            if (fs.exists()) {
                fs.delete();
            }

            fs.createDirectory();
            Assert.assertTrue(fs.isDirectory());
            Assert.assertTrue(fs.exists());

            List<String> files = fs.list().collect(Collectors.toList());
            Assert.assertEquals(0, files.size());

        } finally {
            try (DataSource fs = createSource(getDataName("empty_dir/"))) {
                if (fs.exists()) {
                    fs.delete();
                }
                Assert.assertFalse(fs.exists());
            }
        }
    }

    @Test
    public void testList() throws IOException {
        try (DataSource fs = createSource(getDataName("bam"))) {
            List<String> files = fs.list().collect(Collectors.toList());
            Assert.assertEquals(8, files.size());
        }

        try (DataSource fs = createSource(getDataName("bam/"))) {
            List<String> files = fs.list().collect(Collectors.toList());
            Assert.assertEquals(8, files.size());
        }

        try (DataSource fs = createSource(getDataName("README"))) {
            List<String> files = fs.list().collect(Collectors.toList());
        } catch (GorResourceException e) {
            // Expected.
        }

        try (DataSource fs = createSource(getDataName("derived"))) {
            List<String> files = fs.list().collect(Collectors.toList());
            Assert.assertEquals(3, files.size());
        }
    }
}
