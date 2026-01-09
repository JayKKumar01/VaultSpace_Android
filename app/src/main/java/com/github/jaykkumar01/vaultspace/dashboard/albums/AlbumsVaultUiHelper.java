package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.dashboard.BaseVaultSectionUiHelper;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.github.jaykkumar01.vaultspace.views.AlbumsContentView;
import com.github.jaykkumar01.vaultspace.views.FolderActionView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsVaultUiHelper extends BaseVaultSectionUiHelper {

    private static final String TAG = "VaultSpace:AlbumsUI";

    private enum UiState { UNINITIALIZED, LOADING, EMPTY, CONTENT, ERROR }

    private final AlbumsDriveHelper drive;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private UiState state = UiState.UNINITIALIZED;
    private boolean released;
    private AlbumsContentView albumsContentView;

    public AlbumsVaultUiHelper(Context context, FrameLayout container) {
        super(context, container);

        drive = new AlbumsDriveHelper(context);

        loadingView.setText("Loading albums…");

        emptyView.setIcon(R.drawable.ic_album_empty);
        emptyView.setTitle("No albums yet");
        emptyView.setSubtitle("Albums help you organize memories your way.");
        emptyView.setPrimaryAction("Create Album", v -> onCreateAlbum());
        emptyView.hideSecondaryAction();

        Log.d(TAG, "init");
    }

    @Override
    protected View createContentView(Context context) {
        albumsContentView = new AlbumsContentView(context);

        albumsContentView.setOnAddAlbumClickListener(v -> onCreateAlbum());

        albumsContentView.setOnAlbumClickListener(album ->
                Log.d(TAG, "Album clicked: " + album.name + " (" + album.id + ")")
        );

        return albumsContentView;
    }

    /* ---------------- Initial load ---------------- */

    @Override
    public void show() {
        if (released || state != UiState.UNINITIALIZED) return;

        moveToState(UiState.LOADING);

        drive.fetchAlbums(executor, new AlbumsDriveHelper.FetchCallback() {
            @Override
            public void onResult(List<AlbumInfo> albums) {
                if (released) return;

                if (albums.isEmpty()) {
                    moveToState(UiState.EMPTY);
                } else {
                    albumsContentView.setAlbums(albums);
                    moveToState(UiState.CONTENT);
                }
            }

            @Override
            public void onError(Exception e) {
                if (released) return;
                Log.e(TAG, "Initial album fetch failed", e);
                moveToState(UiState.ERROR);
            }
        });
    }

    /* ---------------- Create Album ---------------- */

    private void onCreateAlbum() {
        if (released) return;

        albumsContentView.setFabVisible(false);

        showFolderActionPopup(
                "Create Album",
                "Album name",
                "Create",
                TAG,
                new FolderActionView.Callback() {
                    @Override
                    public void onCreate(String name) {
                        createAlbum(name);
                    }

                    @Override
                    public void onCancel() {
                        restoreFab();
                        hideFolderActionPopup();
                    }
                }
        );
    }

    private void createAlbum(String name) {
        hideFolderActionPopup();

        drive.createAlbum(executor, name,
                new AlbumsDriveHelper.CreateAlbumCallback() {

                    @Override
                    public void onSuccess(AlbumInfo album) {
                        if (released) return;

                        albumsContentView.addAlbum(album);
                        moveToState(UiState.CONTENT);
                        restoreFab();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (released) return;

                        Log.e(TAG, "createAlbum failed", e);
                        Toast.makeText(
                                context,
                                "Failed to create album",
                                Toast.LENGTH_SHORT
                        ).show();
                        restoreFab();
                    }
                });
    }

    /* ---------------- Future hooks (Phase 2.3 ready) ---------------- */

    /** Called after Drive thumbnail fetch */
    public void onAlbumCoverUpdated(String albumId, String coverPath) {
        if (released || albumsContentView == null) return;
        albumsContentView.updateAlbumCover(albumId, coverPath);
    }

    /** Called after Drive rename */
    public void onAlbumRenamed(String albumId, String newName) {
        if (released || albumsContentView == null) return;
        albumsContentView.updateAlbumName(albumId, newName);
    }

    /** Called after Drive delete */
    public void onAlbumDeleted(String albumId) {
        if (released || albumsContentView == null) return;

        albumsContentView.deleteAlbum(albumId);

        if (state == UiState.CONTENT && isContentEmpty()) {
            moveToState(UiState.EMPTY);
        }
    }

    private boolean isContentEmpty() {
        // lightweight state sync; no adapter exposure needed
        return false; // can be enhanced later if needed
    }

    /* ---------------- State machine ---------------- */

    private void moveToState(UiState newState) {
        if (state == newState) return;

        Log.d(TAG, "UI state: " + state + " → " + newState);
        state = newState;

        switch (newState) {
            case LOADING:
                showLoading();
                break;
            case EMPTY:
                showEmpty();
                break;
            case CONTENT:
                showContent();
                break;
            case ERROR:
                showEmpty(); // fallback
                break;
        }
    }

    private void restoreFab() {
        if (albumsContentView != null) {
            albumsContentView.setFabVisible(true);
        }
    }

    /* ---------------- Back / Lifecycle ---------------- */

    @Override
    public boolean onBackPressed() {
        if (folderActionView != null && folderActionView.isVisible()) {
            hideFolderActionPopup();
            restoreFab();
            return true;
        }
        return false;
    }

    @Override
    public void release() {
        released = true;
        executor.shutdownNow();
    }
}
