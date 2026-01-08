package com.github.jaykkumar01.vaultspace.dashboard;

import android.content.Context;
import android.widget.FrameLayout;

import com.github.jaykkumar01.vaultspace.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumsVaultUiHelper extends BaseVaultSectionUiHelper {

    private final AlbumsDriveHelper albumsDriveHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AlbumsVaultUiHelper(Context context, FrameLayout container) {
        super(context, container);
        this.albumsDriveHelper = new AlbumsDriveHelper(context);

        loadingView.setText("Loading albums…");

        emptyView.setIcon(R.drawable.ic_album_empty);
        emptyView.setTitle("No albums yet");
        emptyView.setSubtitle("Albums help you organize memories your way.");
        emptyView.setPrimaryAction("Create Album", v -> {});
        emptyView.hideSecondaryAction();
    }

    @Override
    public void show() {
        showLoading();

        executor.execute(() -> {
            boolean hasAlbums;
            try {
                hasAlbums = albumsDriveHelper.hasAlbums();
            } catch (Exception e) {
                hasAlbums = false;
            }

            boolean finalHasAlbums = hasAlbums;
            container.post(() -> {
                if (finalHasAlbums) {
                    showContent();
                } else {
                    showEmpty();
                }
            });
        });
    }
}
