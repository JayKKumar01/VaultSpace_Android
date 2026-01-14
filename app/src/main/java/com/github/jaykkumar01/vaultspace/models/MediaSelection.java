package com.github.jaykkumar01.vaultspace.models;

import android.net.Uri;

import androidx.annotation.NonNull;

public final class MediaSelection {

    public final Uri uri;
    public final String mimeType;
    public final boolean isVideo;

    public MediaSelection(
            Uri uri,
            String mimeType,
            boolean isVideo
    ) {
        this.uri = uri;
        this.mimeType = mimeType;
        this.isVideo = isVideo;
    }

    @NonNull
    @Override
    public String toString() {
        return "MediaSelection{" +
                "uri=" + uri +
                ", mimeType='" + mimeType + '\'' +
                ", isVideo=" + isVideo +
                '}';
    }
}
