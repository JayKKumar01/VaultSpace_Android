package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.widget.FrameLayout;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.dashboard.BaseVaultSectionUiHelper;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsVaultUiHelper extends BaseVaultSectionUiHelper {

    private enum UiState {
        UNINITIALIZED,
        LOADING,
        EMPTY,
        CONTENT,
        ERROR
    }

    private final AlbumsDriveHelper albumsDriveHelper;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private UiState uiState = UiState.UNINITIALIZED;
    private boolean released = false;

    public AlbumsVaultUiHelper(Context context, FrameLayout container) {
        super(context, container);

        this.albumsDriveHelper = new AlbumsDriveHelper(context);

        loadingView.setText("Loading albums…");

        emptyView.setIcon(R.drawable.ic_album_empty);
        emptyView.setTitle("No albums yet");
        emptyView.setSubtitle("Albums help you organize memories your way.");
        emptyView.setPrimaryAction("Create Album", v -> {
            // album creation flow later
        });
        emptyView.hideSecondaryAction();
    }

    @Override
    public void show() {
        if (released) return;

        if (uiState != UiState.UNINITIALIZED) {
            return;
        }

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

    /**
     * Call this after album creation / deletion.
     */
    public void refresh() {
        if (released) return;

        albumsDriveHelper.invalidateCache();
        uiState = UiState.UNINITIALIZED;
        show();
    }

    @Override
    public void release() {
        released = true;
        executor.shutdownNow();
    }
}
