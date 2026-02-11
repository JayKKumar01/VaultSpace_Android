package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.services.drive.Drive;

import java.io.IOException;
import java.io.InputStream;

final class DriveSdkSource implements DriveSource {

    private static final String TAG = "Drive:SdkSource";

    private final Drive drive;
    private final String fileId;
    private InputStream stream;

    DriveSdkSource(Context context,String fileId) {
        this.drive = DriveClientProvider.getPrimaryDrive(context);
        this.fileId = fileId;
    }

    @Override
    public InputStream openStream(long position) throws IOException {

        long start = SystemClock.elapsedRealtime();

        Drive.Files.Get req = drive.files().get(fileId);
        req.getMediaHttpDownloader().setDirectDownloadEnabled(true);
        req.setDisableGZipContent(true);

        if (position > 0)
            req.getRequestHeaders().setRange("bytes=" + position + "-");

        stream = req.executeMediaAsInputStream();

        int code = req.getLastStatusCode();
        String message = req.getLastStatusMessage();

        Log.d(TAG,
                "open @" + position +
                        " code=" + code +
                        " msg=" + message +
                        " +" + (SystemClock.elapsedRealtime() - start) + "ms"
        );

        return stream;
    }

    @Override
    public void close() {
        if (stream != null) {
            Log.d(TAG,"close");
            try { stream.close(); } catch (Exception ignored) {}
            stream = null;
        }
    }
}
