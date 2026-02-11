package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;
import android.net.Uri;

import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;
import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate1;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

final class DriveHttpSource implements DriveSource {

    private final DriveAuthGate authGate;
    private final Uri uri;
    private java.net.HttpURLConnection connection;

    DriveHttpSource(Context context,String fileId) {
        this.authGate = DriveAuthGate.get(context);
        this.uri = Uri.parse("https://www.googleapis.com/drive/v3/files/"+fileId+"?alt=media");
    }

    @Override
    public InputStream openStream(long position) throws IOException {
        String token = authGate.getToken();

        java.net.URL url = new java.net.URL(uri.toString());
        connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization","Bearer "+token);
        connection.setRequestProperty("Range","bytes="+position+"-");
        connection.connect();

        return connection.getInputStream();
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }
}
