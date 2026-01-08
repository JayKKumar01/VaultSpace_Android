package com.github.jaykkumar01.vaultspace.models;

public final class AlbumInfo {

    public final String id;
    public final String name;
    public final long createdTime;
    public final long modifiedTime;

    public AlbumInfo(
            String id,
            String name,
            long createdTime,
            long modifiedTime
    ) {
        this.id = id;
        this.name = name;
        this.createdTime = createdTime;
        this.modifiedTime = modifiedTime;
    }
}
