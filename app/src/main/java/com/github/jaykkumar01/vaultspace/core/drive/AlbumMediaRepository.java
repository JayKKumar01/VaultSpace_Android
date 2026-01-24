package com.github.jaykkumar01.vaultspace.core.drive;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.helper.AlbumDriveHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.AlbumMediaCache;
import com.github.jaykkumar01.vaultspace.core.session.cache.AlbumMediaEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AlbumMediaRepository {

    /* ================= Singleton ================= */

    private static volatile AlbumMediaRepository INSTANCE;

    public static AlbumMediaRepository getInstance(Context c) {
        if (INSTANCE == null) {
            synchronized (AlbumMediaRepository.class) {
                if (INSTANCE == null) INSTANCE = new AlbumMediaRepository(c.getApplicationContext());
            }
        }
        return INSTANCE;
    }

    /* ================= APIs ================= */

    public interface Callback {
        void onLoaded();
        void onError(Exception e);
    }

    /** Bulk listener – used for initial load / refresh */
    public interface MediaListener {
        void onAlbumChanged(Iterable<AlbumMedia> media);
    }

    /** O(1) listener – used for AlbumMetaInfo */
    public interface CountListener {
        void onCountsChanged(int photos,int videos);
    }

    /* ================= Core ================= */

    private final AlbumMediaCache cache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Object lock = new Object();

    private final Map<String,Boolean> loading = new HashMap<>();
    private final Map<String,MediaListener> mediaListenerByAlbum = new HashMap<>();
    private final Map<String,CountListener> countListenerByAlbum = new HashMap<>();

    /** Derived state (owned by repo) */
    private final Map<String,Integer> photoCountByAlbum = new HashMap<>();
    private final Map<String,Integer> videoCountByAlbum = new HashMap<>();

    private AlbumMediaRepository(Context appContext) {
        cache = new UserSession(appContext).getVaultCache().albumMedia;
    }

    /* ================= Load / Init ================= */

    public void loadAlbum(Context c,String albumId,Callback cb) {
        AlbumMediaEntry entry = cache.getOrCreateEntry(albumId);

        synchronized (lock) {
            if (entry.isInitialized()) {
                notifyCounts(albumId);
                cb.onLoaded();
                return;
            }
            if (Boolean.TRUE.equals(loading.get(albumId))) return;
            loading.put(albumId,true);
        }

        AlbumDriveHelper drive = new AlbumDriveHelper(c,albumId);
        drive.fetchAlbumMedia(executor,new AlbumDriveHelper.FetchCallback() {

            @Override
            public void onResult(@NonNull List<AlbumMedia> items) {
                int photos = 0;
                int videos = 0;

                for (AlbumMedia m : items) {
                    if (m.isVideo) videos++;
                    else photos++;
                }

                synchronized (lock) {
                    entry.initializeFromDrive(items);
                    photoCountByAlbum.put(albumId,photos);
                    videoCountByAlbum.put(albumId,videos);
                    loading.remove(albumId);
                }

                notifyMedia(albumId);
                notifyCounts(albumId);
                cb.onLoaded();
            }

            @Override
            public void onError(@NonNull Exception e) {
                synchronized (lock) {
                    loading.remove(albumId);
                }
                cb.onError(e);
            }
        });
    }

    /* ================= Reads ================= */

    public List<AlbumMedia> getMediaSnapshot(String albumId) {
        AlbumMediaEntry entry = cache.getOrCreateEntry(albumId);
        List<AlbumMedia> out = new ArrayList<>();
        if (!entry.isInitialized()) return out;
        for (AlbumMedia m : entry.getMediaView()) out.add(m);
        return out;
    }

    /* ================= Mutations (O(1)) ================= */

    public void addMedia(String albumId,AlbumMedia media) {
        AlbumMediaEntry entry = cache.getOrCreateEntry(albumId);
        if (!entry.isInitialized()) return;

        entry.addMedia(media);

        synchronized (lock) {
            if (media.isVideo)
                videoCountByAlbum.compute(albumId,(k,v) -> v == null ? 1 : v + 1);
            else
                photoCountByAlbum.compute(albumId,(k,v) -> v == null ? 1 : v + 1);
        }


        notifyMedia(albumId);
        notifyCounts(albumId);
    }

    public void removeMedia(String albumId,AlbumMedia media) {
        AlbumMediaEntry entry = cache.getOrCreateEntry(albumId);
        if (!entry.isInitialized()) return;

        boolean wasVideo = media.isVideo;
        entry.removeMedia(media.fileId);

        synchronized (lock) {
            if (wasVideo)
                videoCountByAlbum.compute(albumId,(k,v) -> v == null || v <= 1 ? 0 : v - 1);
            else
                photoCountByAlbum.compute(albumId,(k,v) -> v == null || v <= 1 ? 0 : v - 1);
        }

        notifyMedia(albumId);
        notifyCounts(albumId);
    }

    /* ================= Refresh ================= */

    public void refreshAlbum(Context c,String albumId,Callback cb) {
        synchronized (lock) {
            cache.invalidateAlbum(albumId);
            photoCountByAlbum.remove(albumId);
            videoCountByAlbum.remove(albumId);
        }
        loadAlbum(c,albumId,cb);
    }

    /* ================= Listeners ================= */

    public void addMediaListener(String albumId,MediaListener l) {
        if (albumId == null || l == null) return;
        synchronized (lock) {
            mediaListenerByAlbum.put(albumId,l);
        }
    }

    public void removeMediaListener(String albumId,MediaListener l) {
        if (albumId == null || l == null) return;
        synchronized (lock) {
            if (mediaListenerByAlbum.get(albumId) == l) mediaListenerByAlbum.remove(albumId);
        }
    }

    public void addCountListener(String albumId,CountListener l) {
        if (albumId == null || l == null) return;
        synchronized (lock) {
            countListenerByAlbum.put(albumId,l);
        }
    }

    public void removeCountListener(String albumId,CountListener l) {
        if (albumId == null || l == null) return;
        synchronized (lock) {
            if (countListenerByAlbum.get(albumId) == l) countListenerByAlbum.remove(albumId);
        }
    }

    /* ================= Notify helpers ================= */

    private void notifyMedia(String albumId) {
        MediaListener l;
        Iterable<AlbumMedia> snapshot;

        synchronized (lock) {
            l = mediaListenerByAlbum.get(albumId);
            if (l == null) return;
            snapshot = cache.getOrCreateEntry(albumId).getMediaView();
        }

        mainHandler.post(() -> l.onAlbumChanged(snapshot));
    }

    private void notifyCounts(String albumId) {
        CountListener l;
        int photos;
        int videos;

        synchronized (lock) {
            l = countListenerByAlbum.get(albumId);
            if (l == null) return;

            Integer p = photoCountByAlbum.get(albumId);
            Integer v = videoCountByAlbum.get(albumId);
            photos = p != null ? p : 0;
            videos = v != null ? v : 0;
        }

        mainHandler.post(() -> l.onCountsChanged(photos,videos));
    }


}
