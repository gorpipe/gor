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

package gorsat;

import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.querydialogs.factory.AbstractDialogFactory;
import org.gorpipe.querydialogs.templating.TemplateCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * Yml template rendering ({@link FreemarkerQueryUtilities#requestQuery}) must be safe to call concurrently, must pick up
 * edited template files and must not re-parse an unchanged template on every call.
 */
public class UTestTemplateRenderingConcurrency {

    private static final String TEMPLATE = String.join("\n",
            "render_test:",
            "  query: |",
            "    <#setting number_format=\"computer\">",
            "    <#assign c = (chrom.val!\"\")?remove_beginning(\"chr\")>",
            "    <#if !(chrom.val!\"\")?matches(\"(chr)?([0-9]{1,2}|X|Y)\") || !(pos.val!\"\")?matches(\"[0-9]+\")>",
            "    norrows 1 | calc error 'invalid input'",
            "    <#else>",
            "    gorrow chr${c},${pos.val},${pos.val} | calc ref '${ref.val}' | calc alt '${alt.val}'",
            "    | where ${skip('and')} 1=1 <#if tag.val??>${skip('and')} tag = '${tag.val}'</#if>",
            "    | calc w ${width.val}",
            "    </#if>",
            "  arguments:",
            "    - name: chrom",
            "    - name: pos",
            "    - name: ref",
            "    - name: alt",
            "    - name: tag",
            "      optional: true",
            "    - name: width",
            "      type: number",
            "      default: 10",
            "  perspectives:",
            "    - name: near",
            "      filter: \"${skip('and')} pos > 0 ${skip('and')} tag_filter = '${tag.val!''}'\"",
            "");

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private FileReader fileReader;
    private String cacheDir;

    @Before
    public void setUp() throws IOException {
        fileReader = new DriverBackedFileReader("");
        cacheDir = workDir.newFolder("cache").getAbsolutePath();
    }

    private String render(String yml, Map<String, String> params) throws Exception {
        return FreemarkerQueryUtilities.requestQuery(yml, fileReader, null, null, params, cacheDir).orElseThrow();
    }

    private static Map<String, String> params(int i) {
        Map<String, String> p = new HashMap<>();
        p.put("chrom", "chr" + (1 + i % 22));
        p.put("pos", String.valueOf(1000 + i));
        p.put("ref", i % 2 == 0 ? "A" : "C");
        p.put("alt", i % 3 == 0 ? "G" : "T");
        if (i % 4 == 0) p.put("tag", "t" + i);
        if (i % 5 == 0) p.put("width", String.valueOf(i));
        if (i % 7 == 0) p.put("perspective", "near");
        return p;
    }

    private Path writeTemplate(String name, String content) throws IOException {
        Path yml = workDir.getRoot().toPath().resolve(name);
        Files.writeString(yml, content);
        return yml;
    }

    @Test
    public void renderOutputReflectsArguments() throws Exception {
        String yml = writeTemplate("render_test.yml", TEMPLATE).toString();

        String withTag = render(yml, params(0));
        Assert.assertTrue(withTag, withTag.startsWith("gorrow chr1,1000,1000 | calc ref 'A' | calc alt 'G'"));
        Assert.assertTrue(withTag, withTag.contains("| where  1=1 and tag = 't0'"));
        Assert.assertTrue(withTag, withTag.contains("| calc w 0"));
        Assert.assertTrue(withTag, withTag.endsWith("| where pos > 0 and tag_filter = 't0'"));

        String withoutTag = render(yml, params(1));
        Assert.assertTrue(withoutTag, withoutTag.startsWith("gorrow chr2,1001,1001 | calc ref 'C' | calc alt 'T'"));
        Assert.assertTrue(withoutTag, withoutTag.contains("| calc w 10"));
        // Optional argument from the previous call must not leak into this one.
        Assert.assertFalse(withoutTag, withoutTag.contains("tag"));
    }

    @Test
    public void concurrentRendersWithDifferentArgumentsProduceTheirOwnOutput() throws Exception {
        String yml = writeTemplate("render_test.yml", TEMPLATE).toString();
        int distinct = 200;

        Map<Integer, String> expected = new HashMap<>();
        for (int i = 0; i < distinct; i++) {
            expected.put(i, render(yml, params(i)));
        }
        Assert.assertEquals("expected outputs should be distinct", distinct, new HashSet<>(expected.values()).size());

        int threads = 8;
        int perThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<String>>> futures = new ArrayList<>();
        try {
            for (int t = 0; t < threads; t++) {
                final int offset = t * 37;
                futures.add(executor.submit(() -> {
                    List<String> errors = new ArrayList<>();
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        int k = (offset + i) % distinct;
                        String actual = render(yml, params(k));
                        if (!expected.get(k).equals(actual)) {
                            errors.add("params " + k + ": expected <" + expected.get(k) + "> but got <" + actual + ">");
                        }
                    }
                    return errors;
                }));
            }
            start.countDown();
            List<String> errors = new ArrayList<>();
            for (Future<List<String>> f : futures) {
                errors.addAll(f.get(2, TimeUnit.MINUTES));
            }
            Assert.assertTrue(errors.size() + " wrong renders, first: " + errors.stream().findFirst().orElse(""), errors.isEmpty());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void editedTemplateIsPickedUp() throws Exception {
        Path yml = writeTemplate("edited.yml", "edited:\n  query: gorrow chr1,1,1 | calc v 'one-${x.val}'\n  arguments:\n    - name: x\n");
        Assert.assertEquals("gorrow chr1,1,1 | calc v 'one-a'", render(yml.toString(), Map.of("x", "a")));

        // Same length and same modification time: only the content differs.
        long modified = Files.getLastModifiedTime(yml).toMillis();
        Files.writeString(yml, "edited:\n  query: gorrow chr1,1,1 | calc v 'two-${x.val}'\n  arguments:\n    - name: x\n");
        Files.setLastModifiedTime(yml, java.nio.file.attribute.FileTime.fromMillis(modified));

        Assert.assertEquals("gorrow chr1,1,1 | calc v 'two-b'", render(yml.toString(), Map.of("x", "b")));
    }

    @Test
    public void unchangedTemplateIsNotReparsed() throws Exception {
        String yml = writeTemplate("cached.yml", TEMPLATE).toString();

        render(yml, params(1));
        long parses = AbstractDialogFactory.definitionParseCount();
        long compiles = TemplateCache.compileCount();

        for (int i = 2; i < 50; i++) {
            render(yml, params(i));
        }

        Assert.assertEquals("yml should be parsed once while unchanged", parses, AbstractDialogFactory.definitionParseCount());
        Assert.assertEquals("query template should be compiled once while unchanged", compiles, TemplateCache.compileCount());

        Files.writeString(Path.of(yml), TEMPLATE.replace("calc w", "calc width"));
        String edited = render(yml, params(1));
        Assert.assertTrue(edited, edited.contains("| calc width 10"));
        Assert.assertEquals(parses + 1, AbstractDialogFactory.definitionParseCount());
    }

    @Test
    public void embeddedFileIsRestoredIfCacheDirIsCleaned() throws Exception {
        Path yml = writeTemplate("embedded.yml", String.join("\n",
                "embedded:",
                "  query: nor ${script} | calc v '${x.val}'",
                "  script: |",
                "    embedded file content",
                "  arguments:",
                "    - name: x",
                ""));

        String first = render(yml.toString(), Map.of("x", "a"));
        Path embedded = Path.of(first.substring(4, first.indexOf(' ', 4)));
        Assert.assertEquals("embedded file content\n", Files.readString(embedded));

        Files.delete(embedded);
        String second = render(yml.toString(), Map.of("x", "b"));
        Assert.assertEquals(first.replace("'a'", "'b'"), second);
        Assert.assertEquals("embedded file content\n", Files.readString(embedded));
    }
}
