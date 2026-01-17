package com.github.jaykkumar01.vaultspace.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;

public final class UploadMetadataResolver {

    @NonNull
    public static String resolveDisplayName(
            @NonNull Context context,
            @NonNull Uri uri
    ){
        Cursor c = context.getContentResolver().query(
                uri,
                new String[]{ OpenableColumns.DISPLAY_NAME },
                null,
                null,
                null
        );

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) return c.getString(idx);
                }
            } finally {
                c.close();
            }
        }

        // Fallback (last segment)
        String last = uri.getLastPathSegment();
        return last != null ? last : "unknown";
    }
}
