package com.github.jaykkumar01.vaultspace.core.session.cache;

import android.util.Log;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;

import java.util.*;

public final class AlbumMediaCache {

    private static final String TAG = "VaultSpace:AlbumMediaCache";

    private final Map<String, List<AlbumMedia>> mediaByAlbumId = new HashMap<>();

    /* ---------------- Queries ---------------- */

    public boolean hasAlbumMediaCached(String albumId) {
        return albumId != null && mediaByAlbumId.containsKey(albumId);
    }

    public List<AlbumMedia> getAlbumMedia(String albumId) {
        List<AlbumMedia> media = mediaByAlbumId.get(albumId);
        return media != null ? media : Collections.emptyList();
    }

    /* ---------------- Set ---------------- */

    public void setAlbumMedia(String albumId, List<AlbumMedia> media) {
        if (albumId == null) return;
        mediaByAlbumId.put(
                albumId,
                media != null ? new ArrayList<>(media) : new ArrayList<>()
        );
        Log.d(TAG, "Album media cached: " + albumId);
    }

    /* ---------------- Mutations ---------------- */

    public void addAlbumMedia(String albumId, AlbumMedia media) {
        if (albumId == null || media == null) return;
        List<AlbumMedia> list =
                mediaByAlbumId.computeIfAbsent(albumId, k -> new ArrayList<>());
        list.add(0, media);
        Log.d(TAG, "Album media added: " + media.fileId);
    }

    public void removeAlbumMedia(String albumId, String fileId) {
        if (albumId == null || fileId == null) return;
        List<AlbumMedia> media = mediaByAlbumId.get(albumId);
        if (media == null) return;

        media.removeIf(m -> fileId.equals(m.fileId));
        Log.d(TAG, "Album media removed: " + fileId);
    }

    /* ---------------- Lifecycle ---------------- */

    public void invalidateAlbumMedia(String albumId) {
        if (albumId == null) return;
        mediaByAlbumId.remove(albumId);
        Log.d(TAG, "Album media invalidated: " + albumId);
    }

    public void clear() {
        mediaByAlbumId.clear();
        Log.d(TAG, "Album media cache cleared");
    }
}
