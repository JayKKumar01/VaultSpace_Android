package com.github.jaykkumar01.vaultspace.core.download.base;

import android.os.Parcel;
import android.os.Parcelable;

public final class DownloadRequest implements Parcelable {

    public final String fileId;
    public final String name;
    public final long sizeBytes;

    public DownloadRequest(String fileId, String name, long sizeBytes) {
        this.fileId = fileId;
        this.name = name;
        this.sizeBytes = sizeBytes;
    }

    private DownloadRequest(Parcel in) {
        fileId = in.readString();
        name = in.readString();
        sizeBytes = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fileId);
        dest.writeString(name);
        dest.writeLong(sizeBytes);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<DownloadRequest> CREATOR =
            new Creator<>() {
                @Override public DownloadRequest createFromParcel(Parcel in) {
                    return new DownloadRequest(in);
                }
                @Override public DownloadRequest[] newArray(int size) {
                    return new DownloadRequest[size];
                }
            };
}
