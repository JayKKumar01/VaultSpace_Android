package com.github.jaykkumar01.vaultspace.utils;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class Mp4LayoutLogger {

    private static final String TAG = "VaultSpace:Mp4Layout";
    private static final long SMALL_HEAD_BYTES = 64 * 1024; // 64KB is more than enough

    private Mp4LayoutLogger() {}

    public static void log(@NonNull ContentResolver cr,
                           @NonNull Uri uri,
                           long fileSize) {

        if (fileSize <= 0) return;

        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) return;

            long offset = 0;
            byte[] header = new byte[8];

            while (offset + 8 <= fileSize) {

                if (readFully(in, header) < 8) break;

                long atomSize =
                        ((header[0] & 0xffL) << 24) |
                                ((header[1] & 0xffL) << 16) |
                                ((header[2] & 0xffL) << 8) |
                                (header[3] & 0xffL);

                if (atomSize < 8) break;

                String type = new String(header, 4, 4, StandardCharsets.US_ASCII);

                if ("moov".equals(type)) {

                    long moovStart = offset;
                    long moovSize = atomSize;
                    long moovEnd = moovStart + moovSize;
                    boolean moovAtTail = (moovEnd == fileSize);

                    long headRequiredBytes;
                    long tailRequiredBytes;

                    if (moovAtTail) {
                        // Exo logic: read small head + tail moov
                        headRequiredBytes = Math.min(SMALL_HEAD_BYTES, fileSize);
                        tailRequiredBytes = moovSize;
                    } else {
                        // moov at head or middle → read until moov ends
                        headRequiredBytes = moovEnd;
                        tailRequiredBytes = 0;
                    }

                    Log.d(TAG,
                            "uri=" + uri +
                                    " moovStart=" + moovStart +
                                    " moovSize=" + moovSize +
                                    " moovAtTail=" + moovAtTail +
                                    " headRequiredBytes=" + headRequiredBytes +
                                    " tailRequiredBytes=" + tailRequiredBytes +
                                    " fileSize=" + fileSize);

                    return;
                }

                long skip = atomSize - 8;
                long skipped = in.skip(skip);
                if (skipped != skip) break;

                offset += atomSize;
            }

        } catch (Exception e) {
            Log.d(TAG, "layout scan failed: " + e.getMessage());
        }
    }

    private static int readFully(InputStream in, byte[] buffer) throws Exception {
        int total = 0;
        while (total < buffer.length) {
            int r = in.read(buffer, total, buffer.length - total);
            if (r == -1) break;
            total += r;
        }
        return total;
    }
}
