package com.github.jaykkumar01.vaultspace.album.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.band.TimeBucketizer;
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

    /* ================= Caches ================= */

    // groupKey -> sorted media (DESC by momentMillis)
    private final Map<String, List<AlbumMedia>> groupMediaMap = new LinkedHashMap<>();

    // groupKey -> computed layouts
    private final Map<String, List<BandLayout>> layoutMap = new LinkedHashMap<>();

    /* ================= ctor ================= */

    public AlbumContentView(Context c) { this(c, null); }
    public AlbumContentView(Context c, @Nullable AttributeSet a) { this(c, a, 0); }

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
        groupMediaMap.clear();
        for (AlbumMedia m : snapshot) {
            long now = System.currentTimeMillis();
            String key = TimeBucketizer.resolveKey(m.momentMillis, now);
            groupMediaMap.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
        }
        rebuild();
    }

    /* ================= Incremental add ================= */

    public void addMedia(AlbumMedia m) {
        if (m == null) return;

        int width = recyclerView.getWidth();
        if (width == 0) {
            recyclerView.post(this::rebuild);
            return;
        }

        long now = System.currentTimeMillis();
        String groupKey = TimeBucketizer.resolveKey(m.momentMillis, now);

        // 1. Insert into cached group media (sorted DESC)
        List<AlbumMedia> groupMedia =
                groupMediaMap.computeIfAbsent(groupKey, k -> new ArrayList<>());

        int i = 0;
        while (i < groupMedia.size() && groupMedia.get(i).momentMillis > m.momentMillis) i++;
        groupMedia.add(i, m);

        // 2. Recompute pairing + layout ONLY for this group
        BandGroup g = PairingEngine.build(groupMedia).get(0);
        List<BandLayout> newLayouts =
                BandLayoutEngine.compute(albumId, width, g.bands);

        // 3. Push delta to adapter
        List<BandLayout> oldLayouts = layoutMap.get(groupKey);
        int removed = oldLayouts == null ? 0 : oldLayouts.size();

        layoutMap.put(groupKey, newLayouts);
        adapter.onGroupChanged(groupKey, 0, newLayouts, removed);
    }

    public void removeMedia(AlbumMedia m) {
        // to be implemented (mirror of addMedia)
    }

    /* ================= Full rebuild ================= */

    private void rebuild() {
        Log.d(TAG, "rebuild() called");

        if (groupMediaMap.isEmpty()) {
            layoutMap.clear();
            adapter.setAll(new LinkedHashMap<>());
            return;
        }

        int width = recyclerView.getWidth();
        if (width == 0) {
            recyclerView.post(this::rebuild);
            return;
        }

        layoutMap.clear();

        // Ensure per-group order (DESC)
        for (List<AlbumMedia> list : groupMediaMap.values()) {
            list.sort(Comparator.comparingLong((AlbumMedia m) -> m.momentMillis).reversed());
        }

        // Build layouts per group
        for (Map.Entry<String, List<AlbumMedia>> e : groupMediaMap.entrySet()) {
            List<AlbumMedia> groupMedia = e.getValue();
            if (groupMedia.isEmpty()) continue;

            BandGroup g = PairingEngine.build(groupMedia).get(0);
            layoutMap.put(
                    e.getKey(),
                    BandLayoutEngine.compute(albumId, width, g.bands)
            );
        }

        adapter.setAll(layoutMap);
        Log.d(TAG, "adapter.setAll() called");
    }
}
