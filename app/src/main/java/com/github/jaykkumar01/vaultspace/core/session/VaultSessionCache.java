package com.github.jaykkumar01.vaultspace.core.session;

import android.util.Log;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VaultSessionCache {

    private static final String TAG = "VaultSpace:SessionCache";

    private boolean albumsCached = false;
    private List<AlbumInfo> cachedAlbums = Collections.emptyList();

    public boolean hasAlbumListCached() {
        return albumsCached;
    }

    public List<AlbumInfo> getAlbums() {
        return albumsCached ? cachedAlbums : Collections.emptyList();
    }

    public void setAlbums(List<AlbumInfo> albums) {
        cachedAlbums = albums == null ? Collections.emptyList() : new ArrayList<>(albums);
        albumsCached = true;
        Log.d(TAG, "Albums cached: " + cachedAlbums.size());
    }

    /** Optimistic update on album creation */
    public void addAlbum(AlbumInfo album) {
        if (!albumsCached) {
            cachedAlbums = new ArrayList<>();
            albumsCached = true;
        } else if (!(cachedAlbums instanceof ArrayList)) {
            cachedAlbums = new ArrayList<>(cachedAlbums);
        }
        cachedAlbums.add(0, album); // newest first
        Log.d(TAG, "Album added to cache: " + album.name);
    }

    /** Rare escape hatch */
    public void invalidateAlbums() {
        albumsCached = false;
        cachedAlbums = Collections.emptyList();
        Log.d(TAG, "Albums cache invalidated");
    }

    public void clear() {
        invalidateAlbums();
        Log.d(TAG, "Session cache cleared");
    }
}
