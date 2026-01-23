package com.github.jaykkumar01.vaultspace.utils;

import android.annotation.SuppressLint;

public final class ByteFormat {

    private ByteFormat() {}

    @SuppressLint("DefaultLocale")
    public static String human(long bytes) {
        if (bytes < 1024) return bytes + " B";

        float v = bytes;
        String[] u = {"KB", "MB", "GB", "TB"};
        int i = -1;

        do {
            v /= 1024f;
            i++;
        } while (v >= 1024 && i < u.length - 1);

        return String.format("%.1f %s", v, u[i]);
    }
}
