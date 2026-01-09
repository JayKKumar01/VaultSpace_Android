package com.github.jaykkumar01.vaultspace.views;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.github.jaykkumar01.vaultspace.views.util.VaultFabUtil;

import java.util.List;

public class AlbumsContentView extends FrameLayout {

    public interface OnAlbumClickListener {
        void onAlbumClick(AlbumInfo album);
    }

    private final AlbumsAdapter adapter;
    private final ImageButton addAlbumFab;

    public AlbumsContentView(Context context) {
        super(context);

        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));

        adapter = new AlbumsAdapter();
        recyclerView.setAdapter(adapter);
        addView(recyclerView);

        addAlbumFab = VaultFabUtil.createAddAlbumFab(context);
        addView(addAlbumFab);
    }

    /* ---------------- Public API ---------------- */

    /** Initial load only */
    public void setAlbums(List<AlbumInfo> albums) {
        adapter.submitAlbums(albums);
    }

    /** Incremental insert (e.g. after createAlbum) */
    public void addAlbum(AlbumInfo album) {
        adapter.addAlbum(album);
    }

    /** Update album cover (async thumbnail / Drive update) */
    public void updateAlbumCover(String albumId, String coverPath) {
        adapter.updateAlbumCover(albumId, coverPath);
    }

    /** Update album name (rename) */
    public void updateAlbumName(String albumId, String newName) {
        adapter.updateAlbumName(albumId, newName);
    }

    public void deleteAlbum(String albumId) {
        adapter.deleteAlbum(albumId);
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
}
