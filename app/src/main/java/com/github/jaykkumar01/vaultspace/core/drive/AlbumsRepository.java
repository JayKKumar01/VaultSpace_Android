package com.github.jaykkumar01.vaultspace.core.drive;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.AlbumsCache;
import com.github.jaykkumar01.vaultspace.dashboard.albums.helper.AlbumsDriveHelper;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AlbumsRepository {

    /* ================= Singleton ================= */

    private static volatile AlbumsRepository INSTANCE;

    public static AlbumsRepository getInstance(Context c) {
        if (INSTANCE == null) {
            synchronized (AlbumsRepository.class) {
                if (INSTANCE == null) INSTANCE = new AlbumsRepository(c.getApplicationContext());
            }
        }
        return INSTANCE;
    }

    /* ================= APIs ================= */

    public interface AlbumsListener {
        void onAlbumsLoaded(Iterable<AlbumInfo> albums);
        void onAlbumAdded(AlbumInfo album);
        void onAlbumRemoved(String albumId);
        void onAlbumUpdated(AlbumInfo album);
    }

    public interface Failure { void call(Exception e); }

    /* ================= Core ================= */

    private final AlbumsCache cache;
    private final AlbumsDriveHelper drive;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Object lock = new Object();
    private final Set<AlbumsListener> listeners = new HashSet<>();

    private AlbumsRepository(Context appContext) {
        cache = new UserSession(appContext).getVaultCache().albums;
        drive = new AlbumsDriveHelper(appContext);
    }

    /* ================= Load ================= */

    public void load(Failure err) {
        synchronized (lock) {
            if (cache.isInitialized()) {
                notifyLoaded();
                return;
            }
        }

        drive.fetchAlbums(executor,
                albums -> {
                    synchronized (lock) {
                        cache.initializeFromDrive(albums);
                    }
                    notifyLoaded();
                },
                e -> mainHandler.post(() -> err.call(e))
        );
    }

    /* ================= Reads ================= */

    public List<AlbumInfo> getAlbumsSnapshot() {
        List<AlbumInfo> out = new ArrayList<>();
        synchronized (lock) {
            if (!cache.isInitialized()) return out;
            for (AlbumInfo a : cache.getAlbumsView()) out.add(a);
        }
        return out;
    }

    /* ================= Mutations ================= */

    public void createAlbum(String name, Failure err) {
        long now = System.currentTimeMillis();
        AlbumInfo temp = new AlbumInfo("temp_" + now, name, now, now, null);

        synchronized (lock) { cache.addAlbum(temp); }
        notifyAdded(temp);

        drive.createAlbum(executor, name,
                real -> {
                    synchronized (lock) {
                        cache.removeAlbum(temp.id);
                        cache.addAlbum(real);
                    }
                    notifyRemoved(temp.id);
                    notifyAdded(real);
                },
                e -> {
                    synchronized (lock) { cache.removeAlbum(temp.id); }
                    notifyRemoved(temp.id);
                    mainHandler.post(() -> err.call(e));
                }
        );
    }

    public void renameAlbum(AlbumInfo old, String newName, Failure err) {
        AlbumInfo updated = new AlbumInfo(
                old.id, newName, old.createdTime, System.currentTimeMillis(), old.coverPath
        );

        synchronized (lock) { cache.replaceAlbum(updated); }
        notifyUpdated(updated);

        drive.renameAlbum(executor, old.id, newName,
                () -> {},
                e -> {
                    synchronized (lock) { cache.replaceAlbum(old); }
                    notifyUpdated(old);
                    mainHandler.post(() -> err.call(e));
                }
        );
    }

    public void deleteAlbum(AlbumInfo album, Failure err) {
        if (album == null) return;

        synchronized (lock) { cache.removeAlbum(album.id); }
        notifyRemoved(album.id);

        mainHandler.postDelayed(() -> {
            synchronized (lock) { cache.addAlbum(album); }
            notifyAdded(album);
            if (err != null) err.call(new Exception("Delete failed (simulated)"));
        }, 2000);
    }



    public void setAlbumCover(String albumId,String coverFileId,Failure err) {
        AlbumInfo old;
        synchronized (lock) { old = cache.getAlbumById(albumId); }
        if (old == null) return;

        drive.setAlbumCover(executor, albumId, coverFileId,
                path -> {
                    AlbumInfo updated = new AlbumInfo(
                            old.id, old.name, old.createdTime,
                            System.currentTimeMillis(), path
                    );
                    synchronized (lock) { cache.replaceAlbum(updated); }
                    notifyUpdated(updated);
                },
                e -> mainHandler.post(() -> err.call(e))
        );
    }


    public void clearAlbumCover(String albumId,String coverFileId,Failure err) {
        AlbumInfo old;
        synchronized (lock) { old = cache.getAlbumById(albumId); }
        if (old == null) return;

        drive.clearAlbumCover(executor, albumId, coverFileId,
                () -> {
                    AlbumInfo updated = new AlbumInfo(
                            old.id, old.name, old.createdTime,
                            System.currentTimeMillis(), null
                    );
                    synchronized (lock) { cache.replaceAlbum(updated); }
                    notifyUpdated(updated);
                },
                e -> mainHandler.post(() -> err.call(e))
        );
    }


    /* ================= Listeners ================= */

    public void addListener(AlbumsListener l) {
        if (l == null) return;
        synchronized (lock) { listeners.add(l); }
    }

    public void removeListener(AlbumsListener l) {
        if (l == null) return;
        synchronized (lock) { listeners.remove(l); }
    }

    /* ================= Notify helpers ================= */

    private void notifyLoaded() {
        Iterable<AlbumInfo> snapshot;
        synchronized (lock) { snapshot = cache.getAlbumsView(); }
        mainHandler.post(() -> {
            for (AlbumsListener l : listeners) l.onAlbumsLoaded(snapshot);
        });
    }

    private void notifyAdded(AlbumInfo a) {
        mainHandler.post(() -> {
            for (AlbumsListener l : listeners) l.onAlbumAdded(a);
        });
    }

    private void notifyRemoved(String albumId) {
        mainHandler.post(() -> {
            for (AlbumsListener l : listeners) l.onAlbumRemoved(albumId);
        });
    }

    private void notifyUpdated(AlbumInfo a) {
        mainHandler.post(() -> {
            for (AlbumsListener l : listeners) l.onAlbumUpdated(a);
        });
    }
}
