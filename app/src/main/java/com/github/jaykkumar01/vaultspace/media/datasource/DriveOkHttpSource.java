package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

final class DriveOkHttpSource implements DriveStreamSource {

    private static final String BASE_URL = "https://www.googleapis.com/drive/v3/files/";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 20000;

    /* ========================= SHARED CLIENT ========================= */

    private static final OkHttpClient CLIENT =
            new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

    /* ========================= CORE ========================= */

    private final String fileId;
    private final Context appContext;

    private String token;

    DriveOkHttpSource(Context context, String fileId) {
        this.appContext = context.getApplicationContext();
        this.fileId = fileId;
    }

    /* ========================= OPEN ========================= */

    @Override
    public StreamSession open(long position) throws IOException {

        Call call = buildCall(position, false);
        Response response = call.execute();

        // Retry once if unauthorized
        if (response.code() == 401) {
            response.close();
            token = null;
            call = buildCall(position, true);
            response = call.execute();
        }

        if (!response.isSuccessful())
            throw new IOException("HTTP " + response.code());

        long length = response.body().contentLength();

        InputStream stream = response.body().byteStream();

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
                try { stream.close(); } catch (Exception ignored) {}
                finalCall.cancel();
                finalResponse.close();
            }
        };
    }

    /* ========================= REQUEST ========================= */

    @NonNull
    private Call buildCall(long position, boolean forceRefreshToken) throws IOException {

        if (token == null || forceRefreshToken) {
            token = DriveAuthGate.get(appContext).getToken();
        }

        String url = BASE_URL + fileId + "?alt=media";

        Request.Builder builder = new Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer " + token)
                .header("Accept-Encoding", "identity");

        if (position > 0) {
            builder.header("Range", "bytes=" + position + "-");
        }

        Request request = builder.build();
        return CLIENT.newCall(request);
    }
}
