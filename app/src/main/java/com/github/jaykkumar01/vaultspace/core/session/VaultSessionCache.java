package com.github.jaykkumar01.vaultspace.core.session;

import android.util.Log;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VaultSessionCache {

    private static final String TAG = "VaultSpace:SessionCache";

    /* ---------------- Albums cache ---------------- */

    private boolean albumsCached = false;
    private List<AlbumInfo> cachedAlbums = Collections.emptyList();

    /* ---------------- Albums API ---------------- */

    public boolean hasAlbumListCached() {
        return albumsCached;
    }

    public List<AlbumInfo> getAlbums() {
        return albumsCached
                ? cachedAlbums
                : Collections.emptyList();
    }

    public void setAlbums(List<AlbumInfo> albums) {
        if (albums == null) {
            cachedAlbums = Collections.emptyList();
        } else {
            // Defensive copy to avoid external mutation
            cachedAlbums = new ArrayList<>(albums);
        }
        albumsCached = true;
        Log.d(TAG, "Albums cached: " + cachedAlbums.size());
    }

    public void invalidateAlbums() {
        albumsCached = false;
        cachedAlbums = Collections.emptyList();
        Log.d(TAG, "Albums cache invalidated");
    }

    /* ---------------- Full session clear ---------------- */

    public void clear() {
        invalidateAlbums();
        Log.d(TAG, "Session cache cleared");
    }
}
