package com.github.jaykkumar01.vaultspace.models;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.models.base.UploadType;

/**
 * Non-media files.
 */
public final class FileSelection extends UploadSelection {

    public FileSelection(
            @NonNull Uri uri,
            @NonNull String mimeType
    ) {
        super(uri, mimeType);

        if (getType() != UploadType.FILE) {
            throw new IllegalArgumentException(
                    "FileSelection cannot be image/* or video/*, got: " + mimeType
            );
        }
    }
}
