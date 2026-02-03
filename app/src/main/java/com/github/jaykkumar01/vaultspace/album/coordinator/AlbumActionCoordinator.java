package com.github.jaykkumar01.vaultspace.album.coordinator;

import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.selection.UriSelection;
import com.github.jaykkumar01.vaultspace.core.upload.helper.UploadSelectionHelper;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AlbumActionCoordinator {

    private static final String TAG = "VaultSpace:AlbumAction";
    private boolean released;

    /* ============================================================
     * Host contract
     * ============================================================ */

    public interface Listener {
        void onMediaSelected(int size);
        void onMediaResolved(List<UploadSelection> selections);
    }

    /* ============================================================
     * Fields
     * ============================================================ */

    private final Listener listener;
    private final UriSelection uriSelection;
    private final UploadSelectionHelper uploadSelectionHelper;
    private final ExecutorService executor;

    /* ============================================================
     * Constructor
     * ============================================================ */

    public AlbumActionCoordinator(AppCompatActivity activity,String albumId,Listener listener) {
        this.listener = listener;

        // 🔑 Coordinator OWNS executor
        this.executor = Executors.newSingleThreadExecutor();
        this.uploadSelectionHelper = new UploadSelectionHelper(activity, albumId);

        this.uriSelection = new UriSelection(activity, uris -> {
            if (released || uris.isEmpty()) return;
            listener.onMediaSelected(uris.size());
            handleMediaUris(uris);
        });
    }

    /* ============================================================
     * Uri intake
     * ============================================================ */

    private void handleMediaUris(List<Uri> uris) {
        Log.d(TAG, "Media URIs selected count=" + uris.size());

        executor.execute(() -> {
            if (released) return;

            uploadSelectionHelper.resolve(uris, selections -> {
                if (released) return;
                listener.onMediaResolved(selections);
            });
        });
    }

    /* ============================================================
     * User intents
     * ============================================================ */

    public void onAddMediaClicked() {
        if (released) return;
        uriSelection.selectMediaUris();
    }

    public void onDownloadMedia(AlbumMedia m) {
    }

    /* ============================================================
     * Lifecycle
     * ============================================================ */

    public void release() {
        released = true;
        uploadSelectionHelper.release();
        executor.shutdownNow();
    }
}
