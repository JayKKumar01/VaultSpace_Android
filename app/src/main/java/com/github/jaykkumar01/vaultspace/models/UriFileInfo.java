package com.github.jaykkumar01.vaultspace.models;

import android.net.Uri;

import androidx.annotation.NonNull;

public final class UriFileInfo {

    @NonNull public final Uri uri;
    @NonNull public final String name;
    public final long sizeBytes;
    public final long modifiedTimeMillis;

    public UriFileInfo(
            @NonNull Uri uri,
            @NonNull String name,
            long sizeBytes,
            long modifiedTimeMillis
    ){
        this.uri=uri;
        this.name=name;
        this.sizeBytes=sizeBytes;
        this.modifiedTimeMillis=modifiedTimeMillis;
    }
}
