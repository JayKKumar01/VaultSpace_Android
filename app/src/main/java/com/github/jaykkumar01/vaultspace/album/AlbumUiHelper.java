package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.views.states.EmptyStateView;
import com.github.jaykkumar01.vaultspace.views.states.LoadingStateView;

import java.util.List;
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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AlbumSnapshotListener snapshotListener;
    private final String albumId;

    private final LoadingStateView loadingView;
    private final EmptyStateView emptyView;
    private final AlbumContentView contentView;

    private final AlbumDriveHelper driveHelper;

    public AlbumUiHelper(Context context, FrameLayout container, String albumId, AlbumSnapshotListener snapshotListener) {
        this.context = context;
        this.albumId = albumId;
        this.snapshotListener = snapshotListener;
        this.driveHelper = new AlbumDriveHelper(context, albumId);

        loadingView = new LoadingStateView(context);
        emptyView = new EmptyStateView(context);
        contentView = new AlbumContentView(context);

        container.addView(contentView);
        container.addView(emptyView);
        container.addView(loadingView);

        configureEmptyState();
        loadingView.setText("Loading media…");

        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);

        Log.d(TAG, "init album=" + albumId);

    }

    public void show() {
        if (released || state != UiState.UNINITIALIZED) return;
        moveToState(UiState.LOADING);

        driveHelper.fetchAlbumItems(executor, new AlbumDriveHelper.FetchCallback() {
            @Override
            public void onResult(List<AlbumMedia> items) {
                if (released) return;

                int photoCount = 0, videoCount = 0;
                for (AlbumMedia item : items) {
                    if (item.isVideo) videoCount++;
                    else photoCount++;
                }

                AlbumSnapshot snapshot = new AlbumSnapshot(albumId);
                snapshot.photoCount = photoCount;
                snapshot.videoCount = videoCount;
                snapshot.isError = false;

                snapshotListener.onAlbumSnapshot(snapshot);
                moveToState(snapshot.getTotalCount() > 0 ? UiState.CONTENT : UiState.EMPTY);
            }

            @Override
            public void onError(Exception e) {
                if (released) return;

                Log.e(TAG, "Album load failed: " + albumId, e);
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();

                AlbumSnapshot snapshot = new AlbumSnapshot(albumId);
                snapshot.isError = true;

                snapshotListener.onAlbumSnapshot(snapshot);
                moveToState(UiState.ERROR);
            }
        });
    }

    private void moveToState(UiState newState) {
        if (state == newState) return;

        Log.d(TAG, "state " + state + " → " + newState);
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
        emptyView.setPrimaryAction("Add Media", v -> {});
        emptyView.hideSecondaryAction();
    }

    public void release() {
        released = true;
        executor.shutdownNow();
        Log.d(TAG, "release album=" + albumId);
    }
}
