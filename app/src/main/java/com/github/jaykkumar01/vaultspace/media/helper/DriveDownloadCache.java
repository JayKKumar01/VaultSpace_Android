package com.github.jaykkumar01.vaultspace.media.helper;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public final class DriveDownloadCache {

    private static final long MAX_BYTES = 120L * 1024L * 1024L; // 120 MB

    private final File dir;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public DriveDownloadCache(@NonNull Context context) {
        dir = new File(context.getCacheDir(), "drive_download_cache");
        if (!dir.exists()) dir.mkdirs();
    }

    /* ---------------- public api ---------------- */

    @NonNull
    public synchronized File getFile(@NonNull String fileId) {
        return new File(dir, "drive_" + fileId);
    }

    public synchronized void touch(@NonNull File file) {
        file.setLastModified(System.currentTimeMillis());
    }

    /**
     * LRU eviction with working-set semantics.
     *
     * Rules:
     * 1. Normal LRU eviction while total > MAX_BYTES
     * 2. If the newest file alone exceeds MAX_BYTES,
     *    keep ONLY that file and evict everything else.
     */
    public synchronized void evictIfNeeded() {
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return;

        long total = 0;
        for (File f : files) total += f.length();
        if (total <= MAX_BYTES) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        File newest = files[files.length - 1];

        // 🔑 single huge file case → keep only newest
        if (newest.length() > MAX_BYTES) {
            for (File f : files) {
                if (f != newest) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
            return;
        }

        // 🔁 standard LRU eviction
        for (File f : files) {
            if (total <= MAX_BYTES) break;
            long len = f.length();
            //noinspection ResultOfMethodCallIgnored
            f.delete();
            total -= len;
        }
    }

    /* ---------------- optional ---------------- */

    public synchronized void clear() {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
