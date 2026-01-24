package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.AlbumMediaRepository;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSideEffect;
import com.github.jaykkumar01.vaultspace.core.upload.base.FailureReason;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;

public final class AlbumUploadSideEffect implements UploadSideEffect {

    private final AlbumMediaRepository repo;

    public AlbumUploadSideEffect(Context context) {
        Context appContext = context.getApplicationContext();
        repo = AlbumMediaRepository.getInstance(appContext);
    }

    @Override
    public void onUploadSuccess(@NonNull String albumId, @NonNull UploadedItem item) {
        repo.addMedia(albumId, new AlbumMedia(item));
    }

    @Override
    public void onUploadFailure(@NonNull String albumId, @NonNull UploadSelection selection, FailureReason reason) {
    }
}
