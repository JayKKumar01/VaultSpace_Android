package com.github.jaykkumar01.vaultspace.media.remux;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.UUID;

public final class VideoRemuxHelper {

    private static final String TAG = "VideoRemux";

    private VideoRemuxHelper() {}

    public static final class Result {
        public final File file;
        public final long sizeBytes;
        public final boolean isHevc;

        Result(File f, long s, boolean h) {
            file = f;
            sizeBytes = s;
            isHevc = h;
        }
    }

    /**
     * Remuxes a video Uri into a fast-start MP4.
     * Throws on any failure. Caller owns threading.
     */
    @NonNull
    public static Result remux(
            @NonNull Context context,
            @NonNull Uri input
    ) throws Exception {

        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(context, input, null);

        File outFile = new File(
                context.getCacheDir(),
                "vs_remux_" + UUID.randomUUID() + ".mp4"
        );

        MediaMuxer muxer =
                new MediaMuxer(outFile.getAbsolutePath(),
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        int trackCount = extractor.getTrackCount();
        int[] trackMap = new int[trackCount];
        boolean hasVideo = false;
        boolean isHevc = false;

        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);

            if (mime == null) {
                trackMap[i] = -1;
                continue;
            }

            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                int dst = muxer.addTrack(format);
                trackMap[i] = dst;

                if (mime.startsWith("video/")) {
                    hasVideo = true;
                    if (mime.equalsIgnoreCase("video/hevc")
                            || mime.equalsIgnoreCase("video/h265")) {
                        isHevc = true;
                    }
                }
            } else {
                trackMap[i] = -1;
            }
        }

        if (!hasVideo) {
            extractor.release();
            muxer.release();
            throw new IllegalStateException("No video track found");
        }

        muxer.start();

        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        for (int i = 0; i < trackCount; i++) {
            if (trackMap[i] < 0) continue;

            extractor.selectTrack(i);

            while (true) {
                int size = extractor.readSampleData(buffer, 0);
                if (size < 0) break;

                info.offset = 0;
                info.size = size;
                info.presentationTimeUs = extractor.getSampleTime();
                info.flags = extractor.getSampleFlags();

                muxer.writeSampleData(trackMap[i], buffer, info);
                extractor.advance();
            }

            extractor.unselectTrack(i);
        }

        muxer.stop();
        muxer.release();
        extractor.release();

        long outSize = outFile.length();

        Log.d(TAG,
                "remux done | size=" + outSize +
                        " hevc=" + isHevc);

        return new Result(outFile, outSize, isHevc);
    }
}
