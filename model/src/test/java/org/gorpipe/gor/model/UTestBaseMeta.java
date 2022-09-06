package org.gorpipe.gor.model;

import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

public class UTestBaseMeta {

    @Rule
    public final TemporaryFolder tf = new TemporaryFolder();

    @Test
    public void testReadFromGorz() {
        BaseMeta meta = new BaseMeta();
        meta.loadAndMergeMeta(Path.of("../tests/data/gor/genes.gorz"));
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol", String.join("\t", meta.getColumns()));
    }

    @Test
    public void testReadFromColumnCompressedGorz() {
        String columnCompressedGorz = Path.of(tf.getRoot().getAbsolutePath()).resolve("genes.cc.gorz").toString();
        TestUtils.runGorPipe("gor ../tests/data/gor/genes.gorz | write -c " + columnCompressedGorz);
        BaseMeta meta = new BaseMeta();
        meta.loadAndMergeMeta(Path.of(columnCompressedGorz));

        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol", String.join("\t", meta.getColumns()));
    }

}
