package com.github.jaykkumar01.vaultspace.media.datasource;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

final class SmartHttpStreamLayer {

    private static final String BASE_URL = "https://www.googleapis.com/drive/v3/files/";
    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final int READ_TIMEOUT_MS = 15000;

    /* ---------------- SHARED CLIENT ---------------- */

    private static final ConnectionPool POOL =
            new ConnectionPool(8, 5, TimeUnit.MINUTES);

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .connectionPool(POOL)
            .protocols(java.util.Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .retryOnConnectionFailure(true)
            .build();

    /* ---------------- CORE ---------------- */

    private final Context appContext;
    private final String fileId;

    private String token;

    /* ---------------- ACTIVE STATE ---------------- */

    private Call activeCall;
    private Response activeResponse;
    private InputStream activeStream;

    private long activeStart;
    private long activePosition;
    private boolean logicallyClosed;

    SmartHttpStreamLayer(@NonNull Context context, @NonNull String fileId) {
        this.appContext = context.getApplicationContext();
        this.fileId = fileId;
    }

    /* ========================= OPEN ========================= */

    synchronized StreamSession open(long position) throws IOException {

        if (canReuse(position)) {
            logicallyClosed = false;
            return wrapSession();
        }

        forceClose();

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

        activeCall = call;
        activeResponse = response;
        activeStream = response.body().byteStream();

        activeStart = position;
        activePosition = position;
        logicallyClosed = false;

        long length = resolveLength(response, position);

        return new StreamSession(activeStream, length);
    }

    /* ========================= REUSE ========================= */

    private boolean canReuse(long position) {
        return activeStream != null
                && logicallyClosed
                && position == activePosition;
    }

    /* ========================= LENGTH ========================= */

    private long resolveLength(Response response, long position) {

        if (response.code() == 206) {
            String contentRange = response.header("Content-Range");
            if (contentRange != null && contentRange.contains("/")) {
                long total = Long.parseLong(
                        contentRange.substring(contentRange.indexOf("/") + 1)
                );
                return total - position;
            }
        }

        long contentLength = response.body().contentLength();
        return contentLength >= 0 ? contentLength : -1;
    }

    /* ========================= REQUEST ========================= */

    private Call newCall(long position, boolean forceRefresh) {

        if (token == null || forceRefresh)
            token = DriveAuthGate.get(appContext).getToken();

        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + fileId + "?alt=media")
                .get()
                .header("Authorization", "Bearer " + token)
                .header("Accept-Encoding", "identity");

        if (position > 0)
            builder.header("Range", "bytes=" + position + "-");

        return CLIENT.newCall(builder.build());
    }

    /* ========================= FORCE CLOSE ========================= */

    synchronized void forceClose() {
        try { if (activeStream != null) activeStream.close(); } catch (Exception ignored) {}
        try { if (activeResponse != null) activeResponse.close(); } catch (Exception ignored) {}
        try { if (activeCall != null) activeCall.cancel(); } catch (Exception ignored) {}

        activeStream = null;
        activeResponse = null;
        activeCall = null;
        logicallyClosed = false;
    }

    /* ========================= SESSION ========================= */

    final class StreamSession {

        private final InputStream stream;
        private final long length;

        StreamSession(InputStream stream, long length) {
            this.stream = new ForwardTrackingStream(stream);
            this.length = length;
        }

        InputStream stream() { return stream; }
        long length() { return length; }

        void logicalClose() {
            logicallyClosed = true;
        }
    }

    /* ========================= TRACKING STREAM ========================= */

    private final class ForwardTrackingStream extends InputStream {

        private final InputStream delegate;

        ForwardTrackingStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int r = delegate.read();
            if (r != -1) activePosition++;
            return r;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int r = delegate.read(b, off, len);
            if (r > 0) activePosition += r;
            return r;
        }

        @Override
        public void close() {
            // logical close only
        }
    }
}
