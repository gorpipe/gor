package org.gorpipe.gor.driver.utils;


import org.gorpipe.model.genome.files.gor.GenomicIterator;
import com.google.common.base.Joiner;
import org.gorpipe.gor.driver.GorDriver;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.meta.SourceReferenceBuilder;
import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.model.genome.files.gor.Row;
import com.sun.management.UnixOperatingSystemMXBean;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.SystemUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by villi on 25/08/15.
 */
public class TestUtils {
    static public GorDriver gorDriver;
    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

    static {
        gorDriver = GorDriverFactory.fromConfig();
    }

    public static String getTestFile(String name) {
        String canonicalPath = getCanonicalPath(name);
        if (canonicalPath == null) {
            canonicalPath = getCanonicalPath("dummy.gor");
        }
        return canonicalPath;
    }


    private static String getCanonicalPath(String name) {
        Path testDataPath = Paths.get("../tests/data/" + name);
        String canonicalPath = null;
        try {
            canonicalPath = testDataPath.toFile().getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return canonicalPath;
    }

    public static String md5(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("MD5").digest(data);
        return new String(Hex.encodeHex(digest));
    }

    public static String md5(byte[] data, int off, int len) throws Exception {
        return md5(new String(data, off, len).getBytes());
    }

    public static String readFile(File file) throws IOException {
        try (FileInputStream str = new FileInputStream(file)) {
            return StreamUtils.readString(str, (int) file.length());
        }
    }

    /**
     * Read source through gor driver and compare to expected data. Data should include header
     */
    public static void assertFullGor(String context, String source, String expectedData) throws IOException {
        GenomicIterator iterator = gorDriver.createIterator(new SourceReferenceBuilder(source).securityContext(context).build());
        iterator.init(null);
        StringBuilder builder = new StringBuilder();

        addHeader(builder, iterator);
        addLines(builder, iterator, null);

        Assert.assertEquals("Checking full gor on " + source, expectedData, builder.toString());
    }

    public static void addHeader(StringBuilder builder, GenomicIterator iterator) {
        builder.append(iterator.getHeader());
        builder.append("\n");
    }

    public static void addLines(StringBuilder builder, GenomicIterator iterator, Integer lineCount) {
        int lines = 0;
        while (iterator.hasNext() && (lineCount == null || lines < lineCount)) {
            Row line = iterator.next();
            builder.append(line.toString());
            builder.append("\n");
            lines++;
        }
    }

    // Helper method to check if file has an open file handle.
    public static boolean isFileClosed(File file) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            // TODO: Windows does not have lsof so this will not be working on windows.
            return true;
        }

        Process plsof = null;
        BufferedReader reader = null;
        try {
            plsof = new ProcessBuilder(new String[]{"lsof", "|", "grep", file.getAbsolutePath()}).start();
            reader = new BufferedReader(new InputStreamReader(plsof.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(file.getAbsolutePath())) {
                    return false;
                }
            }
        } finally {
            if (reader != null) reader.close();
            if (plsof != null) plsof.destroy();
        }
        return true;
    }

    public static int getActivePoolConnections(String dbUrl, String user) throws MalformedObjectNameException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + dbUrl.replace(":", "-") + "-" + user + ")");
        HikariPoolMXBean poolProxy = JMX.newMXBeanProxy(mBeanServer, poolName, HikariPoolMXBean.class);
        return poolProxy.getActiveConnections();
    }

    public static long countOpenFiles() throws IOException, InterruptedException {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            String pid = name.substring(0, name.indexOf('@'));
            Process p = Runtime.getRuntime().exec("lsof -p " + pid);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = p.getInputStream();
            try {
                int r = is.read();
                while (r != -1) {
                    baos.write(r);
                    r = is.read();
                }
            } finally {
                try {
                    is.close();
                } finally {
                    baos.close();
                    p.destroyForcibly();
                    p.waitFor();
                }
            }
            String[] split = baos.toString().split("\n");
            return Arrays.asList(split).stream().filter(ps -> ps.contains("REG")).collect(Collectors.toList()).size();
        } else {
            log.warn("Unable to do open file count on this platform");
            return -1;
        }
    }
}