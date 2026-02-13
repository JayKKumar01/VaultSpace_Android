package com.github.jaykkumar01.vaultspace.utils;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;

public final class Mp4LayoutAnalyzer {

    private static final long SMALL_HEAD_BYTES = 64 * 1024;
    private static final int HEADER_SIZE = 8;

    private Mp4LayoutAnalyzer() {}

    @Nullable
    public static Result analyze(@NonNull ContentResolver cr,
                                 @NonNull Uri uri,
                                 long fileSize) {

        if (fileSize <= 0) return null;

        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) return null;

            long offset = 0;
            byte[] header = new byte[HEADER_SIZE];
            byte[] ext = new byte[8];

            while (offset + HEADER_SIZE <= fileSize) {

                if (!readFully(in, header, HEADER_SIZE)) return null;

                long atomSize = readUint32(header);
                long headerLength = HEADER_SIZE;

                if (atomSize == 1) {
                    if (!readFully(in, ext, 8)) return null;
                    atomSize = readUint64(ext);
                    headerLength = 16;
                } else if (atomSize == 0) {
                    atomSize = fileSize - offset;
                }

                if (atomSize < headerLength || offset + atomSize > fileSize)
                    return null;

                // Compare bytes directly instead of creating String
                if (isMoov(header)) {

                    long moovStart = offset;
                    long moovSize = atomSize;
                    long moovEnd = moovStart + moovSize;
                    boolean moovAtTail = (moovEnd == fileSize);

                    long headRequired = moovAtTail
                            ? Math.min(SMALL_HEAD_BYTES, fileSize)
                            : moovEnd;

                    long tailRequired = moovAtTail ? moovSize : 0;

                    return new Result(
                            moovStart,
                            moovSize,
                            headRequired,
                            tailRequired,
                            moovAtTail
                    );
                }

                if (!skipFully(in, atomSize - headerLength))
                    return null;

                offset += atomSize;
            }

        } catch (Exception ignored) {}

        return null;
    }

    private static boolean readFully(InputStream in, byte[] buffer, int length) throws Exception {
        int total = 0;
        while (total < length) {
            int r = in.read(buffer, total, length - total);
            if (r == -1) return false;
            total += r;
        }
        return true;
    }

    private static boolean skipFully(InputStream in, long bytes) throws Exception {
        long remaining = bytes;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) return false;
            remaining -= skipped;
        }
        return true;
    }

    private static boolean isMoov(byte[] header) {
        return header[4] == 'm' &&
                header[5] == 'o' &&
                header[6] == 'o' &&
                header[7] == 'v';
    }

    private static long readUint32(byte[] b) {
        return ((b[0] & 0xffL) << 24) |
                ((b[1] & 0xffL) << 16) |
                ((b[2] & 0xffL) << 8) |
                (b[3] & 0xffL);
    }

    private static long readUint64(byte[] b) {
        return ((b[0] & 0xffL) << 56) |
                ((b[1] & 0xffL) << 48) |
                ((b[2] & 0xffL) << 40) |
                ((b[3] & 0xffL) << 32) |
                ((b[4] & 0xffL) << 24) |
                ((b[5] & 0xffL) << 16) |
                ((b[6] & 0xffL) << 8) |
                (b[7] & 0xffL);
    }

    public static final class Result {
        public final long moovStart;
        public final long moovSize;
        public final long headRequiredBytes;
        public final long tailRequiredBytes;
        public final boolean moovAtTail;

        Result(long moovStart,
               long moovSize,
               long headRequiredBytes,
               long tailRequiredBytes,
               boolean moovAtTail) {
            this.moovStart = moovStart;
            this.moovSize = moovSize;
            this.headRequiredBytes = headRequiredBytes;
            this.tailRequiredBytes = tailRequiredBytes;
            this.moovAtTail = moovAtTail;
        }
    }
}
