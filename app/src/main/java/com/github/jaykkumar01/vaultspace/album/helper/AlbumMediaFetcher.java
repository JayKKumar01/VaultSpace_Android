package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.github.jaykkumar01.vaultspace.models.base.UploadedItem;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

final class AlbumMediaFetcher {

    private static final String TAG = "VaultSpace:AlbumFetch";
    private final Context appContext;
    private final String albumId;
    private final DriveThumbnailResolver thumbnailResolver;

    AlbumMediaFetcher(@NonNull Context context, @NonNull String albumId) {
        this.appContext = context.getApplicationContext();
        this.albumId = albumId;
        this.thumbnailResolver = new DriveThumbnailResolver(appContext);
    }

    @NonNull
    List<AlbumMedia> fetch(@NonNull List<TrustedAccount> accounts) throws Exception {
        long startMs = System.currentTimeMillis();
        int available = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(1, Math.min(accounts.size(), available));
        Log.d(TAG, "fetch start albumId=" + albumId + " accounts=" + accounts.size() + " cpu=" + available + " threads=" + threads);

        Map<String, AlbumMedia> unique = new ConcurrentHashMap<>();
        List<Future<?>> tasks = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (TrustedAccount account : accounts) {
                tasks.add(executor.submit(() -> {
                    fetchFromAccount(account, unique);
                    return null;
                }));
            }
            for (Future<?> f : tasks) f.get();
        }

        List<AlbumMedia> result = new ArrayList<>(unique.values());
        result.sort((a, b) -> Long.compare(b.modifiedTime, a.modifiedTime));
        Log.d(TAG, "fetch done albumId=" + albumId + " items=" + result.size() + " took=" + (System.currentTimeMillis() - startMs) + "ms");
        return result;
    }

    private void fetchFromAccount(@NonNull TrustedAccount account, @NonNull Map<String, AlbumMedia> out) throws Exception {
        Drive drive = DriveClientProvider.forAccount(appContext, account.email);
        FileList list = drive.files().list()
                .setQ("'" + albumId + "' in parents and trashed=false")
                .setFields("files(id,name,mimeType,modifiedTime,size,thumbnailLink,appProperties)")
                .execute();
        if (list.getFiles() != null) {
            for (File f : list.getFiles()) {
                out.computeIfAbsent(f.getId(), id -> createAlbumMedia(account.email, f));
            }
        }
    }

    private AlbumMedia createAlbumMedia(@NonNull String email, @NonNull File file) {
        String thumbRef = thumbnailResolver.resolve(email, file);
        UploadedItem item = new UploadedItem(
                file.getId(),
                file.getName(),
                file.getMimeType(),
                file.getSize() != null ? file.getSize() : 0L,
                file.getModifiedTime() != null ? file.getModifiedTime().getValue() : 0L,
                thumbRef
        );
        return new AlbumMedia(item);
    }
}
