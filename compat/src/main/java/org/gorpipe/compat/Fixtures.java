package org.gorpipe.compat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Canonical fixtures. Expanded in Task 8 with the shared data set and the
 * synthetic chromSeq reference build.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /**
     * Returns the engine config file for a case root, or null when the case does
     * not need one. Task 8 makes this return the generated reference-build config.
     */
    public static Path configFileIfPresent(Path root) {
        Path config = root.resolve("gor_config.txt");
        return Files.exists(config) ? config : null;
    }

    /** Reads a shared fixture from compat/data by file name. */
    public static String readSharedData(String name, String caseId) {
        if (name == null) {
            throw new IllegalStateException("Case " + caseId
                    + " declares an input with neither content nor contentFile");
        }
        Path path = CaseLoader.moduleRoot().resolve("data").resolve(name);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Missing shared fixture " + path + " referenced by case " + caseId, e);
        }
    }
}
