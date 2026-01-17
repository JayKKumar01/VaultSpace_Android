package com.github.jaykkumar01.vaultspace.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;

public final class UriUtils {

    private UriUtils() {}

    /**
     * Fast, metadata-only validation for picker-provided Uris.
     * Does NOT open streams or read file data.
     */
    public static boolean isUriAccessible(
            @NonNull Context context,
            @NonNull Uri uri
    ) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = null;

        try {
            cursor = resolver.query(
                    uri,
                    new String[]{
                            OpenableColumns.SIZE,
                            OpenableColumns.DISPLAY_NAME
                    },
                    null,
                    null,
                    null
            );

            // If provider is dead / permission revoked
            if (cursor == null || !cursor.moveToFirst()) {
                return false;
            }

            // Row existence is the real signal
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex == -1) {
                return false;
            }

            String name = cursor.getString(nameIndex);
            return name != null && !name.isEmpty();

        } catch (SecurityException e) {
            // Permission expired / revoked
            return false;
        } catch (Exception e) {
            // Provider failure / invalid Uri
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
