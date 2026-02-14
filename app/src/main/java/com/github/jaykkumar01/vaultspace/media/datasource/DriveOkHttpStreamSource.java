package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

final class DriveOkHttpStreamSource implements DriveStreamSource {

    private static final String BASE_URL = "https://www.googleapis.com/drive/v3/files/";
    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final int READ_TIMEOUT_MS = 15000;

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .retryOnConnectionFailure(true)
            .build();

    private final Context context;
    private final String fileId;

    private String token;

    DriveOkHttpStreamSource(@NonNull Context ctx, @NonNull String fileId) {
        this.context = ctx.getApplicationContext();
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

        if (!response.isSuccessful())
            throw new IOException("HTTP " + response.code());

        final InputStream stream = response.body().byteStream();
        final long availableLength = response.body().contentLength();
        final Call finalCall = call;
        final Response finalResponse = response;

        return new StreamSession() {
            @Override public InputStream stream() { return stream; }
            @Override public long length() { return availableLength; }
            @Override public void cancel() {
                try { stream.close(); } catch (Exception ignored) {}
                finalCall.cancel();
                finalResponse.close();
            }
        };
    }

    private Call newCall(long position, boolean refresh) {

        if (token == null || refresh)
            token = DriveAuthGate.get(context).getToken();

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + fileId + "?alt=media")
                .header("Authorization", "Bearer " + token)
                .header("Accept-Encoding", "identity");

        if (position > 0)
            builder.header("Range", "bytes=" + position + "-");

        return CLIENT.newCall(builder.build());
    }
}
