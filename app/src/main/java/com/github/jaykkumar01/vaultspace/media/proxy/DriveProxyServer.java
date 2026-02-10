package com.github.jaykkumar01.vaultspace.media.proxy;

import static fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fi.iki.elonen.NanoHTTPD;

/**
 * Hybrid Drive proxy:
 *  - range 0   → Drive SDK (stable start)
 *  - range >0  → Drive HTTP (fast seek)
 *
 * File-size aware (CRITICAL for ExoPlayer)
 */
public final class DriveProxyServer {

    private static final String TAG = "DriveProxyHybrid";

    /* ---------------- deps ---------------- */

    private final GoogleAccountCredential credential;
    private final HttpTransport transport;
    private final Drive drive;

    /* ---------------- state ---------------- */

    private final Map<String, Long> fileSizes = new ConcurrentHashMap<>();

    private Server server;
    private int port;
    private boolean started;

    public DriveProxyServer(@NonNull Context context) {
        Context app = context.getApplicationContext();
        this.credential = GoogleCredentialFactory.forPrimaryDrive(app);
        this.transport = new NetHttpTransport();
        this.drive = DriveClientProvider.getPrimaryDrive(app);
    }

    /* ---------------- lifecycle ---------------- */

    public synchronized void start() throws Exception {
        if (started) return;

        server = new Server();
        server.start(SOCKET_READ_TIMEOUT, false);
        port = server.getListeningPort();
        started = true;

        Log.i(TAG, "Started @ " + port);
    }

    public synchronized void stop() {
        if (!started) return;
        server.stop();
        server = null;
        started = false;
        fileSizes.clear();
    }

    public void registerFile(@NonNull String fileId, long sizeBytes) {
        fileSizes.put(fileId, sizeBytes);
    }

    @NonNull
    public String getProxyUrl(@NonNull String fileId) {
        if (!started) throw new IllegalStateException("Proxy not started");
        return "http://127.0.0.1:" + port + "/drive/" + fileId;
    }

    /* ---------------- HTTP server ---------------- */

    private final class Server extends NanoHTTPD {

        Server() { super("127.0.0.1", 0); }

        @Override
        public Response serve(IHTTPSession session) {
            try {
                if (session.getMethod() != Method.GET)
                    return error(Response.Status.METHOD_NOT_ALLOWED);

                String uri = session.getUri();
                if (!uri.startsWith("/drive/"))
                    return error(Response.Status.NOT_FOUND);

                String fileId = uri.substring("/drive/".length());
                long start = parseRange(session.getHeaders());
                Long totalObj = fileSizes.get(fileId);
                if (totalObj == null || totalObj <= 0) {
                    throw new IllegalStateException("Missing size for " + fileId);
                }
                long total = totalObj;


                InputStream stream =
                        (start == 0)
                                ? openSdkStream(fileId)
                                : openHttpStream(fileId, start);

                long end = total - 1;
                long len = total - start;

                Log.d(TAG, "[" + fileId + "] @" + start
                        + (start == 0 ? " SDK" : " HTTP"));

                Response res = newFixedLengthResponse(
                        Response.Status.PARTIAL_CONTENT,
                        "application/octet-stream",
                        stream,
                        len
                );

                res.addHeader("Accept-Ranges", "bytes");
                res.addHeader(
                        "Content-Range",
                        "bytes " + start + "-" + end + "/" + total
                );
                res.addHeader("Content-Length", String.valueOf(len));
                return res;

            } catch (Exception e) {
                Log.e(TAG, "Serve error", e);
                return error(Response.Status.INTERNAL_ERROR);
            }
        }

        private Response error(Response.Status s) {
            return newFixedLengthResponse(s, MIME_PLAINTEXT, s.getDescription());
        }
    }

    /* ---------------- backends ---------------- */

    private InputStream openSdkStream(String fileId) throws Exception {
        return drive.files()
                .get(fileId)
                .executeMediaAsInputStream();
    }

    private InputStream openHttpStream(String fileId, long start) throws Exception {
        GenericUrl url = new GenericUrl(
                "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media"
        );
        HttpRequestFactory f = transport.createRequestFactory(credential);
        HttpRequest r = f.buildGetRequest(url);
        r.getHeaders().setRange("bytes=" + start + "-");
        r.setConnectTimeout(0);
        r.setReadTimeout(0);
        HttpResponse resp = r.execute();
        return resp.getContent();
    }

    /* ---------------- utils ---------------- */

    private static long parseRange(Map<String, String> headers) {
        String r = headers.get("range");
        if (r == null) return 0;
        try {
            int eq = r.indexOf('=');
            int dash = r.indexOf('-');
            if (eq >= 0 && dash > eq)
                return Long.parseLong(r.substring(eq + 1, dash));
        } catch (Exception ignored) {}
        return 0;
    }
}
