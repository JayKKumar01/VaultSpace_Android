package com.github.jaykkumar01.vaultspace.core.session.cache;

import android.util.Log;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.*;

public final class AlbumsCache extends VaultCache<List<AlbumInfo>> {

    private static final String TAG = "VaultSpace:AlbumsCache";

    private List<AlbumInfo> albums = new ArrayList<>();
    private final Map<String, AlbumInfo> albumsById = new HashMap<>();
    private final Set<String> albumNames = new HashSet<>();

    /* ================= VaultCache hooks ================= */

    @Override
    protected List<AlbumInfo> getInternal() {
        return albums;
    }

    @Override
    protected List<AlbumInfo> getEmpty() {
        return Collections.emptyList();
    }

    @Override
    protected void setInternal(List<AlbumInfo> list) {
        albums.clear();
        albumsById.clear();
        albumNames.clear();

        if (list != null) {
            for (AlbumInfo a : list) {
                albums.add(a);
                albumsById.put(a.id, a);
                albumNames.add(a.name);
            }
        }

        Log.d(TAG, "Albums cached: " + albums.size());
    }

    @Override
    protected void clearInternal() {
        albums.clear();
        albumsById.clear();
        albumNames.clear();
        Log.d(TAG, "Albums cache cleared");
    }

    /* ================= Domain API ================= */

    public boolean hasAlbumWithName(String name) {
        return isCached() && name != null && albumNames.contains(name);
    }

    public void addAlbum(AlbumInfo album) {
        if (!isCached() || album == null) return;

        albums.add(0, album);
        albumsById.put(album.id, album);
        albumNames.add(album.name);

        Log.d(TAG, "Album added: " + album.name);
    }

    public void removeAlbum(String albumId) {
        if (!isCached() || albumId == null) return;

        AlbumInfo removed = albumsById.remove(albumId);
        if (removed == null) return;

        albumNames.remove(removed.name);
        albums.removeIf(a -> albumId.equals(a.id));

        Log.d(TAG, "Album removed: " + albumId);
    }

    public void replaceAlbum(AlbumInfo updated) {
        if (!isCached() || updated == null) return;

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
}
