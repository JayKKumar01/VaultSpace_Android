package com.github.jaykkumar01.vaultspace.album.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.band.Band;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayoutEngine;
import com.github.jaykkumar01.vaultspace.album.layout.PairingEngine;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class AlbumContentView extends FrameLayout {

    private static final String TAG = "VaultSpace:AlbumContent";

    private final RecyclerView recyclerView;
    private final AlbumBandAdapter adapter;

    private String albumId;
    private String albumName;

    private final List<AlbumMedia> media = new ArrayList<>();

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
        recyclerView.setItemAnimator(null);
        recyclerView.setLayoutManager(new LinearLayoutManager(c)); // 🔥 REQUIRED

        adapter = new AlbumBandAdapter();
        recyclerView.setAdapter(adapter);


        addView(recyclerView, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
    }

    /* ================= Public API ================= */

    public void setAlbum(String albumId, String albumName) {
        this.albumId = albumId;
        this.albumName = albumName;
        Log.d(TAG, "album=" + albumId);
    }

    public void setMedia(Iterable<AlbumMedia> snapshot) {
        media.clear();
        for (AlbumMedia m : snapshot) media.add(m);
        rebuild();
    }

    public void addMedia(AlbumMedia m) {
        media.add(m);
        rebuild();
    }

    public void removeMedia(String mediaId) {

    }

    /* ================= Pipeline ================= */

    private void rebuild() {
        Log.d(TAG, "rebuild() called, mediaCount=" + media.size());

        if (media.isEmpty()) {
            Log.d(TAG, "media empty → clearing adapter");
            adapter.submitLayouts(List.of());
            return;
        }

        int width = recyclerView.getWidth();
        if (width == 0) {
            Log.d(TAG, "RecyclerView width=0 → delaying rebuild");
            recyclerView.post(this::rebuild);
            return;
        }

        Log.d(TAG, "RecyclerView width=" + width);

        // 1️⃣ sort by momentMillis (newest first)
        media.sort(Comparator.comparingLong((AlbumMedia m) -> m.momentMillis).reversed());

        // 2️⃣ pairing
        List<Band> bands = PairingEngine.build(media);
        Log.d(TAG, "PairingEngine → bandCount=" + bands.size());

        // 3️⃣ layout
        List<BandLayout> layouts = BandLayoutEngine.compute(albumId, width, bands);
        Log.d(TAG, "BandLayoutEngine → layoutCount=" + layouts.size());

        // 🟢 normalize duplicate time labels
        normalizeTimeLabels(layouts);

        adapter.submitLayouts(layouts);
        Log.d(TAG, "adapter.submitLayouts() called");
    }

    private static void normalizeTimeLabels(List<BandLayout> layouts) {
        String lastLabel = null;

        for (BandLayout layout : layouts) {
            if (layout.timeLabel != null && layout.timeLabel.equals(lastLabel)) {
                layout.showTimeLabel = false;
            } else {
                layout.showTimeLabel = true;
                lastLabel = layout.timeLabel;
            }
        }
    }




}
