package org.gorpipe.compat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes inventory/surface.json from the live registries. */
public final class InventoryMain {

    private InventoryMain() {
    }

    public static void main(String[] args) {
        SurfaceInventory inventory = SurfaceInventory.read();
        Path out = CaseLoader.moduleRoot().resolve("inventory/surface.json");
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, inventory.toJson(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + out, e);
        }
        System.out.printf("Wrote %s: %d commands, %d flags, %d functions.%n",
                out, inventory.commands().size(), inventory.totalFlagCount(),
                inventory.functionNames().size());
    }
}
