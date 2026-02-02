package com.github.jaykkumar01.vaultspace.album.view;

import android.util.Log;
import com.github.jaykkumar01.vaultspace.album.band.Band;
import java.util.List;

public final class BandDiffHelper {

    private static final String TAG = "BandDiff";

    public BandDiff diff(List<Band> oldBands, List<Band> newBands) {
        int oldSize = oldBands.size(), newSize = newBands.size();

        if (oldSize == 0) {
            Log.d(TAG, "FULL INSERT bands=" + newSize);
            return new BandDiff(0, 0, newBands);
        }

        if (newSize == 0) {
            Log.d(TAG, "FULL REMOVE bands=" + oldSize);
            return new BandDiff(0, oldSize, List.of());
        }

        int sameStart = 0, max = Math.min(oldSize, newSize);
        while (sameStart < max && oldBands.get(sameStart).equals(newBands.get(sameStart)))
            sameStart++;

        int sameEnd = 0, oi = oldSize - 1, ni = newSize - 1;
        while (oi >= sameStart && ni >= sameStart &&
                oldBands.get(oi).equals(newBands.get(ni))) {
            sameEnd++; oi--; ni--;
        }

        if (sameStart == 0 && sameEnd == 0) {
            Log.d(TAG, "FULL REPLACE old=" + oldSize + " new=" + newSize);
            return new BandDiff(0, oldSize, newBands);
        }

        int removeCount = oldSize - sameStart - sameEnd;
        int insertFrom = sameStart, insertTo = newSize - sameEnd;

        Log.d(TAG,
                "PARTIAL start=" + sameStart +
                        " remove=" + removeCount +
                        " insert=" + (insertTo - insertFrom)
        );

        return new BandDiff(
                sameStart,
                removeCount,
                newBands.subList(insertFrom, insertTo)
        );
    }
}
