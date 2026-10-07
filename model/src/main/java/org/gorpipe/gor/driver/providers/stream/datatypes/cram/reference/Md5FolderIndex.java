package org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Index from md5 to reference data for a reference folder.  Shared by all reference sources using the same folder,
 * as scanning the folder is expensive.
 *
 * The folder is rescanned when the index is older than the rescan interval.  A rescan builds a new map and swaps it in,
 * so readers never see a partially built (or cleared) index (see ENGKNOW-3778).
 */
class Md5FolderIndex<V> {

    public static final String KEY_RESCAN_INTERVAL = "gor.driver.cram.reference.rescaninterval"; // Seconds

    private static final long NOT_SCANNED = -1;

    private final Path folder;
    private final Function<Path, Map<String, V>> scanner;

    private volatile Map<String, V> md5Map = new ConcurrentHashMap<>();
    private long lastScanMillis = NOT_SCANNED;

    /**
     * Get the index for the given folder, creating it if needed.
     */
    static <V> Md5FolderIndex<V> get(Map<Path, Md5FolderIndex<V>> indexByFolder, Path folder,
                                     Function<Path, Map<String, V>> scanner) {
        return indexByFolder.computeIfAbsent(folder.toAbsolutePath().normalize(),
                f -> new Md5FolderIndex<>(f, scanner));
    }

    private Md5FolderIndex(Path folder, Function<Path, Map<String, V>> scanner) {
        this.folder = folder;
        this.scanner = scanner;
    }

    V get(String md5) {
        return md5 != null ? md5Map.get(md5) : null;
    }

    synchronized void put(String md5, V value) {
        md5Map.put(md5, value);
    }

    Collection<V> values() {
        return md5Map.values();
    }

    /**
     * Rescan the folder if it has not been scanned or the index is older than the rescan interval.
     */
    synchronized void refreshIfStale() {
        long now = System.currentTimeMillis();
        long intervalMillis = Long.parseLong(System.getProperty(KEY_RESCAN_INTERVAL, "300")) * 1000;
        if (lastScanMillis == NOT_SCANNED || now - lastScanMillis >= intervalMillis) {
            md5Map = new ConcurrentHashMap<>(scanner.apply(folder));
            lastScanMillis = now;
        }
    }
}
