package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class UTestBaselineStore {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static CompatCase baselineCase(String id, String query) {
        CompatCase c = new CompatCase();
        c.id = id;
        c.tier = "baseline";
        c.mode = "exact";
        c.query = query;
        return c;
    }

    @Test
    public void pathIsDerivedFromCategoryAndFeature() {
        Path p = BaselineStore.pathFor(baselineCase("cmd.norrows.basic_one_row", "norrows 1"));
        Assert.assertTrue(p.toString(), p.toString().endsWith("baselines/cmd/norrows.out"));
    }

    @Test
    public void rendersSuccessAsHeaderAndRows() {
        CompatResult r = CaseRunner.run(baselineCase("cmd.norrows.x", "norrows 2"));
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum\nchrN\t0\t0\nchrN\t0\t1\n",
                BaselineStore.render(r));
    }

    @Test
    public void rendersFailureWithErrorPrefix() {
        CompatResult r = CaseRunner.run(baselineCase("cmd.x.bad", "norrows 1 | nosuchcommand"));
        Assert.assertTrue(r.failed());
        Assert.assertTrue(BaselineStore.render(r).startsWith("ERROR: "));
        Assert.assertTrue(BaselineStore.render(r).contains("NOSUCHCOMMAND"));
    }

    @Test
    public void roundTripsThroughAFile() throws IOException {
        Path file = tmp.newFile("norrows.out").toPath();
        Map<String, String> written = new LinkedHashMap<>();
        written.put("cmd.norrows.b", "ChromNOR\tPosNOR\tRowNum\nchrN\t0\t0\n");
        written.put("cmd.norrows.a", "ERROR: boom\n");

        BaselineStore.write(file, written);
        Map<String, String> read = BaselineStore.loadAll(file.getParent());

        Assert.assertEquals(2, read.size());
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum\nchrN\t0\t0\n", read.get("cmd.norrows.b"));
        Assert.assertEquals("ERROR: boom\n", read.get("cmd.norrows.a"));
    }

    @Test
    public void writesBlocksSortedByIdForStableDiffs() throws IOException {
        Path file = tmp.newFile("sorted.out").toPath();
        Map<String, String> written = new LinkedHashMap<>();
        written.put("cmd.z.last", "z\n");
        written.put("cmd.a.first", "a\n");

        BaselineStore.write(file, written);
        String content = java.nio.file.Files.readString(file);

        Assert.assertTrue(content.indexOf("cmd.a.first") < content.indexOf("cmd.z.last"));
    }
}
