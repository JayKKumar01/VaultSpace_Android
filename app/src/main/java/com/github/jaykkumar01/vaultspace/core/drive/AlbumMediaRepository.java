package com.github.jaykkumar01.vaultspace.core.drive;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
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
    private static final String TAG = "VaultSpace:AlbumRepo";

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

    public interface AlbumStateListener {
        void onLoading();
        void onMedia(Iterable<AlbumMedia> media);
        void onError(Exception e);
    }

    /** Used for RecyclerView delta updates (O(1)) */
    public interface MediaDeltaListener {
        void onMediaAdded(AlbumMedia media);
        void onMediaRemoved(String mediaId);
    }

    /** Used by AlbumMetaInfoView (O(1)) */
    public interface CountListener {
        void onCountsChanged(int photos,int videos);
    }

    /* ================= Core ================= */

    private final AlbumMediaCache cache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Object lock = new Object();

    private final Map<String,Boolean> loading = new HashMap<>();

    private final Map<String,AlbumStateListener> stateListenerByAlbum = new HashMap<>();
    private final Map<String,MediaDeltaListener> deltaListenerByAlbum = new HashMap<>();
    private final Map<String,CountListener> countListenerByAlbum = new HashMap<>();


    /** Derived state (owned by repo) */
    private final Map<String,Integer> photoCountByAlbum = new HashMap<>();
    private final Map<String,Integer> videoCountByAlbum = new HashMap<>();

    private AlbumMediaRepository(Context appContext) {
        cache = new UserSession(appContext).getVaultCache().albumMedia;
    }

    /* ================= Load / Init ================= */

    public void openAlbum(Context c,String albumId) {
        AlbumMediaEntry entry = cache.getOrCreateEntry(albumId);

        synchronized (lock) {
            AlbumStateListener l = stateListenerByAlbum.get(albumId);
            if (l == null) return;

            if (entry.isInitialized()) {
                notifyMedia(albumId);
                notifyCounts(albumId);
                return;
            }

            if (Boolean.TRUE.equals(loading.get(albumId))) return;
            loading.put(albumId,true);
        }

        notifyLoading(albumId);

        AlbumDriveHelper drive = new AlbumDriveHelper(c,albumId);
        drive.fetchAlbumMedia(executor,new AlbumDriveHelper.FetchCallback() {
            @Override public void onResult(@NonNull List<AlbumMedia> items) {
                int photos = 0, videos = 0;
                for (AlbumMedia m : items) {
                    if (m.isVideo) videos++; else photos++;
                }

                synchronized (lock) {
                    entry.initializeFromDrive(items);
                    photoCountByAlbum.put(albumId,photos);
                    videoCountByAlbum.put(albumId,videos);
                    loading.remove(albumId);
                }

                notifyMedia(albumId);
                notifyCounts(albumId);
            }

            @Override public void onError(@NonNull Exception e) {
                synchronized (lock) {
                    loading.remove(albumId);
                }
                notifyError(albumId,e);
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

    public boolean isAlbumEmpty(String albumId) {
        AlbumMediaEntry entry = cache.getOrCreateEntry(albumId);
        synchronized (lock) {
            return entry.isEmpty();
        }
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

        notifyDeltaAdded(albumId,media);
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

        notifyDeltaRemoved(albumId,media.fileId);
        notifyCounts(albumId);
    }

    public AlbumMedia getMediaById(String albumId,String fileId) {
        AlbumMediaEntry entry = cache.getOrCreateEntry(albumId);
        if (!entry.isInitialized()) return null;
        return entry.getByMediaId(fileId);
    }


    /* ================= Refresh ================= */

    public void refreshAlbum(Context c,String albumId) {
        synchronized (lock) {
            cache.invalidateAlbum(albumId);
            photoCountByAlbum.remove(albumId);
            videoCountByAlbum.remove(albumId);
        }
        openAlbum(c,albumId);
    }


    /* ================= Listeners ================= */

    public void addAlbumStateListener(String albumId,AlbumStateListener l) {
        if (albumId == null || l == null) return;
        synchronized (lock) {
            stateListenerByAlbum.put(albumId,l);
        }

    }

    public void removeAlbumStateListener(String albumId,AlbumStateListener l) {
        if (albumId == null || l == null) return;
        synchronized (lock) {
            if (stateListenerByAlbum.get(albumId) == l)
                stateListenerByAlbum.remove(albumId);
        }
    }


    public void addDeltaListener(String albumId,MediaDeltaListener l) {
        if (albumId == null || l == null) return;
        synchronized (lock) {
            deltaListenerByAlbum.put(albumId,l);
        }
    }

    public void removeDeltaListener(String albumId,MediaDeltaListener l) {
        if (albumId == null || l == null) return;
        synchronized (lock) {
            if (deltaListenerByAlbum.get(albumId) == l) deltaListenerByAlbum.remove(albumId);
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

    private void notifyLoading(String albumId) {
        AlbumStateListener l;
        synchronized (lock) {
            l = stateListenerByAlbum.get(albumId);
            if (l == null) return;
        }
        mainHandler.post(l::onLoading);
    }

    private void notifyMedia(String albumId) {
        AlbumStateListener l;
        Iterable<AlbumMedia> snapshot;

        synchronized (lock) {
            l = stateListenerByAlbum.get(albumId);
            if (l == null) return;
            snapshot = cache.getOrCreateEntry(albumId).getMediaView();
        }

        mainHandler.post(() -> l.onMedia(snapshot));
    }

    private void notifyError(String albumId,Exception e) {
        AlbumStateListener l;
        synchronized (lock) {
            l = stateListenerByAlbum.get(albumId);
            if (l == null) return;
        }
        mainHandler.post(() -> l.onError(e));
    }


    private void notifyDeltaAdded(String albumId,AlbumMedia media) {
        MediaDeltaListener l;
        synchronized (lock) {
            l = deltaListenerByAlbum.get(albumId);
            if (l == null) return;
        }
        mainHandler.post(() -> l.onMediaAdded(media));
    }

    private void notifyDeltaRemoved(String albumId,String mediaId) {
        MediaDeltaListener l;
        synchronized (lock) {
            l = deltaListenerByAlbum.get(albumId);
            if (l == null) return;
        }
        mainHandler.post(() -> l.onMediaRemoved(mediaId));
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

    public static void destroy() {
        synchronized (AlbumsRepository.class) {
            if (INSTANCE != null) {
                INSTANCE.releaseInternal();
                INSTANCE = null;
            }
        }
    }

    private void releaseInternal() {
        executor.shutdown();
    }
}
