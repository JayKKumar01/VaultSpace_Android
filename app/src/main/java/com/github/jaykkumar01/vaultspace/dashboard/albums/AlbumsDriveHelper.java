package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.VaultSessionCache;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AlbumsDriveHelper {

    private static final String TAG="VaultSpace:AlbumsDrive";
    private static final long OP_TIMEOUT_MS=10_000;

    private final Drive primaryDrive;
    private final VaultSessionCache cache;
    private final Handler mainHandler=new Handler(Looper.getMainLooper());

    public interface FetchCallback{
        void onResult(List<AlbumInfo> albums);
        void onError(Exception e);
    }

    public interface CreateAlbumCallback{
        void onSuccess(AlbumInfo album);
        void onError(Exception e);
    }

    public interface DeleteAlbumCallback{
        void onSuccess(String albumId);
        void onError(Exception e);
    }

    public interface RenameAlbumCallback{
        void onSuccess();
        void onError(Exception e);
    }

    public AlbumsDriveHelper(Context context){
        UserSession session=new UserSession(context);
        cache=session.getVaultCache();
        String email=session.getPrimaryAccountEmail();
        primaryDrive=DriveClientProvider.forAccount(context,email);
        Log.d(TAG,"Initialized primaryDrive for "+email);
    }

    public void fetchAlbums(ExecutorService executor,FetchCallback callback){
        executor.execute(()->{
            try{
                if (cache.albums.isCached()) {
                    postResult(callback, cache.albums.get());
                    return;
                }


                String rootId=DriveFolderRepository.findAlbumsRootId(primaryDrive);
                if(rootId==null){
                    cache.albums.set(List.of());
                    postResult(callback, List.of());
                    return;
                }

                String q="'"+rootId+"' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";

                FileList list=primaryDrive.files().list()
                        .setQ(q)
                        .setFields("files(id,name,createdTime,modifiedTime)")
                        .setOrderBy("modifiedTime desc")
                        .execute();

                List<AlbumInfo> albums=getAlbums(list);
                cache.albums.set(albums);
                postResult(callback, albums);


            }catch(Exception e){
                Log.e(TAG,"Failed to fetch albums",e);
                postError(callback,e);
            }
        });
    }

    public void createAlbum(ExecutorService executor,String albumName,CreateAlbumCallback callback){
        String trimmed=albumName==null?"":albumName.trim();
        if(trimmed.isEmpty()){
            callback.onError(new IllegalArgumentException("Album name is empty"));
            return;
        }

        if (cache.albums.hasAlbumWithName(trimmed)) {
            callback.onError(new IllegalStateException("Album already exists"));
            return;
        }

        AtomicBoolean completed=new AtomicBoolean(false);
        Runnable timeout=()->{
            if(completed.compareAndSet(false,true)){
                Log.w(TAG,"Create album timed out");
                callback.onError(new TimeoutException("Album creation timed out"));
            }
        };
        mainHandler.postDelayed(timeout,OP_TIMEOUT_MS);

        executor.execute(()->{
            try{
                String rootId=DriveFolderRepository.getOrCreateAlbumsRootId(primaryDrive);
                File created=DriveFolderRepository.createFolder(primaryDrive,albumName,rootId);

                AlbumInfo album=new AlbumInfo(
                        created.getId(),
                        created.getName(),
                        created.getCreatedTime().getValue(),
                        created.getModifiedTime().getValue(),
                        null
                );

                if(completed.compareAndSet(false,true)){
                    mainHandler.removeCallbacks(timeout);
                    cache.albums.addAlbum(album);
                    mainHandler.post(()->callback.onSuccess(album));
                }
            }catch(Exception e){
                if(completed.compareAndSet(false,true)){
                    mainHandler.removeCallbacks(timeout);
                    Log.e(TAG,"Failed to create album",e);
                    mainHandler.post(()->callback.onError(e));
                }
            }
        });
    }

    public void renameAlbum(ExecutorService executor,String albumId,String newName,RenameAlbumCallback callback){
        AtomicBoolean completed=new AtomicBoolean(false);

        Runnable timeout=()->{
            if(completed.compareAndSet(false,true)){
                Log.w(TAG,"Rename album timed out");
                callback.onError(new TimeoutException("Album rename timed out"));
            }
        };

        mainHandler.postDelayed(timeout,OP_TIMEOUT_MS);

        executor.execute(()->{
            try{
                File update=new File();
                update.setName(newName);

                File updated=primaryDrive.files()
                        .update(albumId,update)
                        .setFields("id,name,createdTime,modifiedTime")
                        .execute();

                AlbumInfo replaced=new AlbumInfo(
                        updated.getId(),
                        updated.getName(),
                        updated.getCreatedTime().getValue(),
                        updated.getModifiedTime().getValue(),
                        null
                );

                if(completed.compareAndSet(false,true)){
                    mainHandler.removeCallbacks(timeout);
                    cache.albums.replaceAlbum(replaced);
                    mainHandler.post(callback::onSuccess);
                }

            }catch(Exception e){
                if(completed.compareAndSet(false,true)){
                    mainHandler.removeCallbacks(timeout);
                    Log.e(TAG,"Failed to rename album "+albumId,e);
                    mainHandler.post(()->callback.onError(e));
                }
            }
        });
    }

    public void deleteAlbum(ExecutorService executor,String albumId,DeleteAlbumCallback callback){
        AtomicBoolean completed=new AtomicBoolean(false);
        Runnable timeout=()->{
            if(completed.compareAndSet(false,true)){
                Log.w(TAG,"Delete album timed out");
                callback.onError(new TimeoutException("Album deletion timed out"));
            }
        };
        mainHandler.postDelayed(timeout,OP_TIMEOUT_MS);

        executor.execute(()->{
            try{
                primaryDrive.files().delete(albumId).execute();
                if(completed.compareAndSet(false,true)){
                    mainHandler.removeCallbacks(timeout);
                    cache.albums.removeAlbum(albumId);
                    Log.d(TAG,"Deleted album "+albumId);
                    mainHandler.post(()->callback.onSuccess(albumId));
                }
            }catch(Exception e){
                if(completed.compareAndSet(false,true)){
                    mainHandler.removeCallbacks(timeout);
                    Log.e(TAG,"Failed to delete album "+albumId,e);
                    mainHandler.post(()->callback.onError(e));
                }
            }
        });
    }

    public void invalidateCache() {
        cache.albums.clear();
    }


    private static List<AlbumInfo> getAlbums(FileList list){
        List<AlbumInfo> albums=new ArrayList<>();
        if(list.getFiles()!=null){
            for(File f:list.getFiles()){
                albums.add(new AlbumInfo(
                        f.getId(),
                        f.getName(),
                        f.getCreatedTime().getValue(),
                        f.getModifiedTime().getValue(),
                        null
                ));
            }
        }
        return albums;
    }

    private void postResult(FetchCallback cb,List<AlbumInfo> albums){
        mainHandler.post(()->cb.onResult(albums));
    }

    private void postError(FetchCallback cb,Exception e){
        mainHandler.post(()->cb.onError(e));
    }
}
