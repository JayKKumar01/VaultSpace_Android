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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
        albumsContentView.setOnAlbumClickListener(album -> {
            // TEMP: future navigation hook
            Log.d(TAG, "Album clicked: " + album.name);
            Toast.makeText(context, "Clicked: " + album.name, Toast.LENGTH_SHORT).show();
        });
        return albumsContentView;
    }

    @Override
    public void show() {
        if (released || state != UiState.UNINITIALIZED) return;

        state = UiState.LOADING;
        showLoading();

        drive.fetchAlbums(executor, new AlbumsDriveHelper.Callback() {

            @Override
            public void onSuccess(List<AlbumInfo> albums) {
                if (released) return;
                state = UiState.CONTENT;
                albumsContentView.submitAlbums(albums);
                showContent();
            }

            @Override
            public void onEmpty() {
                if (released) return;
                state = UiState.EMPTY;
                showEmpty();
            }

            @Override
            public void onError(Exception e) {
                if (released) return;
                state = UiState.ERROR;
                showEmpty();
            }
        });
    }

    /* ---------------- Overlay ---------------- */

    private void onCreateAlbum() {
        if (released) return;

        if (albumsContentView != null) {
            albumsContentView.setFabVisible(false);
        }

        showCreatePopup(
                "Create Album",
                "Album name",
                "Create",
                TAG,
                new FolderActionView.Callback() {
                    @Override public void onCreate(String name) { createAlbum(name); }
                    @Override public void onCancel() { restoreFab(); hideFolderActionPopup(); }
                }
        );
    }

    private void createAlbum(String name) {
        hideFolderActionPopup();
        restoreFab();

        Toast.makeText(context, "Album name: " + name, Toast.LENGTH_SHORT).show();

        if (albumsContentView != null) {
            albumsContentView.submitAlbums(createDummyAlbums());
            showContent();
        }
    }

    private void restoreFab() {
        if (albumsContentView != null) {
            albumsContentView.setFabVisible(true);
        }
    }

    private List<AlbumInfo> createDummyAlbums() {
        List<AlbumInfo> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        int count = 8 + new Random().nextInt(6);

        for (int i = 1; i <= count; i++) {
            list.add(new AlbumInfo(
                    "temp_" + i,
                    "Album " + i,
                    now - (i * 100000L),
                    now
            ));
        }
        return list;
    }

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
