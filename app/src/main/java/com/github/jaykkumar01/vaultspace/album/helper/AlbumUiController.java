package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.AlbumContentView;
import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.views.states.EmptyStateView;
import com.github.jaykkumar01.vaultspace.views.states.LoadingStateView;

public final class AlbumUiController {

    public void onMediaAdded(AlbumMedia media) {
    }

    public void onMediaRemoved(String mediaId) {

    }

    public interface Callback {
        void onAddMediaClicked();
        void onMediaClicked(AlbumMedia media, int position);
        void onMediaLongPressed(AlbumMedia media, int position);
    }

    private final Callback callback;

    private final LoadingStateView loadingView;
    private final EmptyStateView emptyView;
    private final AlbumContentView contentView;

    public AlbumUiController(
            Context context,
            FrameLayout container,
            Callback callback
    ) {
        this.callback = callback;

        loadingView = new LoadingStateView(context);
        emptyView = new EmptyStateView(context);
        contentView = new AlbumContentView(context);

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

    public void addMedia(AlbumMedia media) {
        contentView.addMedia(media);
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
//        contentView.setListener(new AlbumContentView.Listener() {
//            @Override
//            public void onItemClick(AlbumMedia media, int position) {
//                callback.onMediaClicked(media, position);
//            }
//
//            @Override
//            public void onItemLongPress(AlbumMedia media, int position) {
//                callback.onMediaLongPressed(media, position);
//            }
//        });
    }
}
