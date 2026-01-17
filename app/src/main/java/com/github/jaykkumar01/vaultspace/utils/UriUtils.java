package com.github.jaykkumar01.vaultspace.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;

public final class UriUtils {

    private UriUtils() {}

    public static boolean isUriAccessibleDebug(
            @NonNull Context context,
            @NonNull Uri uri,
            @NonNull String tag
    ) {
        ContentResolver resolver = context.getContentResolver();

        try (Cursor cursor = resolver.query(
                uri,
                new String[]{
                        OpenableColumns.SIZE,
                        OpenableColumns.DISPLAY_NAME
                },
                null,
                null,
                null
        )) {

            if (cursor == null) {
                Log.w(tag, "Uri check FAILED → cursor == null | uri=" + uri);
                return false;
            }

            if (!cursor.moveToFirst()) {
                Log.w(tag, "Uri check FAILED → moveToFirst=false | uri=" + uri);
                return false;
            }

            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex == -1) {
                Log.w(tag, "Uri check FAILED → DISPLAY_NAME missing | uri=" + uri);
                return false;
            }

            String name = cursor.getString(nameIndex);
            boolean ok = name != null && !name.isEmpty();

            Log.d(tag, "Uri check OK=" + ok +
                    " | name=" + name +
                    " | thread=" + Thread.currentThread().getName());

            return ok;

        } catch (SecurityException e) {
            Log.e(tag, "Uri check FAILED → SecurityException | uri=" + uri, e);
            return false;

        } catch (Exception e) {
            Log.e(tag, "Uri check FAILED → Exception | uri=" + uri, e);
            return false;
        }
    }


    /**
     * Fast, metadata-only validation for picker-provided Uris.
     * Does NOT open streams or read file data.
     */
    public static boolean isUriAccessible(
            @NonNull Context context,
            @NonNull Uri uri
    ) {
        ContentResolver resolver = context.getContentResolver();

        try (Cursor cursor = resolver.query(
                uri,
                new String[]{
                        OpenableColumns.SIZE,
                        OpenableColumns.DISPLAY_NAME
                },
                null,
                null,
                null
        )) {

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
        }
    }
}
