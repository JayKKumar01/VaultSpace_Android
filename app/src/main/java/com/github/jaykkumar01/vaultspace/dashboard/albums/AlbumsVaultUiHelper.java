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

        albumsContentView.setOnAlbumClickListener(this::onAlbumClick);
        albumsContentView.setOnAlbumClickListener(album ->
                Log.d(TAG, "Album clicked: " + album.name + " (" + album.id + ")")
        );

        albumsContentView.setOnAlbumActionListener(onAlbumAction());

        return albumsContentView;
    }

    private void onAlbumClick(AlbumInfo album){
        Log.d(TAG, "Album clicked: " + album.name + " (" + album.id + ")");
    }

    private AlbumsContentView.OnAlbumActionListener onAlbumAction(){
        return new AlbumsContentView.OnAlbumActionListener(){
            @Override
            public void onAlbumOverflowClicked(AlbumInfo album){
                Log.d(TAG,"Overflow clicked: "+album.name+" ("+album.id+")");
                Toast.makeText(context,"Overflow clicked: "+album.name,Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onAlbumLongPressed(AlbumInfo album){
                Log.d(TAG,"Long-pressed: "+album.name+" ("+album.id+")");
                Toast.makeText(context,"Long-pressed: "+album.name,Toast.LENGTH_SHORT).show();
            }
        };
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
        moveToState(UiState.LOADING);

        drive.createAlbum(executor, name,
                new AlbumsDriveHelper.CreateAlbumCallback() {

                    @Override
                    public void onSuccess(AlbumInfo album) {
                        if (released) return;

                        albumsContentView.addAlbum(album);
                        moveToState(UiState.CONTENT);
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

                        // Restore correct UI state
                        if (albumsContentView.isEmpty()) {
                            moveToState(UiState.EMPTY);
                        } else {
                            moveToState(UiState.CONTENT);
                        }
                    }
                });
    }

    /* ---------------- Drive update hooks ---------------- */

    public void onAlbumCoverUpdated(String albumId, String coverPath) {
        if (released || albumsContentView == null) return;
        albumsContentView.updateAlbumCover(albumId, coverPath);
    }

    public void onAlbumRenamed(String albumId, String newName) {
        if (released || albumsContentView == null) return;
        albumsContentView.updateAlbumName(albumId, newName);
    }

    public void onAlbumDeleted(String albumId) {
        if (released || albumsContentView == null) return;

        albumsContentView.deleteAlbum(albumId);

        if (state == UiState.CONTENT && albumsContentView.isEmpty()) {
            moveToState(UiState.EMPTY);
        }
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

    /* ---------------- Back / Lifecycle ---------------- */

    @Override
    public boolean onBackPressed() {
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
