package org.gorpipe.compat.gen;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A repeated top-level key in a curated file silently discards the earlier one:
 * YAML takes the last. Appending a second PRGTGEN section to flag-values.yml did
 * exactly that and dropped two entries, which showed up only as a case count that
 * had fallen rather than risen.
 */
public class UTestDuplicateKeys {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path write(String name, String body) throws IOException {
        Path file = tmp.newFile(name).toPath();
        Files.writeString(file, body);
        return file;
    }

    @Test
    public void flagValuesRejectsARepeatedCommand() throws IOException {
        Path file = write("flag-values.yml",
                "JOIN:\n  \"-maxseg\": \"1000\"\nGROUP:\n  \"-gc\": \"Chrom\"\n"
                        + "JOIN:\n  \"-rprefix\": \"r\"\n");
        try {
            FlagValues.loadFrom(file);
            Assert.fail("expected a repeated key to be rejected");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("JOIN"));
        }
    }

    @Test
    public void commandArgsRejectsARepeatedCommand() throws IOException {
        Path file = write("command-args.yml",
                "JOIN:\n  positional: \"a\"\nJOIN:\n  positional: \"b\"\n");
        try {
            CommandArgs.loadFrom(file);
            Assert.fail("expected a repeated key to be rejected");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("JOIN"));
        }
    }

    @Test
    public void theCuratedFilesThemselvesHaveNoRepeatedKeys() {
        // The real files, so the guard is not only tested against synthetic input.
        Assert.assertFalse(FlagValues.load().has("NOSUCHCOMMAND", "-nosuchflag"));
        Assert.assertEquals("", CommandArgs.load().requiredFlags("NOSUCHCOMMAND"));
        Assert.assertEquals("", InputSourceMatrixGenerator.sourceArgs()
                .requiredFlags("NOSUCHSOURCE"));
    }
}
