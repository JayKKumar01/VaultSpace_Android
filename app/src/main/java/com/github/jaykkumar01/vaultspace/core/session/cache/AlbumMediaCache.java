package com.github.jaykkumar01.vaultspace.core.session.cache;

import android.util.Log;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;

import java.util.*;

public final class AlbumMediaCache
        extends VaultCache<Map<String, List<AlbumMedia>>> {

    private static final String TAG = "VaultSpace:AlbumMediaCache";

    private final Map<String, List<AlbumMedia>> mediaByAlbumId = new HashMap<>();

    /* ================= VaultCache hooks ================= */

    @Override
    protected Map<String, List<AlbumMedia>> getInternal() {
        return mediaByAlbumId;
    }

    @Override
    protected Map<String, List<AlbumMedia>> getEmpty() {
        return Collections.emptyMap();
    }

    @Override
    protected void setInternal(Map<String, List<AlbumMedia>> data) {
        mediaByAlbumId.clear();
        if (data != null) {
            mediaByAlbumId.putAll(data);
        }
    }

    @Override
    protected void clearInternal() {
        mediaByAlbumId.clear();
        Log.d(TAG, "Album media cache cleared");
    }

    /* ================= Domain API ================= */

    public boolean hasAlbumMediaCached(String albumId) {
        return isCached() && albumId != null && mediaByAlbumId.containsKey(albumId);
    }

    public List<AlbumMedia> getAlbumMedia(String albumId) {
        List<AlbumMedia> media = mediaByAlbumId.get(albumId);
        return media != null ? media : Collections.emptyList();
    }

    public void setAlbumMedia(String albumId, List<AlbumMedia> media) {
        if (!isCached() || albumId == null) return;
        mediaByAlbumId.put(
                albumId,
                media != null ? new ArrayList<>(media) : new ArrayList<>()
        );
        Log.d(TAG, "Album media cached: " + albumId);
    }

    public void addAlbumMedia(String albumId, AlbumMedia media) {
        if (!isCached() || albumId == null || media == null) return;

        List<AlbumMedia> list =
                mediaByAlbumId.computeIfAbsent(albumId, k -> new ArrayList<>());
        list.add(0, media);

        Log.d(TAG, "Album media added: " + media.fileId);
    }

    public void removeAlbumMedia(String albumId, String fileId) {
        if (!isCached() || albumId == null || fileId == null) return;

        List<AlbumMedia> media = mediaByAlbumId.get(albumId);
        if (media == null) return;

        media.removeIf(m -> fileId.equals(m.fileId));
        Log.d(TAG, "Album media removed: " + fileId);
    }

    public void invalidateAlbumMedia(String albumId) {
        if (albumId == null) return;
        mediaByAlbumId.remove(albumId);
        Log.d(TAG, "Album media invalidated: " + albumId);
    }
}
