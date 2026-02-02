package com.github.jaykkumar01.vaultspace.album.view;

import android.util.Log;

import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;

import java.util.List;

public final class LayoutDiffHelper {

    private static final String TAG = "LayoutDiff";

    public LayoutResult diff(
            int groupLayoutStart,
            List<BandLayout> oldLayouts,
            List<BandLayout> newLayouts
    ) {
        int oldSize = oldLayouts.size();
        int newSize = newLayouts.size();

        if (oldSize == 0) {
            Log.d(TAG, "FULL INSERT @ " + groupLayoutStart + " count=" + newSize);
            return LayoutResult.replaceRange(groupLayoutStart, 0, newLayouts);
        }

        if (newSize == 0) {
            Log.d(TAG, "FULL REMOVE @ " + groupLayoutStart + " count=" + oldSize);
            return LayoutResult.replaceRange(groupLayoutStart, oldSize, List.of());
        }

        int sameStart = 0;
        int max = Math.min(oldSize, newSize);
        while (sameStart < max &&
                oldLayouts.get(sameStart).equals(newLayouts.get(sameStart)))
            sameStart++;

        int sameEnd = 0;
        int oi = oldSize - 1;
        int ni = newSize - 1;
        while (oi >= sameStart && ni >= sameStart &&
                oldLayouts.get(oi).equals(newLayouts.get(ni))) {
            sameEnd++;
            oi--;
            ni--;
        }

        if (sameStart == 0 && sameEnd == 0) {
            Log.d(TAG, "FULL REPLACE @ " + groupLayoutStart +
                    " old=" + oldSize + " new=" + newSize);
            return LayoutResult.replaceRange(groupLayoutStart, oldSize, newLayouts);
        }

        int removeCount = oldSize - sameStart - sameEnd;
        int insertFrom = sameStart;
        int insertTo = newSize - sameEnd;

        Log.d(TAG,
                "PARTIAL @" + (groupLayoutStart + sameStart) +
                        " sameStart=" + sameStart +
                        " sameEnd=" + sameEnd +
                        " remove=" + removeCount +
                        " insert=" + (insertTo - insertFrom)
        );

        return LayoutResult.replaceRange(
                groupLayoutStart + sameStart,
                removeCount,
                newLayouts.subList(insertFrom, insertTo)
        );
    }
}
