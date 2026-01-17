package com.github.jaykkumar01.vaultspace.utils;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.InputStream;

public final class UriUtilsDebug {

    private UriUtilsDebug() {}

    /**
     * TEMP / DEBUG ONLY
     *
     * Tries to actually open the file stream.
     * This confirms whether the URI permission is still valid.
     *
     * ❌ Slow
     * ❌ Reads from provider
     * ❌ Do NOT use in production
     */
    public static boolean isUriReadableDebug(
            @NonNull Context context,
            @NonNull Uri uri
    ) {
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            return is != null;
        } catch (Exception e) {
            return false;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
