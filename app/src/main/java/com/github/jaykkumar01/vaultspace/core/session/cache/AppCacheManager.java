package com.github.jaykkumar01.vaultspace.core.session.cache;

import android.content.Context;

import java.io.File;

public final class AppCacheManager {

    private AppCacheManager() {}

    public static void clearCache(Context context) {
        deleteDir(context.getCacheDir());
    }

    private static void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }
}
