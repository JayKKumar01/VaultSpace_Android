package com.github.jaykkumar01.vaultspace.core.session;

import android.util.Log;

import com.github.jaykkumar01.vaultspace.album.AlbumItem;
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

    /* ---------------- Album items cache ---------------- */

    private final Map<String, List<AlbumItem>> albumItemsByAlbumId = new HashMap<>();

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
        albumItemsByAlbumId.remove(albumId);

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

    /* ---------------- Album items cache ---------------- */

    public boolean hasAlbumItemsCached(String albumId) {
        return albumId != null && albumItemsByAlbumId.containsKey(albumId);
    }

    public List<AlbumItem> getAlbumItems(String albumId) {
        List<AlbumItem> items = albumItemsByAlbumId.get(albumId);
        return items != null ? items : Collections.emptyList();
    }

    public void setAlbumItems(String albumId, List<AlbumItem> items) {
        if (albumId == null) return;
        albumItemsByAlbumId.put(albumId, items != null ? new ArrayList<>(items) : new ArrayList<>());
        Log.d(TAG, "Album items cached: " + albumId + " (" + getAlbumItems(albumId).size() + ")");
    }

    public void addAlbumItem(String albumId, AlbumItem item) {
        if (albumId == null || item == null) return;
        List<AlbumItem> items = albumItemsByAlbumId.computeIfAbsent(albumId, k -> new ArrayList<>());
        items.add(0, item);
        Log.d(TAG, "Album item added: " + item.fileId);
    }

    public void removeAlbumItem(String albumId, String fileId) {
        if (albumId == null || fileId == null) return;
        List<AlbumItem> items = albumItemsByAlbumId.get(albumId);
        if (items == null) return;

        items.removeIf(i -> fileId.equals(i.fileId));
        Log.d(TAG, "Album item removed: " + fileId);
    }

    /* ---------------- Lifecycle ---------------- */

    public void invalidateAlbums() {
        albumsCached = false;
        albums = Collections.emptyList();
        albumsById.clear();
        albumNames.clear();
        albumItemsByAlbumId.clear();
        Log.d(TAG, "Albums cache invalidated");
    }

    public void invalidateAlbumItems(String albumId) {
        if (albumId == null) return;
        albumItemsByAlbumId.remove(albumId);
        Log.d(TAG, "Album items invalidated: " + albumId);
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
