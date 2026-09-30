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
import org.gorpipe.gor.model.QueryEvaluator;
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
        return render(yml, params, null);
    }

    private String render(String yml, Map<String, String> params, QueryEvaluator queryEval) throws Exception {
        return FreemarkerQueryUtilities.requestQuery(yml, fileReader, queryEval, null, params, cacheDir).orElseThrow();
    }

    /**
     * Records every query passed to the template gor(...) method.
     */
    private static final class RecordingQueryEvaluator extends QueryEvaluator {
        final List<String> queries = Collections.synchronizedList(new ArrayList<>());

        @Override
        public List<String> asList(String query) {
            queries.add(query);
            return List.of("v");
        }

        @Override
        public String asValue(String query) {
            queries.add(query);
            return "v";
        }

        @Override
        public String[] getHeader() {
            return new String[0];
        }
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

        // The counters are JVM wide, so only compare how much they change while this test renders.
        long parsesBefore = AbstractDialogFactory.definitionParseCount();
        long compilesBefore = TemplateCache.compileCount();
        for (int i = 2; i < 50; i++) {
            render(yml, params(i));
        }
        Assert.assertEquals("yml should not be re-parsed while unchanged", 0, AbstractDialogFactory.definitionParseCount() - parsesBefore);
        Assert.assertEquals("query template should not be re-compiled while unchanged", 0, TemplateCache.compileCount() - compilesBefore);

        parsesBefore = AbstractDialogFactory.definitionParseCount();
        Files.writeString(Path.of(yml), TEMPLATE.replace("calc w", "calc width"));
        String edited = render(yml, params(1));
        Assert.assertTrue(edited, edited.contains("| calc width 10"));
        Assert.assertEquals("edited yml should be parsed once", 1, AbstractDialogFactory.definitionParseCount() - parsesBefore);
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

    private Path writeEmbeddedTemplate() throws IOException {
        return writeTemplate("embedded.yml", String.join("\n",
                "embedded:",
                "  query: nor ${script} | calc v '${x.val}'",
                "  script: |",
                "    embedded file content",
                "  arguments:",
                "    - name: x",
                ""));
    }

    @Test
    public void embeddedFileIsRestoredIfOverwritten() throws Exception {
        Path yml = writeEmbeddedTemplate();

        String first = render(yml.toString(), Map.of("x", "a"));
        Path embedded = Path.of(first.substring(4, first.indexOf(' ', 4)));
        Assert.assertEquals("embedded file content\n", Files.readString(embedded));

        Files.writeString(embedded, "something else\n");
        String second = render(yml.toString(), Map.of("x", "b"));
        Assert.assertEquals(first.replace("'a'", "'b'"), second);
        Assert.assertEquals("embedded file content\n", Files.readString(embedded));
    }

    @Test
    public void ymlWithGlobalTagIsRejected() throws Exception {
        Path yml = writeTemplate("tagged.yml", String.join("\n",
                "tagged:",
                "  query: nor x",
                "  file: !!java.io.File [\"/tmp\"]",
                ""));
        try {
            render(yml.toString(), Map.of());
            Assert.fail("A yml with a global (java class) tag must not be loaded");
        } catch (Exception e) {
            Throwable t = e;
            while (t != null && !(t instanceof org.yaml.snakeyaml.error.YAMLException)) t = t.getCause();
            Assert.assertNotNull("expected a YAMLException, got " + e, t);
        }
    }

    @Test
    public void ymlWithStandardTypesParses() throws Exception {
        Path yml = writeTemplate("types.yml", String.join("\n",
                "types:",
                "  query: nor x | calc v '${x.val}'",
                "  Version_info: 1.0",
                "  when: 2024-01-02",
                "  flag: true",
                "  count: 3",
                "  arguments:",
                "    - name: x",
                "      default: 7",
                ""));
        Assert.assertEquals("nor x | calc v 'b'", render(yml.toString(), Map.of("x", "b")));
    }

    @Test
    public void queryIsRenderedOncePerRequest() throws Exception {
        Path yml = writeTemplate("gor_call.yml", String.join("\n",
                "gor_call:",
                "  query: nor x | calc v '${gor(\"q-\" + (x.val!\"\"))}'",
                "  arguments:",
                "    - name: x",
                ""));
        for (String x : List.of("a", "b")) {
            RecordingQueryEvaluator queryEval = new RecordingQueryEvaluator();
            Assert.assertEquals("nor x | calc v 'v'", render(yml.toString(), Map.of("x", x), queryEval));
            Assert.assertEquals("gor(...) should run once, with the arguments set", List.of("q-" + x), queryEval.queries);
        }
    }

    @Test
    public void failingRenderThrowsTemplateException() throws Exception {
        Path yml = writeTemplate("failing.yml", String.join("\n",
                "failing:",
                "  query: nor x | calc v ${x.val?number}",
                "  arguments:",
                "    - name: x",
                ""));
        Assert.assertEquals("nor x | calc v 1", render(yml.toString(), Map.of("x", "1")));
        try {
            render(yml.toString(), Map.of("x", "abc"));
            Assert.fail("Expected the render to fail");
        } catch (freemarker.template.TemplateException e) {
            // Thrown as the TemplateException requestQuery declares, not wrapped in a RuntimeException.
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("abc"));
        }
    }

    @Test
    public void argumentsNamedSkipOrGorTakePrecedenceOverTheBuiltIns() throws Exception {
        Path yml = writeTemplate("builtin_names.yml", String.join("\n",
                "builtin_names:",
                "  query: nor x | calc s '${skip.val}' | calc g '${gor.val}' | calc x '${x.val}'",
                "  arguments:",
                "    - name: skip",
                "    - name: gor",
                "    - name: x",
                ""));
        RecordingQueryEvaluator queryEval = new RecordingQueryEvaluator();
        Assert.assertEquals("nor x | calc s 's1' | calc g 'g1' | calc x 'x1'",
                render(yml.toString(), Map.of("skip", "s1", "gor", "g1", "x", "x1"), queryEval));
        Assert.assertTrue(queryEval.queries.toString(), queryEval.queries.isEmpty());
    }
}
