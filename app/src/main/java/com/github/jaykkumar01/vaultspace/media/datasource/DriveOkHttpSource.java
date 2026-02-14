package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

final class DriveOkHttpSource implements DriveStreamSource {

    private static final String BASE_URL = "https://www.googleapis.com/drive/v3/files/";
    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final int READ_TIMEOUT_MS = 15000;

    /* ---------------- SHARED HTTP/2 CLIENT ---------------- */

    private static final ConnectionPool CONNECTION_POOL =
            new ConnectionPool(8, 5, TimeUnit.MINUTES);

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .connectionPool(CONNECTION_POOL)
            .protocols(java.util.Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .retryOnConnectionFailure(true)
            .build();


    /* ---------------- CORE ---------------- */

    private final Context appContext;
    private final String fileId;
    private String token;

    DriveOkHttpSource(@NonNull Context context, @NonNull String fileId) {
        this.appContext = context.getApplicationContext();
        this.fileId = fileId;
    }

    /* ========================= OPEN ========================= */

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

        if (!response.isSuccessful())
            throw new IOException("HTTP " + response.code());

        final InputStream stream = response.body().byteStream();
        final long length = response.body().contentLength();
        final Call finalCall = call;
        final Response finalResponse = response;

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
                finalCall.cancel();
                finalResponse.close();
            }
        };
    }

    /* ========================= REQUEST ========================= */

    private Call newCall(long position, boolean forceRefresh) {

        if (token == null || forceRefresh)
            token = DriveAuthGate.get(appContext).getToken();

        String url = BASE_URL + fileId + "?alt=media";

        Request.Builder builder = new Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer " + token)
                .header("Accept-Encoding", "identity");

        if (position > 0)
            builder.header("Range", "bytes=" + position + "-");

        return CLIENT.newCall(builder.build());
    }
}