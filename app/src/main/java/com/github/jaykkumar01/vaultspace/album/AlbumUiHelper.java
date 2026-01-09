package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.dashboard.albums.AlbumsVaultUiHelper;
import com.github.jaykkumar01.vaultspace.views.states.EmptyStateView;
import com.github.jaykkumar01.vaultspace.views.states.LoadingStateView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumUiHelper {

    private static final String TAG = "VaultSpace:AlbumUI";

    /* ---------------- State ---------------- */

    private enum UiState {
        UNINITIALIZED,
        LOADING,
        EMPTY,
        CONTENT,
        ERROR
    }

    private UiState state = UiState.UNINITIALIZED;
    private boolean released;

    /* ---------------- Core ---------------- */

    private final Context context;
    private final FrameLayout container;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /* ---------------- Views ---------------- */

    private final LoadingStateView loadingView;
    private final EmptyStateView emptyView;
    private final AlbumContentView contentView;

    /* ---------------- Album ---------------- */

    private final String albumId;

    public AlbumUiHelper(Context context, FrameLayout container, String albumId) {
        this.context = context;
        this.container = container;
        this.albumId = albumId;

        /* Init views (code-only) */
        loadingView = new LoadingStateView(context);
        emptyView = new EmptyStateView(context);
        contentView = new AlbumContentView(context);

        container.addView(contentView);
        container.addView(emptyView);
        container.addView(loadingView);

        configureEmptyState();
        loadingView.setText("Loading media…");
        showLoading();

        Log.d(TAG, "init for album: " + albumId);
    }

    /* ---------------- Public API ---------------- */

    public void show() {
        if (released || state != UiState.UNINITIALIZED) return;

        moveToState(UiState.LOADING);

        // TEMP: stub media fetch
        executor.execute(() -> {
            try {
                Thread.sleep(5000);

                if (released) return;

                // TODO replace with real media fetch
                boolean hasMedia = false;

                container.post(() -> {
                    if (hasMedia) {
                        moveToState(UiState.CONTENT);
                    } else {
                        moveToState(UiState.EMPTY);
                    }
                });

            } catch (Exception e) {
                if (released) return;
                Log.e(TAG, "Media load failed", e);
                container.post(() -> {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    moveToState(UiState.ERROR);
                });
            }
        });
    }

    /* ---------------- State ---------------- */

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


    /* ---------------- States ---------------- */

    protected void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
    }

    protected void showEmpty() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
    }

    protected void showContent() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
    }

    /* ---------------- Empty ---------------- */

    private void configureEmptyState() {
        emptyView.setIcon(R.drawable.ic_album_empty);
        emptyView.setTitle("No memories here yet");
        emptyView.setSubtitle("Add photos or videos to start capturing moments");
        emptyView.setPrimaryAction("Add Media", v ->
                Log.d(TAG, "Add Media clicked (stub)")
        );
        emptyView.hideSecondaryAction();
    }

    /* ---------------- Lifecycle ---------------- */

    public void release() {
        released = true;
        executor.shutdownNow();
    }
}
