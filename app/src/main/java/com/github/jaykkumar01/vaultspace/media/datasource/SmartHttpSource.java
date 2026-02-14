package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

final class SmartHttpSource implements DriveStreamSource {

    private static final String BASE_URL = "https://www.googleapis.com/drive/v3/files/";
    private static final int CONNECT_TIMEOUT_MS = 6000, READ_TIMEOUT_MS = 15000;

    private static final ConnectionPool POOL =
            new ConnectionPool(8, 5, TimeUnit.MINUTES);
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .connectionPool(POOL)
            .protocols(java.util.Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .retryOnConnectionFailure(true)
            .build();

    private final Context appContext;
    private final String fileId;
    private String token;

    SmartHttpSource(@NonNull Context context, @NonNull String fileId) {
        this.appContext = context.getApplicationContext();
        this.fileId = fileId;
    }

    @Override
    public StreamSession open(long position) throws IOException {
        Call call = newCall(position, false);
        Response response = call.execute();
        if (response.code() == 401) {
            response.close();
            token = null;
            call = newCall(position, true);
            response = call.execute();
        }
        if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
        InputStream stream = response.body().byteStream();
        long length = resolveLength(response, position);
        Call finalCall = call;
        Response finalResponse = response;
        return new StreamSession() {
            @Override
            public InputStream stream() {
                return stream;
            }

            @Override
            public long length() {
                return length;
            }

            @Override
            public void cancel() {
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
                try {
                    finalResponse.close();
                } catch (Exception ignored) {
                }
                try {
                    finalCall.cancel();
                } catch (Exception ignored) {
                }
            }
        };
    }

    private long resolveLength(Response r, long pos) {
        if (r.code() == 206) {
            String cr = r.header("Content-Range");
            if (cr != null && cr.contains("/")) {
                long total = Long.parseLong(cr.substring(cr.indexOf("/") + 1));
                return total - pos;
            }
        }
        long len = r.body().contentLength();
        return len >= 0 ? len : -1;
    }

    private Call newCall(long position, boolean forceRefresh) {
        if (token == null || forceRefresh)
            token = DriveAuthGate.get(appContext).getToken();
        Request.Builder b = new Request.Builder()
                .url(BASE_URL + fileId + "?alt=media")
                .get()
                .header("Authorization", "Bearer " + token)
                .header("Accept-Encoding", "identity");
        if (position > 0) b.header("Range", "bytes=" + position + "-");
        return CLIENT.newCall(b.build());
    }
}
