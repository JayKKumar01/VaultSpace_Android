package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.activities.AlbumActivity;
import com.github.jaykkumar01.vaultspace.core.drive.AlbumsRepository;
import com.github.jaykkumar01.vaultspace.dashboard.base.BaseSectionUi;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.form.FormSpec;
import com.github.jaykkumar01.vaultspace.views.popups.list.ListSpec;

import java.util.Arrays;

public final class AlbumsUi extends BaseSectionUi implements AlbumsRepository.AlbumsListener {

    private static final String TAG = "VaultSpace:AlbumsUI";

    private enum UiState {UNINITIALIZED, LOADING, EMPTY, CONTENT, ERROR}

    /* ================= Core ================= */

    private final AlbumsRepository repo;
    private UiState state = UiState.UNINITIALIZED;
    private boolean released;

    private AlbumsContentView content;

    public AlbumsUi(Context context, FrameLayout container, ModalHost hostView) {
        super(context, container, hostView);
        repo = AlbumsRepository.getInstance(context);
        initStaticUi();
    }

    /* ================= Static UI ================= */

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
        content = new AlbumsContentView(context);
        content.setOnAddAlbumClickListener(v -> onCreateAlbum());
        content.setOnAlbumClickListener(this::onAlbumClick);
        content.setOnAlbumActionListener(this::onAlbumAction);
        return content;
    }

    /* ================= Entry ================= */

    @Override
    public void show() {
        if (released || state != UiState.UNINITIALIZED) return;

        moveToState(UiState.LOADING);
        repo.addListener(this);
        repo.load(e -> {
            if (released) return;
            Log.e(TAG, "Album load failed", e);
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            moveToState(UiState.ERROR);
        });
    }

    /* ================= Repo callbacks ================= */

    @Override
    public void onAlbumsLoaded(Iterable<AlbumInfo> albums) {
        content.setAlbums(albums);
        moveToState(content.isEmpty() ? UiState.EMPTY : UiState.CONTENT);
    }

    @Override
    public void onAlbumAdded(AlbumInfo album) {
        content.addAlbum(album);
        moveToState(UiState.CONTENT);
    }

    @Override
    public void onAlbumUpdated(AlbumInfo album) {
        content.updateAlbum(album.id, album);
    }

    @Override
    public void onAlbumRemoved(String albumId) {
        content.deleteAlbum(albumId);
        if (content.isEmpty()) moveToState(UiState.EMPTY);
    }

    /* ================= Album interactions ================= */

    private void onAlbumClick(AlbumInfo album) {
        try {
            Intent intent = new Intent(context, AlbumActivity.class);
            intent.putExtra("album_id", album.id);
            intent.putExtra("album_name", album.name);
            context.startActivity(intent);

            if (context instanceof android.app.Activity)
                ((android.app.Activity) context).overridePendingTransition(
                        R.anim.album_enter,
                        R.anim.album_exit
                );

        } catch (Exception e) {
            Log.e(TAG, "Failed to open album", e);
            Toast.makeText(context, "Unable to open album", Toast.LENGTH_SHORT).show();
        }
    }

    private void onAlbumAction(AlbumInfo album) {
        hostView.request(new ListSpec(
                album.name.toUpperCase(),
                Arrays.asList("Rename", "Delete"),
                index -> {
                    if (index == 0) showRenameAlbum(album);
                    else showDeleteAlbumConfirm(album);
                },
                null
        ));
    }

    /* ================= Create ================= */

    private void onCreateAlbum() {
        hostView.request(new FormSpec(
                "Create Album",
                "Album name",
                "Create",
                name -> repo.createAlbum(name, e ->
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show()
                ),
                null
        ));
    }

    /* ================= Rename ================= */

    private void showRenameAlbum(AlbumInfo album) {
        hostView.request(new FormSpec(
                "Rename Album",
                album.name,
                "Rename",
                name -> repo.renameAlbum(album, name, e ->
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show()
                ),
                null
        ));
    }

    /* ================= Delete ================= */

    private void showDeleteAlbumConfirm(AlbumInfo album) {
        ConfirmSpec deleteSpec = new ConfirmSpec(
                "Delete album?",
                "This will permanently delete '" + album.name + "' and all its contents.",
                ConfirmView.RISK_CRITICAL
        );
        deleteSpec.onPositive(() -> repo.deleteAlbum(album, e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show()));
        hostView.request(deleteSpec);
    }

    /* ================= State ================= */

    private void moveToState(UiState newState) {
        if (state == newState) return;
        state = newState;

        switch (newState) {
            case LOADING -> showLoading();
            case CONTENT -> showContent();
            case EMPTY, ERROR -> showEmpty();
        }
    }

    /* ================= Lifecycle ================= */

    @Override
    public void onRelease() {
        released = true;
        repo.removeListener(this);
    }
}
