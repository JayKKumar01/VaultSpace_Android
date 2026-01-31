package com.github.jaykkumar01.vaultspace.album.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayoutEngine;
import com.github.jaykkumar01.vaultspace.album.layout.PairingEngine;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.model.BandGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AlbumContentView extends FrameLayout {

    private static final String TAG = "VaultSpace:AlbumContent";

    private final RecyclerView recyclerView;
    private final AlbumBandAdapter adapter;

    private String albumId;

    private final List<AlbumMedia> media = new ArrayList<>();
    private final Map<String, List<BandLayout>> layoutMap = new LinkedHashMap<>();

    public AlbumContentView(Context c) {
        this(c, null);
    }

    public AlbumContentView(Context c, @Nullable AttributeSet a) {
        this(c, a, 0);
    }

    public AlbumContentView(Context c, @Nullable AttributeSet a, int s) {
        super(c, a, s);
        setBackgroundColor(c.getColor(R.color.vs_content_bg));

        recyclerView = new RecyclerView(c);
        recyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        recyclerView.setLayoutManager(new LinearLayoutManager(c));

        adapter = new AlbumBandAdapter();
        recyclerView.setAdapter(adapter);

        addView(recyclerView, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
    }

    /* ================= Public API ================= */

    public void setAlbum(String albumId) {
        this.albumId = albumId;
        Log.d(TAG, "album=" + albumId);
    }

    public void setMedia(Iterable<AlbumMedia> snapshot) {
        media.clear();
        for (AlbumMedia m : snapshot) media.add(m);
        rebuild();
    }

    public void addMedia(AlbumMedia m) {
        // TODO
    }

    public void removeMedia(AlbumMedia m) {
        // TODO
    }

    /* ================= Pipeline ================= */

    private void rebuild() {
        Log.d(TAG, "rebuild() called, mediaCount=" + media.size());

        if (media.isEmpty()) {
            adapter.setAll(new LinkedHashMap<>());
            return;
        }

        int width = recyclerView.getWidth();
        if (width == 0) {
            recyclerView.post(this::rebuild);
            return;
        }

        media.sort(Comparator.comparingLong((AlbumMedia m) -> m.momentMillis).reversed());

        layoutMap.clear();
        for (BandGroup group : PairingEngine.build(media)) {
            layoutMap.put(
                    group.key,
                    BandLayoutEngine.compute(albumId, width, group.bands)
            );
        }

        adapter.setAll(layoutMap);
        Log.d(TAG, "adapter.setAll() called");
    }
}
