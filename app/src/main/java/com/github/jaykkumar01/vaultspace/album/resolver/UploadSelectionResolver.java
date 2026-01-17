package com.github.jaykkumar01.vaultspace.album.resolver;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;

public final class UploadSelectionResolver {

    private final ContentResolver resolver;

    public UploadSelectionResolver(Context context) {
        this.resolver = context.getContentResolver();
    }

    public UploadSelection resolve(Uri uri) {
        String mimeType = resolver.getType(uri);

        assert mimeType != null;

        return new UploadSelection(
                uri,
                mimeType
        );
    }
}
