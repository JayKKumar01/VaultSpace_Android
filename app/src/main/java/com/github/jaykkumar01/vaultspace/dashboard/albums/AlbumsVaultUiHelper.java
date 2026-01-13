package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.activities.AlbumActivity;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.AlbumsCache;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.BaseVaultSectionUiHelper;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.form.FormSpec;
import com.github.jaykkumar01.vaultspace.views.popups.list.ListSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsVaultUiHelper extends BaseVaultSectionUiHelper {

    private static final String TAG = "VaultSpace:AlbumsUI";

    private enum UiState { UNINITIALIZED, LOADING, EMPTY, CONTENT, ERROR }

    private final AlbumsDriveHelper drive;
    private final AlbumsCache cache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private UiState state = UiState.UNINITIALIZED;
    private boolean released;

    private AlbumsContentView albumsContentView;

    public AlbumsVaultUiHelper(
            Context context,
            FrameLayout container,
            ModalHost hostView
    ) {
        super(context, container, hostView);

        drive = new AlbumsDriveHelper(context);
        cache = new UserSession(context).getVaultCache().albums;

        initStaticUi();
    }

    private void initStaticUi() {
        loadingView.setText("Loading albums…");

        emptyView.setIcon(R.drawable.ic_album_empty);
        emptyView.setTitle("No albums yet");
        emptyView.setSubtitle("Albums help you organize memories your way.");
        emptyView.setPrimaryAction("Create Album", v -> onCreateAlbum());
        emptyView.hideSecondaryAction();

        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
    }

    @Override
    protected View createContentView(Context context) {
        albumsContentView = new AlbumsContentView(context);
        albumsContentView.setOnAddAlbumClickListener(v -> onCreateAlbum());
        albumsContentView.setOnAlbumActionListener(this::onAlbumAction);
        albumsContentView.setOnAlbumClickListener(this::onAlbumClick);
        return albumsContentView;
    }

    /* ==========================================================
     * Entry
     * ========================================================== */

    @Override
    public void show() {
        if (released || state != UiState.UNINITIALIZED) return;

        moveToState(UiState.LOADING);

        if (cache.isInitialized()) {
            renderFromCache();
            return;
        }

        drive.fetchAlbums(executor, new AlbumsDriveHelper.FetchCallback() {
            @Override
            public void onResult(List<AlbumInfo> albums) {
                if (released) return;
                cache.initializeFromDrive(albums);
                renderFromCache();
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

    /* ==========================================================
     * Rendering
     * ========================================================== */

    private void renderFromCache() {
        List<AlbumInfo> snapshot = new ArrayList<>();
        for (AlbumInfo a : cache.getAlbumsView()) snapshot.add(a);

        if (snapshot.isEmpty()) {
            moveToState(UiState.EMPTY);
        } else {
            albumsContentView.setAlbums(snapshot);
            moveToState(UiState.CONTENT);
        }
    }

    /* ==========================================================
     * Album interactions
     * ========================================================== */

    private void onAlbumClick(AlbumInfo album) {
        if (released) return;

        try {
            Intent intent = new Intent(context, AlbumActivity.class);
            intent.putExtra("album_id", album.id);
            intent.putExtra("album_name", album.name);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch AlbumActivity", e);
            Toast.makeText(context, "Unable to open album", Toast.LENGTH_SHORT).show();
        }
    }

    private void onAlbumAction(AlbumInfo album) {
        hostView.request(new ListSpec(
                album.name.toUpperCase(),
                Arrays.asList("Rename", "Delete"),
                index -> {
                    if (index == 0) {
                        showRenameAlbum(album);
                    } else {
                        showDeleteAlbumConfirm(album);
                    }
                },
                null
        ));

    }

    /* ==========================================================
     * Rename
     * ========================================================== */

    private void showRenameAlbum(AlbumInfo album) {

        hostView.request(new FormSpec(
                "Rename Album",
                album.name,
                "Create",
                name -> renameAlbum(album,name),
                null
        ));
    }

    private void renameAlbum(AlbumInfo old, String newName) {

        String trimmed = newName == null ? "" : newName.trim();
        if (trimmed.isEmpty() || trimmed.equals(old.name)) {
            Toast.makeText(context, "Name is unchanged", Toast.LENGTH_SHORT).show();
            return;
        }

        AlbumInfo updated = new AlbumInfo(
                old.id,
                trimmed,
                old.createdTime,
                System.currentTimeMillis(),
                old.coverPath
        );

        cache.replaceAlbum(updated);
        albumsContentView.updateAlbum(old.id, updated);

        drive.renameAlbum(executor, old.id, trimmed,
                new AlbumsDriveHelper.RenameAlbumCallback() {
                    @Override
                    public void onSuccess(AlbumInfo real) {}

                    @Override
                    public void onError(Exception e) {
                        if (released) return;
                        cache.replaceAlbum(old);
                        albumsContentView.updateAlbum(old.id, old);
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /* ==========================================================
     * Create
     * ========================================================== */

    private void onCreateAlbum() {
        if (released) return;
        hostView.request(new FormSpec(
                "Create Album",
                "Album name",
                "Create",
                this::createAlbum,
                null
        ));
    }

    private void createAlbum(String name) {

        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            Toast.makeText(context, "Album name required", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();
        AlbumInfo temp = new AlbumInfo(
                "temp_" + now, trimmed, now, now, null
        );

        albumsContentView.addAlbum(temp);
        moveToState(UiState.CONTENT);

        drive.createAlbum(executor, trimmed,
                new AlbumsDriveHelper.CreateAlbumCallback() {
                    @Override
                    public void onSuccess(AlbumInfo real) {
                        if (released) return;
                        albumsContentView.deleteAlbum(temp.id);
                        cache.addAlbum(real);
                        albumsContentView.addAlbum(real);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (released) return;
                        albumsContentView.deleteAlbum(temp.id);
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        if (albumsContentView.isEmpty()) moveToState(UiState.EMPTY);
                    }
                });
    }

    /* ==========================================================
     * Delete
     * ========================================================== */

    private void showDeleteAlbumConfirm(AlbumInfo album) {
        hostView.request(new ConfirmSpec(
                "Delete album?",
                "This will permanently delete \"" + album.name + "\" and all its contents.",
                true,
                ConfirmView.RISK_CRITICAL,
                () -> deleteAlbum(album),
                null

        ));
    }

    private void deleteAlbum(AlbumInfo album) {
        cache.removeAlbum(album.id);
        albumsContentView.deleteAlbum(album.id);

        if (albumsContentView.isEmpty()) moveToState(UiState.EMPTY);

        drive.deleteAlbum(executor, album.id,
                new AlbumsDriveHelper.DeleteAlbumCallback() {
                    @Override
                    public void onSuccess(String albumId) {}

                    @Override
                    public void onError(Exception e) {
                        if (released) return;
                        cache.addAlbum(album);
                        albumsContentView.addAlbum(album);
                        moveToState(UiState.CONTENT);
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /* ==========================================================
     * State & lifecycle
     * ========================================================== */

    private void moveToState(UiState newState) {
        if (state == newState) return;

        Log.d(TAG, "state " + state + " → " + newState);
        state = newState;

        switch (newState) {
            case LOADING:
                showLoading();
                break;
            case CONTENT:
                showContent();
                break;
            case EMPTY:
            case ERROR:
                showEmpty();
                break;
        }
    }


    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onRelease() {
        released = true;
        executor.shutdownNow();
    }
}
