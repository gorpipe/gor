package org.gorpipe.gor.model;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;

public class UTestBaseMeta {

    @Test
    public void testReadFromGorz() {
        BaseMeta meta = new BaseMeta();
        meta.loadAndMergeMeta(Path.of("../tests/data/gor/genes.gorz"));
        Assert.assertEquals("Chrom\tgene_start\tgene_end\tGene_Symbol", String.join("\t", meta.getColumns()));
    }
}
