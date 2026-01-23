package com.github.jaykkumar01.vaultspace.album.helper;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;

public final class AlbumCacheWriter {

    public static void onUploadSuccess(
            @NonNull Context context,
            @NonNull String albumId,
            @NonNull UploadedItem item
    ) {
        AlbumMedia media = new AlbumMedia(item);
        UserSession session = new UserSession(context);
        session.getVaultCache()
                .albumMedia
                .getOrCreateEntry(albumId)
                .addMedia(media);
    }
}
