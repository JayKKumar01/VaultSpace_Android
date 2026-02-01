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
import com.github.jaykkumar01.vaultspace.album.band.TimeBucket;
import com.github.jaykkumar01.vaultspace.album.band.TimeBucketizer;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
import com.github.jaykkumar01.vaultspace.album.layout.BandLayoutEngine;
import com.github.jaykkumar01.vaultspace.album.layout.PairingEngine;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AlbumContentView extends FrameLayout {

    private static final String TAG = "VaultSpace:AlbumContent";
    private static final SimpleDateFormat MONTH_LABEL =
            new SimpleDateFormat("MMM yyyy", Locale.US);

    private final RecyclerView recyclerView;
    private final AlbumBandAdapter adapter;

    private String albumId;

    /* ================= Caches ================= */

    // groupKey -> media sorted DESC by momentMillis
    private final Map<String, List<AlbumMedia>> groupMediaMap = new HashMap<>();

    // groupKey -> computed layouts
    private final Map<String, List<BandLayout>> layoutMap = new HashMap<>();

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

    /**
     * Full snapshot replace.
     * Builds groupMediaMap in DESC order and triggers rebuild.
     */
    public void setMedia(Iterable<AlbumMedia> snapshot) {
        groupMediaMap.clear();

        long now = System.currentTimeMillis();

        for (AlbumMedia m : snapshot) {
            String key = TimeBucketizer.resolveKey(m.momentMillis, now);
            insertSorted(groupMediaMap.computeIfAbsent(key, k -> new ArrayList<>()), m);
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

        // 1. Insert into cached group media (DESC invariant)
        List<AlbumMedia> groupMedia =
                groupMediaMap.computeIfAbsent(groupKey, k -> new ArrayList<>());
        insertSorted(groupMedia, m);

        // 2. Resolve label once
        String label = resolveLabel(groupKey, m.momentMillis, now);

        // 3. Pair + layout ONLY this group
        List<Band> bands = PairingEngine.pair(groupMedia, label);
        List<BandLayout> newLayouts =
                BandLayoutEngine.compute(albumId, width, bands);

        // 4. Push delta to adapter
        List<BandLayout> oldLayouts = layoutMap.get(groupKey);
        int removed = oldLayouts == null ? 0 : oldLayouts.size();

        layoutMap.put(groupKey, newLayouts);
        adapter.onGroupChanged(groupKey, 0, newLayouts, removed);
    }

    public void removeMedia(AlbumMedia m) {
        // mirror of addMedia (intentionally deferred)
    }

    /* ================= Full rebuild ================= */

    private void rebuild() {
        Log.d(TAG, "rebuild() called");

        if (groupMediaMap.isEmpty()) {
            layoutMap.clear();
            adapter.setAll(new HashMap<>());
            return;
        }

        int width = recyclerView.getWidth();
        if (width == 0) {
            recyclerView.post(this::rebuild);
            return;
        }

        layoutMap.clear();

        long now = System.currentTimeMillis();
        List<TimeBucket> buckets = TimeBucketizer.buildBuckets(now);

        for (Map.Entry<String, List<AlbumMedia>> e : groupMediaMap.entrySet()) {
            List<AlbumMedia> groupMedia = e.getValue();
            if (groupMedia.isEmpty()) continue;

            String key = e.getKey();
            String label = resolveLabel(key, groupMedia.get(0).momentMillis, buckets);

            List<Band> bands = PairingEngine.pair(groupMedia, label);
            layoutMap.put(
                    key,
                    BandLayoutEngine.compute(albumId, width, bands)
            );
        }

        adapter.setAll(layoutMap);
        Log.d(TAG, "adapter.setAll() called");
    }

    /* ================= Helpers ================= */

    private static void insertSorted(List<AlbumMedia> list, AlbumMedia m) {
        int i = 0;
        while (i < list.size() && list.get(i).momentMillis > m.momentMillis) i++;
        list.add(i, m);
    }

    private static String resolveLabel(String key, long momentMillis, long now) {
        return resolveLabel(key, momentMillis, TimeBucketizer.buildBuckets(now));
    }

    private static String resolveLabel(
            String key,
            long momentMillis,
            List<TimeBucket> buckets
    ) {
        for (TimeBucket b : buckets) {
            if (b.key.equals(key)) {
                return switch (b.type) {
                    case TODAY -> "Today";
                    case YESTERDAY -> "Yesterday";
                    case THIS_WEEK -> "This Week";
                    case THIS_MONTH -> "This Month";
                    default -> "";
                };
            }
        }
        return MONTH_LABEL.format(momentMillis);
    }
}
