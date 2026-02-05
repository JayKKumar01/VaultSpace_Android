package com.github.jaykkumar01.vaultspace.album.coordinator;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.activities.MediaActivity;
import com.github.jaykkumar01.vaultspace.album.helper.AlbumMediaActionHandler;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.drive.AlbumMediaRepository;
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

    private final AppCompatActivity activity;
    private final String albumId;
    private final Listener listener;
    private final UriSelection uriSelection;
    private final UploadSelectionHelper uploadSelectionHelper;
    private final ExecutorService executor;
    private final AlbumMediaActionHandler mediaActionHandler;


    /* ============================================================
     * Constructor
     * ============================================================ */

    public AlbumActionCoordinator(AppCompatActivity activity,String albumId,Listener listener) {
        this.activity = activity;
        this.albumId = albumId;
        this.listener = listener;
        // 🔑 Coordinator OWNS executor
        this.executor = Executors.newSingleThreadExecutor();
        this.uploadSelectionHelper = new UploadSelectionHelper(activity, albumId);
        this.mediaActionHandler = new AlbumMediaActionHandler(activity);


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

    public void showMediaPreview(AlbumMedia m) {
        if (released || m == null) return;

        Intent i = new Intent(activity, MediaActivity.class);
        i.putExtra("albumId", albumId);
        i.putExtra("fileId", m.fileId);
        activity.startActivity(i);
        activity.overridePendingTransition(R.anim.album_enter, R.anim.album_exit);
    }


    public void onDownloadMedia(AlbumMedia m) {
        if (released || m == null) return;
        mediaActionHandler.downloadMedia(m);
    }

    public void onDeleteMedia(String id, Runnable onSuccess, Runnable onFailure) {
        if (released) return;
        mediaActionHandler.deleteMedia(id, onSuccess, onFailure);
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
