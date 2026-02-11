package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;

import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;
import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@OptIn(markerClass = UnstableApi.class)
final class DriveHttpSource implements DriveSource {

    private final DriveAuthGate authGate;
    private final Uri mediaUri;

    DriveHttpSource(Context context,String fileId) {
        this.authGate = DriveAuthGate.get(context);
        this.mediaUri = Uri.parse("https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media");
    }

    @Override
    public byte[] fetchRange(long position,int length) throws IOException {

        String token = authGate.getToken();

        DefaultHttpDataSource.Factory factory =
                new DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(Map.of(
                                "Authorization","Bearer " + token,
                                "Range","bytes=" + position + "-"
                        ));

        HttpDataSource source = factory.createDataSource();
        ByteArrayOutputStream out = new ByteArrayOutputStream(length);

        source.open(new DataSpec(mediaUri,position,length));

        try {
            byte[] buffer = new byte[16 * 1024];
            int remaining = length;

            while (remaining > 0) {
                int r = source.read(buffer,0,Math.min(buffer.length,remaining));
                if (r == -1) break;
                out.write(buffer,0,r);
                remaining -= r;
            }
        } finally {
            source.close();
        }

        return out.toByteArray();
    }
}
