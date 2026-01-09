package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.activities.AlbumActivity;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.BaseVaultSectionUiHelper;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.github.jaykkumar01.vaultspace.views.popups.ConfirmActionView;
import com.github.jaykkumar01.vaultspace.views.popups.FolderActionView;
import com.github.jaykkumar01.vaultspace.views.popups.ItemActionView;

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

        // ✅ Correct: visibility only, no state mutation
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);

        Log.d(TAG, "init");
    }

    @Override
    protected View createContentView(Context context) {
        albumsContentView = new AlbumsContentView(context);
        albumsContentView.setOnAddAlbumClickListener(v -> onCreateAlbum());
        albumsContentView.setOnAlbumActionListener(this::onAlbumAction);
        albumsContentView.setOnAlbumClickListener(this::onAlbumClick);
        return albumsContentView;
    }

    /* ---------------- Lifecycle ---------------- */

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
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                moveToState(UiState.ERROR);
            }
        });
    }

    /* ---------------- Album Clicks ---------------- */

    private void onAlbumClick(AlbumInfo album) {
        if (released) return;

        Log.d(TAG, "Launching AlbumActivity for: " + album.name + " (" + album.id + ")");

        try {
            Context ctx = context;
            Intent intent = new Intent(ctx, AlbumActivity.class);
            intent.putExtra("album_id", album.id);
            intent.putExtra("album_name", album.name);

            ctx.startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Failed to launch AlbumActivity", e);
            Toast.makeText(context, "Unable to open album", Toast.LENGTH_SHORT).show();
        }
    }


    private void onAlbumAction(AlbumInfo album) {
        showItemActionPopup(
                album.name.toUpperCase(),
                new String[]{"Rename", "Delete"},
                TAG,
                new ItemActionView.Callback() {
                    @Override
                    public void onActionSelected(int index, String label) {
                        hideItemActionPopup();
                        if (index == 0) showRenameAlbum(album);
                        else showDeleteAlbumConfirm(album);
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Album action cancelled");
                    }
                }
        );
    }

    /* ---------------- Rename (cache-first) ---------------- */

    private void showRenameAlbum(AlbumInfo album) {
        showFolderActionPopup(
                "Rename Album",
                album.name,
                "Rename",
                TAG,
                new FolderActionView.Callback() {
                    @Override
                    public void onCreate(String name) {
                        renameAlbum(album, name);
                    }

                    @Override
                    public void onCancel() {
                        hideFolderActionPopup();
                    }
                }
        );
    }

    private void renameAlbum(AlbumInfo album, String newName) {
        hideFolderActionPopup();

        String trimmed = newName == null ? "" : newName.trim();
        if (trimmed.isEmpty() || trimmed.equals(album.name)) {
            Toast.makeText(context, "Name is unchanged", Toast.LENGTH_SHORT).show();
            return;
        }

        final AlbumInfo oldAlbum = album;

        AlbumInfo updated = new AlbumInfo(
                album.id,
                trimmed,
                album.createdTime,
                System.currentTimeMillis(),
                album.coverPath
        );

        onAlbumUpdated(album.id, updated);

        drive.renameAlbum(executor, album.id, trimmed,
                new AlbumsDriveHelper.RenameAlbumCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Album renamed: " + oldAlbum.name + " → " + trimmed);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (released) return;

                        Log.e(TAG, "renameAlbum failed", e);
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        onAlbumUpdated(oldAlbum.id, oldAlbum);
                    }
                });
    }

    /* ---------------- Create (cache-first) ---------------- */

    private void onCreateAlbum() {
        if (released) return;

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
                        hideFolderActionPopup();
                    }
                }
        );
    }

    private void createAlbum(String name) {
        hideFolderActionPopup();

        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            Toast.makeText(context, "Album name required", Toast.LENGTH_SHORT).show();
            return;
        }

        long time = System.currentTimeMillis();
        AlbumInfo tempAlbum = new AlbumInfo(
                "temp_" + time,
                trimmed,
                time,
                time,
                null
        );

        albumsContentView.addAlbum(tempAlbum);
        moveToState(UiState.CONTENT);

        drive.createAlbum(executor, trimmed,
                new AlbumsDriveHelper.CreateAlbumCallback() {
                    @Override
                    public void onSuccess(AlbumInfo realAlbum) {
                        if (released) return;
                        albumsContentView.deleteAlbum(tempAlbum.id);
                        albumsContentView.addAlbum(realAlbum);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (released) return;

                        Log.e(TAG, "createAlbum failed", e);
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        albumsContentView.deleteAlbum(tempAlbum.id);
                        if (albumsContentView.isEmpty()) moveToState(UiState.EMPTY);
                    }
                });
    }

    /* ---------------- Delete (cache-first) ---------------- */

    private void showDeleteAlbumConfirm(AlbumInfo album) {
        showConfirmActionPopup(
                "Delete album?",
                "This will permanently delete \"" + album.name + "\" and all its contents.",
                "Delete",
                ConfirmActionView.RISK_DESTRUCTIVE,
                TAG,
                new ConfirmActionView.Callback() {
                    @Override
                    public void onConfirm() {
                        deleteAlbum(album);
                    }

                    @Override
                    public void onCancel() {
                        hideConfirmActionPopup();
                    }
                }
        );
    }

    private void deleteAlbum(AlbumInfo album) {
        hideConfirmActionPopup();

        albumsContentView.deleteAlbum(album.id);
        if (albumsContentView.isEmpty()) moveToState(UiState.EMPTY);

        drive.deleteAlbum(executor, album.id,
                new AlbumsDriveHelper.DeleteAlbumCallback() {
                    @Override
                    public void onSuccess(String albumId) {
                        if (released) return;
                        Log.d(TAG, "Album deleted: " + album.name);
                        onAlbumDeleted(albumId);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (released) return;

                        Log.e(TAG, "deleteAlbum failed", e);
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        albumsContentView.addAlbum(album);
                        moveToState(UiState.CONTENT);
                    }
                });
    }

    /* ---------------- External hooks ---------------- */

    public void onAlbumUpdated(String albumId, AlbumInfo album) {
        if (released || albumsContentView == null) return;
        albumsContentView.updateAlbum(albumId, album);
    }

    public void onAlbumDeleted(String albumId) {
        if (released || albumsContentView == null) return;
        albumsContentView.deleteAlbum(albumId);
        if (state == UiState.CONTENT && albumsContentView.isEmpty()) {
            moveToState(UiState.EMPTY);
        }
    }

    /* ---------------- State ---------------- */

    private void moveToState(UiState newState) {
        if (state == newState) return;

        Log.d(TAG, "UI state: " + state + " → " + newState);
        state = newState;

        switch (newState) {
            case LOADING: showLoading(); break;
            case EMPTY:
            case ERROR: showEmpty(); break;
            case CONTENT: showContent(); break;
        }
    }

    @Override
    public boolean onBackPressed() {
        if (confirmActionView != null && confirmActionView.isVisible()) {
            hideConfirmActionPopup();
            return true;
        }
        if (itemActionView != null && itemActionView.isVisible()) {
            hideItemActionPopup();
            return true;
        }
        if (folderActionView != null && folderActionView.isVisible()) {
            hideFolderActionPopup();
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
