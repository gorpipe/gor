package org.gorpipe.compat.gen;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A repeated key in a curated file silently discards the earlier one: YAML takes
 * the last. Appending a second PRGTGEN section to flag-values.yml did exactly that
 * and dropped two entries, which showed up only as a case count that had fallen
 * rather than risen. Merging the two sections then left six flags listed twice
 * inside the one block, where the same rule applies and the first value is the one
 * that goes — so both levels are guarded.
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
    public void flagValuesRejectsAFlagRepeatedWithinOneCommand() throws IOException {
        // What the merge of the two PRGTGEN sections left behind: one block, the
        // same flag twice. SnakeYAML only warns, and the first value is discarded.
        Path file = write("flag-values.yml",
                "PRGTGEN:\n  \"-psep\": \",\"\n  \"-rd\": \"Pos\"\n"
                        + "  \"-psep\": \";\"\n  \"-rd\": \"1\"\n");
        try {
            FlagValues.loadFrom(file);
            Assert.fail("expected a repeated nested key to be rejected");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("PRGTGEN -psep"));
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("PRGTGEN -rd"));
        }
    }

    @Test
    public void aQuotedKeyWhoseValueIsAColonIsNotMistakenForARepeat() throws IOException {
        // DAGMAP's -ps carries ":" as its value. A naive split on the first colon
        // would read the key as -ps and the value as ": \"" for one entry and as
        // something else for the next, so distinct flags must stay distinct.
        Path file = write("flag-values.yml",
                "DAGMAP:\n  \"-ps\": \":\"\n  \"-m\": \"NA\"\n");
        FlagValues values = FlagValues.loadFrom(file);
        Assert.assertEquals(":", values.valueFor("DAGMAP", "-ps"));
        Assert.assertEquals("NA", values.valueFor("DAGMAP", "-m"));
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
