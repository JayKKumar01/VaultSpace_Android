package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.views.states.EmptyStateView;
import com.github.jaykkumar01.vaultspace.views.states.LoadingStateView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumUiHelper {

    private static final String TAG = "VaultSpace:AlbumUI";

    public interface AlbumSnapshotListener {
        void onAlbumSnapshot(AlbumSnapshot snapshot);
    }

    private enum UiState { UNINITIALIZED, LOADING, EMPTY, CONTENT, ERROR }

    private UiState state = UiState.UNINITIALIZED;
    private boolean released;

    private final Context context;
    private final FrameLayout container;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AlbumSnapshotListener snapshotListener;
    private final String albumId;

    private final LoadingStateView loadingView;
    private final EmptyStateView emptyView;
    private final AlbumContentView contentView;

    public AlbumUiHelper(Context context, FrameLayout container, String albumId, AlbumSnapshotListener snapshotListener) {
        this.context = context;
        this.container = container;
        this.albumId = albumId;
        this.snapshotListener = snapshotListener;

        loadingView = new LoadingStateView(context);
        emptyView = new EmptyStateView(context);
        contentView = new AlbumContentView(context);

        container.addView(contentView);
        container.addView(emptyView);
        container.addView(loadingView);

        configureEmptyState();
        loadingView.setText("Loading media…");

        Log.d(TAG, "init for album: " + albumId);
    }

    public void show() {
        if (released || state != UiState.UNINITIALIZED) return;

        moveToState(UiState.LOADING);

        executor.execute(() -> {
            try {
                Thread.sleep(1500);
                if (released) return;

                AlbumSnapshot snapshot = new AlbumSnapshot(albumId);
                snapshot.photoCount = 8;
                snapshot.videoCount = 4;
                snapshot.isError = false;

                container.post(() -> {
                    if (released) return;

                    emitSnapshot(snapshot);
                    moveToState(snapshot.getTotalCount() > 0 ? UiState.CONTENT : UiState.EMPTY);
                });

            } catch (Exception e) {
                if (released) return;

                Log.e(TAG, "Media load failed", e);
                container.post(() -> {
                    if (released) return;

                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();

                    AlbumSnapshot snapshot = new AlbumSnapshot(albumId);
                    snapshot.isError = true;
                    emitSnapshot(snapshot);

                    moveToState(UiState.ERROR);
                });
            }
        });
    }

    private void emitSnapshot(AlbumSnapshot snapshot) {
        snapshotListener.onAlbumSnapshot(snapshot);
    }

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

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
    }

    private void showEmpty() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
    }

    private void configureEmptyState() {
        emptyView.setIcon(R.drawable.ic_album_empty);
        emptyView.setTitle("No memories here yet");
        emptyView.setSubtitle("Add photos or videos to start capturing moments");
        emptyView.setPrimaryAction("Add Media", v -> Log.d(TAG, "Add Media clicked (stub)"));
        emptyView.hideSecondaryAction();
    }

    public void release() {
        released = true;
        executor.shutdownNow();
    }
}
