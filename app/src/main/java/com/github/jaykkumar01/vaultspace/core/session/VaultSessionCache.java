package com.github.jaykkumar01.vaultspace.core.session;

import android.util.Log;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VaultSessionCache {

    private static final String TAG="VaultSpace:SessionCache";

    private boolean albumsCached=false;

    private List<AlbumInfo> albums=Collections.emptyList();
    private final Map<String,AlbumInfo> albumsById=new HashMap<>();
    private final Set<String> albumNames=new HashSet<>();

    public boolean hasAlbumListCached(){
        return albumsCached;
    }

    public List<AlbumInfo> getAlbums(){
        return albumsCached?albums:Collections.emptyList();
    }

    /* ---------------- Set ---------------- */

    public void setAlbums(List<AlbumInfo> list){
        albums=new ArrayList<>();
        albumsById.clear();
        albumNames.clear();

        if(list!=null){
            for(AlbumInfo a:list){
                albums.add(a);
                albumsById.put(a.id,a);
                albumNames.add(a.name);
            }
        }

        albumsCached=true;
        Log.d(TAG,"Albums cached: "+albums.size());
    }

    /* ---------------- Queries (O(1)) ---------------- */

    public boolean hasAlbumWithName(String name){
        return albumsCached&&name!=null&&albumNames.contains(name);
    }

    /* ---------------- Mutations ---------------- */

    public void addAlbum(AlbumInfo album){
        if(album==null) return;
        ensureMutable();

        albums.add(0,album);
        albumsById.put(album.id,album);
        albumNames.add(album.name);

        Log.d(TAG,"Album added to cache: "+album.name);
    }

    public void removeAlbum(String albumId){
        if(!albumsCached||albumId==null) return;
        ensureMutable();

        AlbumInfo removed=albumsById.remove(albumId);
        if(removed==null) return;

        albumNames.remove(removed.name);
        albums.removeIf(a->albumId.equals(a.id));

        Log.d(TAG,"Album removed from cache: "+albumId);
    }

    public void replaceAlbum(AlbumInfo updated){
        if(!albumsCached||updated==null) return;
        ensureMutable();

        AlbumInfo old=albumsById.get(updated.id);
        if(old==null) return;

        albumsById.put(updated.id,updated);
        albumNames.remove(old.name);
        albumNames.add(updated.name);

        for(int i=0;i<albums.size();i++){
            if(updated.id.equals(albums.get(i).id)){
                albums.set(i,updated);
                break;
            }
        }

        Log.d(TAG,"Album replaced in cache: "+updated.id);
    }

    /* ---------------- Lifecycle ---------------- */

    public void invalidateAlbums(){
        albumsCached=false;
        albums=Collections.emptyList();
        albumsById.clear();
        albumNames.clear();
        Log.d(TAG,"Albums cache invalidated");
    }

    public void clear(){
        invalidateAlbums();
        Log.d(TAG,"Session cache cleared");
    }

    /* ---------------- Utils ---------------- */

    private void ensureMutable(){
        if(!(albums instanceof ArrayList)){
            albums=new ArrayList<>(albums);
        }
    }
}
