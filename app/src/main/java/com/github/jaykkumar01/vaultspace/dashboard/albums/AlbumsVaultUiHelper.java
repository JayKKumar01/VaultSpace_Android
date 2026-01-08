package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.dashboard.BaseVaultSectionUiHelper;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.github.jaykkumar01.vaultspace.views.CreateFolderView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsVaultUiHelper extends BaseVaultSectionUiHelper {

    private enum UiState {
        UNINITIALIZED,
        LOADING,
        EMPTY,
        CONTENT,
        CREATING,
        ERROR
    }

    private final AlbumsDriveHelper albumsDriveHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private UiState uiState = UiState.UNINITIALIZED;
    private UiState previousState = UiState.UNINITIALIZED;
    private boolean released = false;

    public AlbumsVaultUiHelper(Context context, FrameLayout container) {
        super(context, container);

        albumsDriveHelper = new AlbumsDriveHelper(context);

        loadingView.setText("Loading albums…");

        emptyView.setIcon(R.drawable.ic_album_empty);
        emptyView.setTitle("No albums yet");
        emptyView.setSubtitle("Albums help you organize memories your way.");
        emptyView.setPrimaryAction("Create Album", v -> onCreateAlbumRequested());
        emptyView.hideSecondaryAction();
    }

    @Override
    public void show() {
        if (released || uiState != UiState.UNINITIALIZED) return;

        uiState = UiState.LOADING;
        showLoading();

        albumsDriveHelper.fetchAlbums(executor, new AlbumsDriveHelper.Callback() {
            @Override
            public void onSuccess(List<AlbumInfo> albums) {
                if (released) return;
                uiState = UiState.CONTENT;
                showContent();
            }

            @Override
            public void onEmpty() {
                if (released) return;
                uiState = UiState.EMPTY;
                showEmpty();
            }

            @Override
            public void onError(Exception e) {
                if (released) return;
                uiState = UiState.ERROR;
                showEmpty();
            }
        });
    }

    /* ---------------- Album creation flow ---------------- */

    private void onCreateAlbumRequested() {
        if (released) return;

        previousState = uiState;
        uiState = UiState.CREATING;

        showCreateFolder("Album name", new CreateFolderView.Callback() {
            @Override
            public void onCreate(String name) {
                createAlbumInternal(name);
            }

            @Override
            public void onCancel() {
                restorePreviousState();
            }
        });
    }

    /**
     * TEMPORARY stub:
     * Only validate user input UX.
     * No Drive call, no state promotion.
     */
    private void createAlbumInternal(String albumName) {
        hideCreateFolder();

        Toast.makeText(context, "Album name: " + albumName, Toast.LENGTH_SHORT).show();

        uiState = previousState;
        restorePreviousState();
    }

    private void restorePreviousState() {
        hideCreateFolder();
        uiState = previousState;

        if (uiState == UiState.EMPTY) showEmpty();
        else if (uiState == UiState.CONTENT) showContent();
        else showEmpty();
    }

    /* ---------------- Lifecycle ---------------- */

    @Override
    public void release() {
        released = true;
        executor.shutdownNow();
    }
}
