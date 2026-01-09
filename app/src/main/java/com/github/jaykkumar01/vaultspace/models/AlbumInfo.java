package com.github.jaykkumar01.vaultspace.models;

public final class AlbumInfo {

    public final String id;
    public final String name;
    public final long createdTime;
    public final long modifiedTime;

    // null = no cover yet
    public final String coverPath;

    public AlbumInfo(
            String id,
            String name,
            long createdTime,
            long modifiedTime,
            String coverPath
    ) {
        this.id = id;
        this.name = name;
        this.createdTime = createdTime;
        this.modifiedTime = modifiedTime;
        this.coverPath = coverPath;
    }
}
