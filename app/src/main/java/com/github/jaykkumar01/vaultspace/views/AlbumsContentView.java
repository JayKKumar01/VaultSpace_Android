package com.github.jaykkumar01.vaultspace.views;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.github.jaykkumar01.vaultspace.views.util.VaultFabUtil;

import java.util.List;

public class AlbumsContentView extends FrameLayout
        implements AlbumItemCallbacks {

    /* ---------------- Public callbacks ---------------- */

    public interface OnAlbumClickListener {
        void onAlbumClick(AlbumInfo album);
    }

    public interface OnAlbumActionListener {
        void onAlbumAction(AlbumInfo album);
    }

    /* ---------------- Views ---------------- */

    private final RecyclerView recyclerView;
    private final AlbumsAdapter adapter;
    private final ImageButton addAlbumFab;

    /* ---------------- Listeners ---------------- */

    private OnAlbumActionListener albumActionListener;

    public AlbumsContentView(Context context) {
        super(context);

        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));

        adapter = new AlbumsAdapter();
        adapter.setAlbumItemCallbacks(this);
        recyclerView.setAdapter(adapter);
        addView(recyclerView);

        addAlbumFab = VaultFabUtil.createAddAlbumFab(context);
        addView(addAlbumFab);
    }

    /* ---------------- Adapter → ContentView ---------------- */

    @Override
    public void onOverflowClicked(AlbumInfo album) {
        if (albumActionListener != null) {
            albumActionListener.onAlbumAction(album);
        }
    }

    @Override
    public void onLongPressed(AlbumInfo album) {
        if (albumActionListener != null) {
            albumActionListener.onAlbumAction(album);
        }
    }

    /* ---------------- Public API ---------------- */

    /** Initial load only */
    public void setAlbums(List<AlbumInfo> albums) {
        adapter.submitAlbums(albums);
    }

    /** Incremental insert (newest-first UX) */
    public void addAlbum(AlbumInfo album) {
        adapter.addAlbum(album);
        scrollToTop();
    }

    public void updateAlbum(String albumId, AlbumInfo updated) {
        adapter.updateAlbum(albumId, updated);
        scrollToTop();
    }


    public void deleteAlbum(String albumId) {
        adapter.deleteAlbum(albumId);
    }

    public boolean isEmpty() {
        return adapter.getItemCount() == 0;
    }

    public void setOnAddAlbumClickListener(OnClickListener listener) {
        addAlbumFab.setOnClickListener(listener);
    }

    public void setFabVisible(boolean visible) {
        addAlbumFab.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setOnAlbumClickListener(OnAlbumClickListener listener) {
        adapter.setOnAlbumClickListener(listener);
    }

    public void setOnAlbumActionListener(OnAlbumActionListener listener) {
        this.albumActionListener = listener;
    }

    /* ---------------- Scroll helper ---------------- */

    private void scrollToTop() {
        recyclerView.post(() -> recyclerView.scrollToPosition(0));
    }
}
