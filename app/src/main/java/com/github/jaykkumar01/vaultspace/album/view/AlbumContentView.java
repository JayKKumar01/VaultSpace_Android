package com.github.jaykkumar01.vaultspace.album.view;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.band.*;
import com.github.jaykkumar01.vaultspace.album.layout.*;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.text.SimpleDateFormat;
import java.util.*;

public final class AlbumContentView extends FrameLayout {

    private static final SimpleDateFormat MONTH_LABEL = new SimpleDateFormat("MMM yyyy", Locale.US);

    private final RecyclerView rv;
    private final AlbumBandAdapter adapter;

    private String albumId;

    /* ===== Media cache ===== */

    private final Map<String, List<AlbumMedia>> groupMedia = new HashMap<>();

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
        adapter = new AlbumBandAdapter();
        rv.setAdapter(adapter);
        addView(rv, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    public void setAlbum(String id) {
        albumId = id;
    }

    /* ===== FULL SET ===== */

    public void setMedia(Iterable<AlbumMedia> snapshot) {
        groupMedia.clear();
        long now = System.currentTimeMillis();

        for (AlbumMedia m : snapshot) {
            String k = TimeBucketizer.resolveKey(m.momentMillis, now);
            insertSorted(groupMedia.computeIfAbsent(k, x -> new ArrayList<>()), m);
        }

        rebuild();
    }

    /* ===== ADD ONE MEDIA ===== */

    public void addMedia(AlbumMedia m) {
        if (m == null) return;

        int w = rv.getWidth();
        if (w == 0) {
            rv.post(() -> addMedia(m));
            return;
        }

        long now = System.currentTimeMillis();
        String k = TimeBucketizer.resolveKey(m.momentMillis, now);

        List<AlbumMedia> gm = groupMedia.computeIfAbsent(k, x -> new ArrayList<>());
        insertSorted(gm, m);

        String label = resolveLabel(k, m.momentMillis, now);
        List<Band> bands = PairingEngine.pair(gm, label);
        List<BandLayout> layouts = BandLayoutEngine.compute(albumId, w, bands);

        adapter.onMediaAdded(k, layouts);
    }

    /* ===== REBUILD ===== */

    private void rebuild() {
        int w = rv.getWidth();
        if (w == 0) {
            rv.post(this::rebuild);
            return;
        }

        Map<String, List<BandLayout>> lm = new HashMap<>();
        long now = System.currentTimeMillis();
        List<TimeBucket> buckets = TimeBucketizer.buildBuckets(now);

        for (var e : groupMedia.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            String k = e.getKey();
            String label = resolveLabel(k, e.getValue().get(0).momentMillis, buckets);
            List<Band> bands = PairingEngine.pair(e.getValue(), label);
            lm.put(k, BandLayoutEngine.compute(albumId, w, bands));
        }

        adapter.setAll(lm);
    }

    /* ===== Helpers ===== */

    private static void insertSorted(List<AlbumMedia> l, AlbumMedia m) {
        int i = 0;
        while (i < l.size() && l.get(i).momentMillis > m.momentMillis) i++;
        l.add(i, m);
    }

    private static String resolveLabel(String k, long ms, long now) {
        return resolveLabel(k, ms, TimeBucketizer.buildBuckets(now));
    }

    private static String resolveLabel(String k, long ms, List<TimeBucket> bs) {
        for (TimeBucket b : bs)
            if (b.key.equals(k))
                return switch (b.type) {
                    case TODAY -> "Today";
                    case YESTERDAY -> "Yesterday";
                    case THIS_WEEK -> "This Week";
                    case THIS_MONTH -> "This Month";
                    default -> "";
                };
        return MONTH_LABEL.format(ms);
    }
}
