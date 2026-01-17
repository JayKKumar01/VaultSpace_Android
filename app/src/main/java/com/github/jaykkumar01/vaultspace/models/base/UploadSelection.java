package com.github.jaykkumar01.vaultspace.models.base;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class UploadSelection {

    @NonNull public final Uri uri;
    @NonNull public final String mimeType;
    @NonNull private final UploadType type;

    public UploadSelection(@NonNull Uri uri,@NonNull String mimeType){
        this.uri=uri;
        this.mimeType=mimeType;
        this.type=UploadType.fromMime(mimeType);
    }

    @NonNull
    public UploadType getType(){ return type; }

    @Override
    public boolean equals(Object o){
        if(this==o) return true;
        if(!(o instanceof UploadSelection that)) return false;
        return uri.toString().equals(that.uri.toString())
                && type==that.type;
    }

    @Override
    public int hashCode(){
        return Objects.hash(uri.toString(),type);
    }

    @Override
    @NonNull
    public String toString(){
        return "UploadSelection{" +
                "uri=" + uri +
                ", mimeType='" + mimeType + '\'' +
                ", type=" + type +
                '}';
    }
}
