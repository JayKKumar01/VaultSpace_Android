package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.view.AlbumContentView;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.view.OnMediaActionListener;
import com.github.jaykkumar01.vaultspace.views.states.EmptyStateView;
import com.github.jaykkumar01.vaultspace.views.states.LoadingStateView;

public final class AlbumUiController {


    private final Context context;

    public interface Callback {
        void onAddMediaClicked();
        void onMediaClicked(AlbumMedia media);
        void onMediaLongPressed(AlbumMedia media);
    }

    private final Callback callback;

    private final LoadingStateView loadingView;
    private final EmptyStateView emptyView;
    private final AlbumContentView contentView;

    public AlbumUiController(
            Context context,
            FrameLayout container,
            Callback callback,
            String albumId) {
        this.context = context;
        this.callback = callback;

        loadingView = new LoadingStateView(context);
        emptyView = new EmptyStateView(context);
        contentView = new AlbumContentView(context);
        contentView.setAlbum(albumId);

        container.addView(contentView);
        container.addView(emptyView);
        container.addView(loadingView);

        configureEmptyState();
        hideAll();

        wireContentCallbacks();

        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
    }

    /* ---------------- UI commands ---------------- */

    public void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
    }

    public void showEmpty() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
    }

    public void showContent(Iterable<AlbumMedia> mediaList) {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        contentView.setMedia(mediaList);
    }

    public void onMediaAdded(AlbumMedia media) {
        contentView.addMedia(media);
    }

    public void onMediaRemoved(String mediaId) {
        contentView.removeMedia(mediaId);
    }

    /* ---------------- Setup ---------------- */

    private void hideAll() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
    }

    private void configureEmptyState() {
        emptyView.setIcon(R.drawable.ic_album_empty);
        emptyView.setTitle("No memories here yet");
        emptyView.setSubtitle("Add photos or videos to start capturing moments");
        emptyView.setPrimaryAction("Add Media", v -> callback.onAddMediaClicked());
        emptyView.hideSecondaryAction();
    }

    private void wireContentCallbacks() {
        contentView.setMediaActionListener(new OnMediaActionListener() {
            @Override
            public void onMediaClick(@NonNull AlbumMedia media) {
                callback.onMediaClicked(media);
            }

            @Override
            public void onMediaLongPress(@NonNull AlbumMedia media) {
                callback.onMediaLongPressed(media);
            }
        });
    }
}
