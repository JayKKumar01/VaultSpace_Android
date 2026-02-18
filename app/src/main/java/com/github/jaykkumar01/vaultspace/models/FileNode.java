package com.github.jaykkumar01.vaultspace.models;

import java.util.Objects;

public final class FileNode {

    public final String id, name, mimeType;
    public final boolean isFolder;
    public final long sizeBytes, modifiedTime;

    public FileNode(String id, String name, String mimeType, long sizeBytes, long modifiedTime) {
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.isFolder = "application/vnd.google-apps.folder".equals(mimeType);
        this.sizeBytes = sizeBytes;
        this.modifiedTime = modifiedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileNode that)) return false;
        return isFolder == that.isFolder &&
                sizeBytes == that.sizeBytes &&
                modifiedTime == that.modifiedTime &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(mimeType, that.mimeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, mimeType, isFolder, sizeBytes, modifiedTime);
    }
}
