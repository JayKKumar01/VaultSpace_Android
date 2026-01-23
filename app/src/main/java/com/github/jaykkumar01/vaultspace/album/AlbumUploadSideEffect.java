package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.upload.UploadSideEffect;
import com.github.jaykkumar01.vaultspace.core.upload.base.FailureReason;
import com.github.jaykkumar01.vaultspace.core.upload.drive.UploadDriveHelper;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;

public final class AlbumUploadSideEffect implements UploadSideEffect {

    private final Context appContext;

    public AlbumUploadSideEffect(Context context){
        appContext=context.getApplicationContext();
    }

    @Override
    public void onUploadSuccess(@NonNull String albumId, @NonNull UploadedItem item){
        UserSession userSession = new UserSession(appContext);
        AlbumMedia media=new AlbumMedia(item);
        userSession.getVaultCache().albumMedia
                .getOrCreateEntry(albumId)
                .addMedia(media);
    }

    @Override
    public void onUploadFailure(@NonNull String groupId, @NonNull UploadSelection selection, FailureReason reason){}
}
