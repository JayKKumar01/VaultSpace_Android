package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.interfaces.AlbumItemCallbacks;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.github.jaykkumar01.vaultspace.utils.VaultFabUtil;
import com.github.jaykkumar01.vaultspace.views.creative.delete.DeleteStatusRenderModel;
import com.github.jaykkumar01.vaultspace.views.creative.delete.DeleteStatusView;

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
    private final DeleteStatusView deleteStatusView;

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

        deleteStatusView = new DeleteStatusView(context);
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        lp.gravity = android.view.Gravity.TOP;
        deleteStatusView.setLayoutParams(lp);
        deleteStatusView.setVisibility(GONE);
        addView(deleteStatusView);
    }

    /* ---------------- Delete UI API ---------------- */

    public void showDeleteStatus(DeleteStatusRenderModel model) {
        deleteStatusView.apply(model);
        deleteStatusView.post(() -> applyDeleteInset(true));
    }

    public void hideDeleteStatus() {
        deleteStatusView.hide();
        applyDeleteInset(false);
    }


    private void applyDeleteInset(boolean visible) {
        int inset = visible ? deleteStatusView.getMeasuredHeight() : 0;
        recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                inset,
                recyclerView.getPaddingRight(),
                recyclerView.getPaddingBottom()
        );
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
