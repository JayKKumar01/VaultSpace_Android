package com.github.jaykkumar01.vaultspace.album.resolver;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.github.jaykkumar01.vaultspace.models.MediaSelection;

public final class MediaSelectionResolver {

    private final ContentResolver resolver;

    public MediaSelectionResolver(Context context) {
        this.resolver = context.getContentResolver();
    }

    public MediaSelection resolve(Uri uri) {
        String mimeType = resolver.getType(uri);

        assert mimeType != null;
        return new MediaSelection(
                uri,
                mimeType
        );
    }
}
