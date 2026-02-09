package com.github.jaykkumar01.vaultspace.media.remux;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;
import java.nio.ByteBuffer;

public final class VideoRotationNormalizeHelper {

    private VideoRotationNormalizeHelper() {}

    public record RemuxResult(@NonNull Uri uri, long sizeBytes) {}

    /**
     * TEST ONLY.
     * Removes container rotation metadata by re-muxing video.
     * Pixels are NOT rotated.
     */
    public static @NonNull RemuxResult removeRotationMetadata(
            @NonNull Context context,
            @NonNull Uri inputUri
    ) throws Exception {

        File outFile = new File(
                context.getCacheDir(),
                "rot_meta_zero_" + System.currentTimeMillis() + ".mp4"
        );

        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(context, inputUri, null);

        int videoTrack = selectVideoTrack(extractor);
        extractor.selectTrack(videoTrack);

        MediaFormat format = extractor.getTrackFormat(videoTrack);

        MediaMuxer muxer = new MediaMuxer(
                outFile.getAbsolutePath(),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        );

        // IMPORTANT: do NOT call setOrientationHint()
        int muxerVideoTrack = muxer.addTrack(format);
        muxer.start();

        ByteBuffer buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        while (true) {
            int size = extractor.readSampleData(buffer, 0);
            if (size < 0) {
                info.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                break;
            }

            info.offset = 0;
            info.size = size;
            info.presentationTimeUs = extractor.getSampleTime();
            info.flags = extractor.getSampleFlags();

            muxer.writeSampleData(muxerVideoTrack, buffer, info);
            extractor.advance();
        }

        muxer.stop();
        muxer.release();
        extractor.release();

        long outSize = outFile.length();
        if (outSize <= 0)
            throw new IllegalStateException("Remux failed: empty output");

        return new RemuxResult(Uri.fromFile(outFile), outSize);
    }

    private static int selectVideoTrack(MediaExtractor extractor) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat f = extractor.getTrackFormat(i);
            String mime = f.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("video/"))
                return i;
        }
        throw new IllegalStateException("No video track found");
    }
}
