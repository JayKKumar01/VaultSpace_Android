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
        return albumsContentView;
    }

    private List<AlbumInfo> createDummyAlbums() {
        List<AlbumInfo> list = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();

        int count = 5 + new java.util.Random().nextInt(6); // 5–10

        for (int i = 1; i <= count; i++) {
            list.add(new AlbumInfo(
                    "temp_" + i,
                    "Album " + i,
                    now - (i * 100000),
                    now
            ));
        }
        return list;
    }



    @Override
    public void show() {
        if (released || state != UiState.UNINITIALIZED) {
            Log.d(TAG, "show() ignored state=" + state + " released=" + released);
            return;
        }

        state = UiState.LOADING;
        showLoading();
        Log.d(TAG, "state=LOADING");

        drive.fetchAlbums(executor, new AlbumsDriveHelper.Callback() {

            @Override
            public void onSuccess(List<AlbumInfo> albums) {
                if (released) return;
                state = UiState.CONTENT;
                Log.d(TAG, "state=CONTENT size=" + albums.size());
                showContent();
            }

            @Override
            public void onEmpty() {
                if (released) return;
                state = UiState.EMPTY;
                Log.d(TAG, "state=EMPTY");
                showEmpty();
            }

            @Override
            public void onError(Exception e) {
                if (released) return;
                state = UiState.ERROR;
                Log.e(TAG, "fetch error", e);
                showEmpty();
            }
        });
    }

    /* ---------------- Overlay ---------------- */

    private void onCreateAlbum() {
        if (released) return;

        Log.d(TAG, "show create popup state=" + state);

        showCreatePopup(
                "Create Album",
                "Album name",
                "Create",
                TAG,
                new FolderActionView.Callback() {
                    @Override public void onCreate(String name) { createAlbum(name); }
                    @Override public void onCancel() { hideFolderActionPopup(); }
                }
        );
    }

    private void createAlbum(String name) {
        hideFolderActionPopup();
        Log.d(TAG, "createAlbum name=" + name);
        Toast.makeText(context, "Album name: " + name, Toast.LENGTH_SHORT).show();
        // Future: Drive call + reload
        if (albumsContentView != null) {
            albumsContentView.submitAlbums(createDummyAlbums());
            showContent();
        }
    }


    /* ---------------- Back ---------------- */

    @Override
    public boolean onBackPressed() {
        if (folderActionView != null && folderActionView.isVisible()) {
            Log.d(TAG, "back → dismiss popup");
            hideFolderActionPopup();
            return true;
        }
        return false;
    }

    /* ---------------- Lifecycle ---------------- */

    @Override
    public void release() {
        released = true;
        executor.shutdownNow();
        Log.d(TAG, "release");
    }
}
