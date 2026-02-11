package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.drive.Drive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@OptIn(markerClass = UnstableApi.class)
final class DriveSdkSource implements DriveSource {

    private final Drive drive;
    private final String fileId;

    DriveSdkSource(Context context,String fileId) {
        this.drive = DriveClientProvider.getPrimaryDrive(context);
        this.fileId = fileId;
    }

    @Override
    public byte[] fetchRange(long position,int length) throws IOException {

        Drive.Files.Get req = drive.files().get(fileId);
        req.getRequestHeaders().setRange("bytes=" + position + "-");
        req.setDisableGZipContent(true);

        try (InputStream in = req.executeMediaAsInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream(length)) {

            byte[] buffer = new byte[16 * 1024];
            int remaining = length;

            while (remaining > 0) {
                int r = in.read(buffer,0,Math.min(buffer.length,remaining));
                if (r == -1) break;
                out.write(buffer,0,r);
                remaining -= r;
            }

            return out.toByteArray();
        }
    }
}
