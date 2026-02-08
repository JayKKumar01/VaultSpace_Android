package com.github.jaykkumar01.vaultspace.dashboard.albums.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public final class AlbumsDriveHelper {

    private static final String TAG = "VaultSpace:AlbumsDrive";
    private static final String PROP_COVER = "cover";

    /* ================= Callbacks ================= */

    public interface Success<T> { void call(T value); }
    public interface Failure { void call(Exception e); }

    /* ================= Core ================= */

    private final Context appContext;
    private final Drive drive;
    private final AlbumsFetcher albumsFetcher;
    private final CoverResolver coverResolver;
    private final Handler main = new Handler(Looper.getMainLooper());

    public AlbumsDriveHelper(@NonNull Context context) {
        appContext = context.getApplicationContext();
        drive = DriveClientProvider.getPrimaryDrive(appContext);
        albumsFetcher = new AlbumsFetcher(drive);
        coverResolver = new CoverResolver(appContext,drive);
    }

    /* ================= Fetch ================= */

    public void fetchAlbums(ExecutorService exec, Success<List<AlbumInfo>> ok, Failure err) {
        exec.execute(() -> {
            try {
                String rootId = DriveFolderRepository.getAlbumsRootId(appContext);
                if (rootId == null) { post(() -> ok.call(List.of())); return; }

                List<File> files = albumsFetcher.fetchAll(rootId);
                List<AlbumInfo> out = new ArrayList<>(files.size());

                for (File f : files) {
                    Map<String,String> props = f.getAppProperties();
                    String coverId = props != null ? props.get(PROP_COVER) : null;
                    String coverPath = coverResolver.resolve(coverId);

                    out.add(new AlbumInfo(
                            f.getId(),
                            f.getName(),
                            f.getCreatedTime().getValue(),
                            f.getModifiedTime().getValue(),
                            coverPath
                    ));
                }

                post(() -> ok.call(out));

            } catch (Exception e) {
                post(() -> err.call(e));
            }
        });
    }


    /* ================= Create ================= */

    public void createAlbum(ExecutorService exec, String albumName, Success<AlbumInfo> ok, Failure err) {
        String name = albumName == null ? "" : albumName.trim();
        if (name.isEmpty()) { err.call(new IllegalArgumentException("Album name is empty")); return; }

        exec.execute(() -> {
            try {
                File created = DriveFolderRepository.resolveFolder(
                        drive, name, DriveFolderRepository.getAlbumsRootId(appContext)
                );

                post(() -> ok.call(new AlbumInfo(
                        created.getId(),
                        created.getName(),
                        created.getCreatedTime().getValue(),
                        created.getModifiedTime().getValue(),
                        null
                )));

            } catch (Exception e) {
                Log.e(TAG, "createAlbum failed", e);
                post(() -> err.call(e));
            }
        });
    }

    /* ================= Rename ================= */

    public void renameAlbum(ExecutorService exec, String albumId, String newName, Runnable ok, Failure err) {
        exec.execute(() -> {
            try {
                drive.files().update(albumId, new File().setName(newName)).setFields("id").execute();
                post(ok);
            } catch (Exception e) {
                Log.e(TAG, "renameAlbum failed", e);
                post(() -> err.call(e));
            }
        });
    }

    /* ================= Cover ================= */

    public void setAlbumCover(ExecutorService exec,String albumId,String coverFileId,Success<String> ok,Failure err) {
        exec.execute(() -> {
            try {
                File current = drive.files().get(albumId).setFields("appProperties").execute();
                Map<String,String> merged = withProp(current.getAppProperties(), coverFileId);

                drive.files().update(albumId,new File().setAppProperties(merged)).setFields("id").execute();

                String path = coverResolver.resolve(coverFileId);
                post(() -> ok.call(path));

            } catch (Exception e) {
                Log.e(TAG,"setAlbumCover failed",e);
                post(() -> err.call(e));
            }
        });
    }


    public void clearAlbumCover(ExecutorService exec,String albumId,String coverFileId,Runnable ok,Failure err) {
        exec.execute(() -> {
            try {
                File current = drive.files().get(albumId).setFields("appProperties").execute();
                Map<String,String> merged = withProp(current.getAppProperties(), null);

                drive.files().update(albumId,new File().setAppProperties(merged)).setFields("id").execute();

                coverResolver.clear(coverFileId);
                post(ok);

            } catch (Exception e) {
                Log.e(TAG,"clearAlbumCover failed",e);
                post(() -> err.call(e));
            }
        });
    }


    private static Map<String,String> withProp(Map<String,String> base, String value) {
        Map<String,String> out = base == null ? new java.util.HashMap<>() : new java.util.HashMap<>(base);
        if (value == null) out.remove(AlbumsDriveHelper.PROP_COVER);
        else out.put(AlbumsDriveHelper.PROP_COVER,value);
        return out.isEmpty() ? null : out;
    }


    /* ================= Helpers ================= */

    private void post(Runnable r) { main.post(r); }
}
