package com.github.jaykkumar01.vaultspace.core.upload.helper;

import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.utils.UriUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class UploadSelectionHelper {

    /* ================= Callback ================= */

    public interface Callback {
        void onResolved(List<UploadSelection> selections);
    }

    /* ================= State ================= */

    private final AppCompatActivity context;
    private final String groupId;

    private volatile boolean released;

    /* ================= Init ================= */

    public UploadSelectionHelper(AppCompatActivity activity, String groupId) {
        this.context = activity;
        this.groupId = groupId;
    }

    /* ================= Public API ================= */

    public void resolve(List<Uri> uris, Callback callback) {
        if (released || uris == null || uris.isEmpty()) return;

        int cpu = Runtime.getRuntime().availableProcessors();
        int threads = Math.min(uris.size(), Math.max(1, cpu - 1));

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {

            List<Future<UploadSelection>> futures = new ArrayList<>(uris.size());

            for (Uri uri : uris) {
                if (released) break;
                futures.add(executor.submit(() -> released ? null : resolveSingle(uri)));
            }

            List<UploadSelection> result = new ArrayList<>(uris.size());

            for (Future<UploadSelection> f : futures) {
                if (released) break;
                try {
                    UploadSelection s = f.get();
                    if (s != null) result.add(s);
                } catch (Exception ignored) {
                }
            }

            if (released) return;

            /* ===== SORT ONCE (DESC by momentMillis) ===== */
            result.sort(Comparator.comparingLong((UploadSelection s) -> s.momentMillis).reversed());

            callback.onResolved(result);
        }
    }

    public void release() {
        released = true;
    }

    /* ================= Internal ================= */

    private UploadSelection resolveSingle(Uri uri) {
        if (released) return null;
        try {
            if (UriUtils.isPermissionRevoked(context, uri)) return null;
            return UriUtils.resolve(context, groupId, uri);
        } catch (Exception ignored) {
            return null;
        }
    }
}
