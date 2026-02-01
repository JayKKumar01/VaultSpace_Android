package com.github.jaykkumar01.vaultspace.album.view;

import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AlbumBandAdapter extends RecyclerView.Adapter<BandViewHolder> {

    /* ================= Internal State ================= */

    private final Map<String, List<BandLayout>> groupLayouts = new LinkedHashMap<>();
    private final ArrayList<BandLayout> flatLayouts = new ArrayList<>();

    private final Map<String, Integer> groupStart = new HashMap<>();
    private final Map<String, Integer> groupSize = new HashMap<>();
    private final Map<String, Integer> groupOrder = new HashMap<>();
    private final ArrayList<String> orderedGroupKeys = new ArrayList<>();



    /* ================= Notify scheduler ================= */

    private static final long NOTIFY_DELAY_MS = 64;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object notifyLock = new Object();

    private Runnable notifyRunnable;

    private int pendingStart = Integer.MAX_VALUE;
    private int pendingRemoved = 0;
    private int pendingInserted = 0;
    private int pendingChangedEnd = -1;

    private void trackChange(int start, int removed, int inserted, int changedFrom, int changedTo) {
        synchronized (notifyLock) {
            pendingStart = Math.min(pendingStart, start);
            pendingRemoved += removed;
            pendingInserted += inserted;

            if (changedFrom >= 0) {
                pendingStart = Math.min(pendingStart, changedFrom);
                pendingChangedEnd = Math.max(pendingChangedEnd, changedTo);
            }

            if (notifyRunnable != null)
                mainHandler.removeCallbacks(notifyRunnable);

            notifyRunnable = () -> {
                int s, r, i, cEnd;
                synchronized (notifyLock) {
                    s = pendingStart;
                    r = pendingRemoved;
                    i = pendingInserted;
                    cEnd = pendingChangedEnd;

                    pendingStart = Integer.MAX_VALUE;
                    pendingRemoved = 0;
                    pendingInserted = 0;
                    pendingChangedEnd = -1;
                    notifyRunnable = null;
                }

                if (r > 0)
                    notifyItemRangeRemoved(s, r);

                if (i > 0)
                    notifyItemRangeInserted(s, i);

                if (cEnd >= s) {
                    int count = cEnd - s;
                    if (count > 0)
                        notifyItemRangeChanged(s, count);
                }
            };

            mainHandler.postDelayed(notifyRunnable, NOTIFY_DELAY_MS);
        }
    }

    /* ================= Time label normalization ================= */

    private void normalizeTimeLabels(int start, int endExclusive) {
        int from = Math.max(0, start);
        int to = Math.min(flatLayouts.size(), endExclusive);

        for (int i = from; i < to; i++) {
            BandLayout curr = flatLayouts.get(i);
            String label = curr.timeLabel;

            if (label == null) curr.showTimeLabel = false;
            else if (i == 0) curr.showTimeLabel = true;
            else curr.showTimeLabel = !label.equals(flatLayouts.get(i - 1).timeLabel);
        }
    }

    /* ================= Group order ================= */

    private static int computeGroupOrder(String key) {
        return switch (key) {
            case "today" -> 0;
            case "yesterday" -> 1;
            case "this_week" -> 2;
            case "this_month" -> 3;
            default -> {
                // yyyy-MM month buckets (newer first)
                int year = Integer.parseInt(key.substring(0, 4));
                int month = Integer.parseInt(key.substring(5, 7));
                yield 10_000 - (year * 12 + month);
            }
        };
    }

    /* ================= Group insertion ================= */

    private int findGroupInsertIndex(int order) {
        int lo = 0, hi = orderedGroupKeys.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            String k = orderedGroupKeys.get(mid);
            int o = groupOrder.computeIfAbsent(k, AlbumBandAdapter::computeGroupOrder);
            if (o <= order) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }



    /* ================= set ================= */

    public void setAll(Map<String, List<BandLayout>> layoutMap) {
        groupLayouts.clear();
        flatLayouts.clear();
        groupStart.clear();
        groupSize.clear();
        orderedGroupKeys.clear();
        groupOrder.clear();


        if (layoutMap == null || layoutMap.isEmpty()) return;

        int cursor = 0;
        for (Map.Entry<String, List<BandLayout>> e : layoutMap.entrySet()) {
            String key = e.getKey();
            List<BandLayout> list = e.getValue();
            if (key == null || list == null || list.isEmpty()) continue;

            orderedGroupKeys.add(key);
            groupOrder.put(key, computeGroupOrder(key));

            groupLayouts.put(key, list);
            groupStart.put(key, cursor);
            groupSize.put(key, list.size());

            flatLayouts.addAll(list);
            cursor += list.size();
        }


        normalizeTimeLabels(0, flatLayouts.size());
        trackChange(0, 0, flatLayouts.size(), 0, flatLayouts.size());
    }

    /* ================= add / remove ================= */

    public void onGroupChanged(String groupKey, int startInGroup, List<BandLayout> changedLayouts, int removedCount) {
        if (groupKey == null) return;

        Integer gStart = groupStart.get(groupKey);
        Integer gSize  = groupSize.get(groupKey);
        List<BandLayout> group = groupLayouts.get(groupKey);

        /* ================= NEW GROUP ================= */

        if (gStart == null || gSize == null || group == null) {
            int inserted = changedLayouts == null ? 0 : changedLayouts.size();
            if (inserted == 0) return;

            int order = groupOrder.computeIfAbsent(groupKey, AlbumBandAdapter::computeGroupOrder);
            int groupIndex = findGroupInsertIndex(order);

            int flatStart = 0;
            for (int i = 0; i < groupIndex; i++) {
                Integer sz = groupSize.get(orderedGroupKeys.get(i));
                if (sz != null) flatStart += sz;
            }

            orderedGroupKeys.add(groupIndex, groupKey);
            groupLayouts.put(groupKey, new ArrayList<>(changedLayouts));
            groupStart.put(groupKey, flatStart);
            groupSize.put(groupKey, inserted);

            flatLayouts.addAll(flatStart, changedLayouts);

            for (int i = groupIndex + 1; i < orderedGroupKeys.size(); i++) {
                String k = orderedGroupKeys.get(i);
                Integer prev = groupStart.get(k);
                if (prev != null) groupStart.put(k, prev + inserted);
            }

            normalizeTimeLabels(flatStart - 1, flatStart + inserted + 1);
            trackChange(flatStart, 0, inserted, flatStart - 1, flatStart + inserted);
            return;
        }

        /* ================= EXISTING GROUP ================= */

        int flatStart = gStart + startInGroup;

        for (int i = 0; i < removedCount; i++) {
            if (startInGroup < group.size()) group.remove(startInGroup);
            if (flatStart < flatLayouts.size()) flatLayouts.remove(flatStart);
        }

        int inserted = changedLayouts == null ? 0 : changedLayouts.size();
        if (inserted > 0) {
            group.addAll(startInGroup, changedLayouts);
            flatLayouts.addAll(flatStart, changedLayouts);
        }

        int newSize = gSize - removedCount + inserted;

        /* ================= EMPTY GROUP CLEANUP ================= */

        if (newSize == 0) {
            int groupIndex = orderedGroupKeys.indexOf(groupKey);

            orderedGroupKeys.remove(groupIndex);
            groupLayouts.remove(groupKey);
            groupStart.remove(groupKey);
            groupSize.remove(groupKey);
            groupOrder.remove(groupKey);

            for (int i = groupIndex; i < orderedGroupKeys.size(); i++) {
                String k = orderedGroupKeys.get(i);
                Integer s = groupStart.get(k);
                if (s != null) groupStart.put(k, s - gSize);
            }

            normalizeTimeLabels(gStart - 1, gStart + 1);
            trackChange(gStart, gSize, 0, gStart - 1, gStart + 1);
            return;
        }

        /* ================= SIZE DELTA ================= */

        int delta = inserted - removedCount;
        if (delta != 0) {
            groupSize.put(groupKey, newSize);
            for (Map.Entry<String, Integer> e : groupStart.entrySet()) {
                Integer s = e.getValue();
                if (s != null && s > gStart) e.setValue(s + delta);
            }
        }

        int normFrom = flatStart - 1;
        int normTo   = flatStart + inserted + 1;

        normalizeTimeLabels(normFrom, normTo);
        trackChange(flatStart, removedCount, inserted, normFrom, normTo);
    }

    /* ================= replace ================= */

    public void onLayoutReplaced(String groupKey, int indexInGroup, BandLayout newLayout) {
        if (groupKey == null || newLayout == null) return;

        Integer gStart = groupStart.get(groupKey);
        List<BandLayout> group = groupLayouts.get(groupKey);
        if (gStart == null || group == null) return;

        int flatIndex = gStart + indexInGroup;

        group.set(indexInGroup, newLayout);
        flatLayouts.set(flatIndex, newLayout);

        normalizeTimeLabels(flatIndex, flatIndex + 2);
        trackChange(flatIndex, 0, 0, flatIndex, flatIndex + 2);
    }

    /* ================= adapter ================= */

    @Override
    public int getItemCount() {
        return flatLayouts.size();
    }

    @NonNull
    @Override
    public BandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return BandViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull BandViewHolder holder, int position) {
        holder.bind(flatLayouts.get(position));
    }
}
