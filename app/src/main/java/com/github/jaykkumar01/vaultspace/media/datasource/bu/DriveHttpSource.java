//package com.github.jaykkumar01.vaultspace.media.datasource.bu;
//
//import android.content.Context;
//import android.net.Uri;
//
//import androidx.annotation.OptIn;
//import androidx.media3.common.C;
//import androidx.media3.common.util.UnstableApi;
//import androidx.media3.datasource.DataSpec;
//import androidx.media3.datasource.DefaultHttpDataSource;
//import androidx.media3.datasource.HttpDataSource;
//
//import com.github.jaykkumar01.vaultspace.core.auth.DriveAuthGate1;
//
//import java.io.IOException;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//@OptIn(markerClass = UnstableApi.class)
//final class DriveHttpSource implements DriveSource {
//
//    private static final long POS_0 = 0L;
//    private static final long PLAYBACK_POS = 36L;
//    private static final long SNIFF_POS = 80674015L;
//
//    private static final int PLAYBACK_BYTES = 256 * 1024;
//    private static final int SNIFF_BYTES = 31459;
//
//    private final DriveAuthGate1 authGate;
//    private final Uri mediaUri;
//    private final DefaultHttpDataSource.Factory factory;
//
//    private HttpDataSource source;
//
//    DriveHttpSource(Context context, String fileId) {
//        this.authGate = DriveAuthGate1.get(context);
//        this.mediaUri = Uri.parse("https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media");
//        this.factory = new DefaultHttpDataSource.Factory();
//    }
//
//    @Override
//    public long open(DataSpec spec) throws IOException {
//        try {
//            String token = authGate.requireToken();
////            maybeProbe(spec, token);
//            return openInternal(spec, token);
//        } catch (IOException e) {
//            String token = authGate.refreshTokenAfterFailure();
////            maybeProbe(spec, token);
//            return openInternal(spec, token);
//        }
//    }
//
//    private void maybeProbe(DataSpec spec, String token) throws IOException {
//        if (spec.position == POS_0)
//            runProbe(spec, PLAYBACK_BYTES, token);
//        else if (spec.position == PLAYBACK_POS)
//            runProbe(spec, PLAYBACK_BYTES, token);
//        else if (spec.position == SNIFF_POS)
//            runProbe(spec, SNIFF_BYTES, token);
//    }
//
//    private void runProbe(DataSpec spec, int limit, String token) throws IOException {
//        long start = android.os.SystemClock.elapsedRealtime();
//        long firstByteAt = -1;
//        int total = 0;
//
//        DefaultHttpDataSource.Factory probeFactory =
//                new DefaultHttpDataSource.Factory()
//                        .setDefaultRequestProperties(Map.of(
//                                "Authorization", "Bearer " + token,
//                                "Range", "bytes=" + spec.position + "-"
//                        ));
//
//        HttpDataSource probe = probeFactory.createDataSource();
//        probe.open(new DataSpec(mediaUri, spec.position, limit));
//
//        try {
//            byte[] buf = new byte[16 * 1024];
//            while (total < limit) {
//                int r = probe.read(buf, 0, Math.min(buf.length, limit - total));
//                if (r == -1) break;
//
//                if (firstByteAt == -1) {
//                    firstByteAt = android.os.SystemClock.elapsedRealtime();
//                    android.util.Log.d(
//                            "HttpProbe",
//                            "[" + spec.position + "] first byte +" + (firstByteAt - start) + "ms"
//                    );
//                }
//                total += r;
//            }
//        } finally {
//            probe.close();
//        }
//
//        long end = android.os.SystemClock.elapsedRealtime();
//        android.util.Log.d(
//                "HttpProbe",
//                "[" + spec.position + "] " + total + " bytes +" + (end - start) + "ms"
//        );
//    }
//
//    private long openInternal(DataSpec spec, String token) throws IOException {
//        factory.setDefaultRequestProperties(Map.of(
//                "Authorization", "Bearer " + token,
//                "Range", "bytes=" + spec.position + "-"
//        ));
//        source = factory.createDataSource();
//        source.open(new DataSpec(mediaUri, spec.position, C.LENGTH_UNSET));
//        return C.LENGTH_UNSET;
//    }
//
//    @Override
//    public int read(byte[] buffer, int offset, int length) throws IOException {
//        if (source == null) return C.RESULT_END_OF_INPUT;
//        int read = source.read(buffer, offset, length);
//        return read == -1 ? C.RESULT_END_OF_INPUT : read;
//    }
//
//    @Override
//    public Map<String, List<String>> getResponseHeaders() {
//        return source != null ? source.getResponseHeaders() : Collections.emptyMap();
//    }
//
//    @Override
//    public void close() {
//        if (source != null) {
//            try { source.close(); } catch (Exception ignored) {}
//            source = null;
//        }
//    }
//}
