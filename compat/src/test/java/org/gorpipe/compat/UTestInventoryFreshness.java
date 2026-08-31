package org.gorpipe.compat;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The committed inventory must match the live registries. When a command gains a
 * flag this fails until someone regenerates, which is how new surface becomes a
 * visible diff in review rather than silently uncovered.
 */
public class UTestInventoryFreshness {

    @Test
    public void committedInventoryMatchesRegistries() {
        Path committed = CaseLoader.moduleRoot().resolve("inventory/surface.json");
        Assert.assertTrue("inventory/surface.json is missing; run ./gradlew :compat:inventory",
                Files.exists(committed));

        String onDisk;
        try {
            onDisk = Files.readString(committed, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (!SurfaceInventory.read().toJson().equals(onDisk)) {
            Assert.fail("inventory/surface.json is stale — the language surface changed.\n"
                    + "  Run ./gradlew :compat:inventory and commit the result, then add "
                    + "cases for any new surface.");
        }
    }
}
