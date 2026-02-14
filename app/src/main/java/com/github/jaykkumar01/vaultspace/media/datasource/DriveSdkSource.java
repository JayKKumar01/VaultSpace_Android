package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.media.base.DriveStreamSource;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.drive.Drive;

import java.io.IOException;
import java.io.InputStream;

final class DriveSdkSource implements DriveStreamSource {

    /* ---------------- CORE ---------------- */

    private final Context appContext;
    private final String fileId;
    private final Drive drive;

    /* ---------------- CONSTRUCTOR ---------------- */

    DriveSdkSource(@NonNull Context context, @NonNull String fileId) {
        this.appContext = context.getApplicationContext();
        this.fileId = fileId;
        this.drive = DriveClientProvider.getPrimaryDrive(appContext);
    }

    /* ========================= OPEN ========================= */

    @SuppressWarnings("resource")
    @Override
    public StreamSession open(long position) throws IOException {

        Drive.Files.Get req = drive.files().get(fileId);
        req.getMediaHttpDownloader().setDirectDownloadEnabled(true);
        req.setDisableGZipContent(true);

        if (position > 0)
            req.getRequestHeaders().setRange("bytes=" + position + "-");

        InputStream stream = req.executeMediaAsInputStream();

        HttpHeaders headers = req.getLastResponseHeaders();
        long length = resolveLength(headers, position);

        return new StreamSession() {

            @Override public InputStream stream() { return stream; }

            @Override public long length() { return length; }

            @Override
            public void cancel() {
                try { stream.close(); } catch (Exception ignored) {}
            }
        };
    }

    /* ========================= LENGTH RESOLUTION ========================= */

    private long resolveLength(HttpHeaders headers, long position) {

        if (headers == null)
            return -1;

        String contentRange = headers.getContentRange();
        if (contentRange != null && contentRange.contains("/")) {
            try {
                long total = Long.parseLong(contentRange.substring(contentRange.indexOf("/") + 1));
                return total - position;
            } catch (Exception ignored) {}
        }

        Long contentLength = headers.getContentLength();
        if (contentLength != null)
            return contentLength;

        return -1;
    }
}
