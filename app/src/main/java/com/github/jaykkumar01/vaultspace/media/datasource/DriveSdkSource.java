package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSpec;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.services.drive.Drive;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@OptIn(markerClass = UnstableApi.class)
final class DriveSdkSource implements DriveSource {

    private static final int PROBE_BYTES = 256 * 1024;

    private final Drive drive;
    private final String fileId;
    private InputStream stream;

    DriveSdkSource(Context context, String fileId) {
        this.drive = DriveClientProvider.getPrimaryDrive(context);
        this.fileId = fileId;
    }

    @Override
    public long open(DataSpec spec) throws IOException {
//        if (spec.position == 0) probeFirst256Kb();

        Drive.Files.Get req = drive.files().get(fileId);
        MediaHttpDownloader dl = req.getMediaHttpDownloader();
        dl.setDirectDownloadEnabled(true);
        req.setDisableGZipContent(true);

        stream = req.executeMediaAsInputStream();
        return C.LENGTH_UNSET;
    }

    private void probeFirst256Kb() throws IOException {
        long start = android.os.SystemClock.elapsedRealtime();
        long firstByteAt = -1;
        int total = 0;

        Drive.Files.Get req = drive.files().get(fileId);
        MediaHttpDownloader dl = req.getMediaHttpDownloader();
        dl.setDirectDownloadEnabled(true);
        req.setDisableGZipContent(true);

        try (InputStream in = req.executeMediaAsInputStream()) {
            byte[] buf = new byte[16 * 1024];
            while (total < PROBE_BYTES) {
                int r = in.read(buf);
                if (r == -1) break;
                if (firstByteAt == -1) {
                    firstByteAt = android.os.SystemClock.elapsedRealtime();
                    android.util.Log.d("DriveProbe",
                            "[" + fileId + "] first byte +" + (firstByteAt - start) + "ms");
                }
                total += r;
            }
        }

        long end = android.os.SystemClock.elapsedRealtime();
        android.util.Log.d("DriveProbe",
                "[" + fileId + "] 256KB read +" + (end - start) + "ms");
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (stream == null) return C.RESULT_END_OF_INPUT;
        int read = stream.read(buffer, offset, length);
        return read == -1 ? C.RESULT_END_OF_INPUT : read;
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return Collections.emptyMap();
    }

    @Override
    public void close() {
        if (stream != null) {
            try { stream.close(); } catch (Exception ignored) {}
            stream = null;
        }
    }
}
