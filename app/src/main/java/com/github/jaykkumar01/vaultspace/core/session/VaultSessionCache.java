package com.github.jaykkumar01.vaultspace.core.session;

import android.util.Log;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VaultSessionCache {

    private static final String TAG = "VaultSpace:SessionCache";

    /* ---------------- Albums list cache ---------------- */

    private boolean albumsCached = false;
    private List<AlbumInfo> albums = Collections.emptyList();
    private final Map<String, AlbumInfo> albumsById = new HashMap<>();
    private final Set<String> albumNames = new HashSet<>();

    /* ---------------- Album media cache ---------------- */

    private final Map<String, List<AlbumMedia>> albumMediaByAlbumId = new HashMap<>();

    public boolean hasAlbumListCached() {
        return albumsCached;
    }

    public List<AlbumInfo> getAlbums() {
        return albumsCached ? albums : Collections.emptyList();
    }

    /* ---------------- Albums set ---------------- */

    public void setAlbums(List<AlbumInfo> list) {
        albums = new ArrayList<>();
        albumsById.clear();
        albumNames.clear();

        if (list != null) {
            for (AlbumInfo a : list) {
                albums.add(a);
                albumsById.put(a.id, a);
                albumNames.add(a.name);
            }
        }

        albumsCached = true;
        Log.d(TAG, "Albums cached: " + albums.size());
    }

    /* ---------------- Album queries (O(1)) ---------------- */

    public boolean hasAlbumWithName(String name) {
        return albumsCached && name != null && albumNames.contains(name);
    }

    /* ---------------- Album mutations ---------------- */

    public void addAlbum(AlbumInfo album) {
        if (album == null) return;
        ensureAlbumsMutable();

        albums.add(0, album);
        albumsById.put(album.id, album);
        albumNames.add(album.name);

        Log.d(TAG, "Album added to cache: " + album.name);
    }

    public void removeAlbum(String albumId) {
        if (!albumsCached || albumId == null) return;
        ensureAlbumsMutable();

        AlbumInfo removed = albumsById.remove(albumId);
        if (removed == null) return;

        albumNames.remove(removed.name);
        albums.removeIf(a -> albumId.equals(a.id));
        albumMediaByAlbumId.remove(albumId);

        Log.d(TAG, "Album removed from cache: " + albumId);
    }

    public void replaceAlbum(AlbumInfo updated) {
        if (!albumsCached || updated == null) return;
        ensureAlbumsMutable();

        AlbumInfo old = albumsById.get(updated.id);
        if (old == null) return;

        albumsById.put(updated.id, updated);
        albumNames.remove(old.name);
        albumNames.add(updated.name);

        for (int i = 0; i < albums.size(); i++) {
            if (updated.id.equals(albums.get(i).id)) {
                albums.set(i, updated);
                break;
            }
        }

        Log.d(TAG, "Album replaced in cache: " + updated.id);
    }

    /* ---------------- Album media cache ---------------- */

    public boolean hasAlbumMediaCached(String albumId) {
        return albumId != null && albumMediaByAlbumId.containsKey(albumId);
    }

    public List<AlbumMedia> getAlbumMedia(String albumId) {
        List<AlbumMedia> media = albumMediaByAlbumId.get(albumId);
        return media != null ? media : Collections.emptyList();
    }

    public void setAlbumMedia(String albumId, List<AlbumMedia> media) {
        if (albumId == null) return;
        albumMediaByAlbumId.put(albumId, media != null ? new ArrayList<>(media) : new ArrayList<>());
        Log.d(TAG, "Album media cached: " + albumId + " (" + getAlbumMedia(albumId).size() + ")");
    }

    public void addAlbumMedia(String albumId, AlbumMedia media) {
        if (albumId == null || media == null) return;
        List<AlbumMedia> list = albumMediaByAlbumId.computeIfAbsent(albumId, k -> new ArrayList<>());
        list.add(0, media);
        Log.d(TAG, "Album media added: " + media.fileId);
    }

    public void removeAlbumMedia(String albumId, String fileId) {
        if (albumId == null || fileId == null) return;
        List<AlbumMedia> media = albumMediaByAlbumId.get(albumId);
        if (media == null) return;

        media.removeIf(m -> fileId.equals(m.fileId));
        Log.d(TAG, "Album media removed: " + fileId);
    }

    /* ---------------- Lifecycle ---------------- */

    public void invalidateAlbums() {
        albumsCached = false;
        albums = Collections.emptyList();
        albumsById.clear();
        albumNames.clear();
        albumMediaByAlbumId.clear();
        Log.d(TAG, "Albums cache invalidated");
    }

    public void invalidateAlbumMedia(String albumId) {
        if (albumId == null) return;
        albumMediaByAlbumId.remove(albumId);
        Log.d(TAG, "Album media invalidated: " + albumId);
    }

    public void clear() {
        invalidateAlbums();
        Log.d(TAG, "Session cache cleared");
    }

    /* ---------------- Utils ---------------- */

    private void ensureAlbumsMutable() {
        if (!(albums instanceof ArrayList)) {
            albums = new ArrayList<>(albums);
        }
    }
}
