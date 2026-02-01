package com.github.jaykkumar01.vaultspace.album.view;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AlbumContentView extends FrameLayout {

    private final RecyclerView rv;
    private final AlbumBandAdapter adapter;
    private final LayoutStateManager state;

    private final List<AlbumMedia> media = new ArrayList<>();
    private String albumId;

    public AlbumContentView(Context c) {
        this(c, null);
    }

    public AlbumContentView(Context c, @Nullable AttributeSet a) {
        this(c, a, 0);
    }

    public AlbumContentView(Context c, @Nullable AttributeSet a, int s) {
        super(c, a, s);
        setBackgroundColor(c.getColor(R.color.vs_content_bg));

        rv = new RecyclerView(c);
        rv.setLayoutManager(new LinearLayoutManager(c));
        rv.setOverScrollMode(OVER_SCROLL_NEVER);
        rv.setItemAnimator(null); // 🔒 disable all animations


        adapter = new AlbumBandAdapter();
        state = new LayoutStateManager();

        rv.setAdapter(adapter);
        addView(rv, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    public void setAlbum(String id) {
        albumId = id;
    }

    /* ===== FULL SET ===== */

    public void setMedia(Iterable<AlbumMedia> snapshot) {
        media.clear();
        for (AlbumMedia m : snapshot) if (m != null) media.add(m);

        // DESC by momentMillis
        media.sort((a, b) -> Long.compare(b.momentMillis, a.momentMillis));

        rebuild();
    }

    /* ===== ADD (simple wiring) ===== */

    public void addMedia(AlbumMedia m) {
        if (m == null) return;
        int w = rv.getWidth();
        if (w == 0) {
            rv.post(() -> addMedia(m));
            return;
        }

        LayoutResult r = state.addMedia(albumId, w, m);
        adapter.replaceRange(r.start(), r.removeCount(), r.items());
    }

    /* ===== REMOVE (simple wiring) ===== */

    public void removeMedia(AlbumMedia m) {
        if (m == null) return;
        int w = rv.getWidth();
        if (w == 0) {
            rv.post(() -> removeMedia(m));
            return;
        }

        LayoutResult r = state.removeMedia(albumId, w, m);
        adapter.replaceRange(r.start(), r.removeCount(), r.items());
    }

    /* ===== REBUILD ===== */

    private void rebuild() {
        int w = rv.getWidth();
        if (w == 0) {
            rv.post(this::rebuild);
            return;
        }

        LayoutResult r = state.setMedia(albumId, w, media);
        adapter.setAll(r.items());
    }
}
