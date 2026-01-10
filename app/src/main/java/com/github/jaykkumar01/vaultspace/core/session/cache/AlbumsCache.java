package com.github.jaykkumar01.vaultspace.core.session.cache;

import android.util.Log;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.*;

public final class AlbumsCache {

    private static final String TAG = "VaultSpace:AlbumsCache";

    private boolean albumsCached = false;

    private List<AlbumInfo> albums = Collections.emptyList();
    private final Map<String, AlbumInfo> albumsById = new HashMap<>();
    private final Set<String> albumNames = new HashSet<>();

    /* ---------------- State ---------------- */

    public boolean isCached() {
        return albumsCached;
    }

    public List<AlbumInfo> getAlbums() {
        return albumsCached ? albums : Collections.emptyList();
    }

    /* ---------------- Set ---------------- */

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

    /* ---------------- Queries ---------------- */

    public boolean hasAlbumWithName(String name) {
        return albumsCached && name != null && albumNames.contains(name);
    }

    /* ---------------- Mutations ---------------- */

    public void addAlbum(AlbumInfo album) {
        if (album == null) return;
        ensureMutable();

        albums.add(0, album);
        albumsById.put(album.id, album);
        albumNames.add(album.name);

        Log.d(TAG, "Album added: " + album.name);
    }

    public void removeAlbum(String albumId) {
        if (!albumsCached || albumId == null) return;
        ensureMutable();

        AlbumInfo removed = albumsById.remove(albumId);
        if (removed == null) return;

        albumNames.remove(removed.name);
        albums.removeIf(a -> albumId.equals(a.id));

        Log.d(TAG, "Album removed: " + albumId);
    }

    public void replaceAlbum(AlbumInfo updated) {
        if (!albumsCached || updated == null) return;
        ensureMutable();

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

        Log.d(TAG, "Album replaced: " + updated.id);
    }

    /* ---------------- Lifecycle ---------------- */

    public void clear() {
        albumsCached = false;
        albums = Collections.emptyList();
        albumsById.clear();
        albumNames.clear();
        Log.d(TAG, "Albums cache cleared");
    }

    /* ---------------- Utils ---------------- */

    private void ensureMutable() {
        if (!(albums instanceof ArrayList)) {
            albums = new ArrayList<>(albums);
        }
    }
}
