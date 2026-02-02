package com.github.jaykkumar01.vaultspace.album.band;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import java.util.Objects;

public final class Band {

    public final AlbumMedia first;
    public final AlbumMedia second;   // null = solo
    public final String timeLabel;

    public Band(AlbumMedia first, AlbumMedia second, String timeLabel) {
        this.first = first;
        this.second = second;
        this.timeLabel = timeLabel;
    }

    public boolean isSolo() { return second == null; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Band b)) return false;

        if (!Objects.equals(timeLabel, b.timeLabel)) return false;
        if (!Objects.equals(first.fileId, b.first.fileId)) return false;

        if (second == null && b.second == null) return true;
        if (second == null || b.second == null) return false;

        return Objects.equals(second.fileId, b.second.fileId);
    }

    @Override
    public int hashCode() {
        int h = first.fileId.hashCode();
        h = 31 * h + (second != null ? second.fileId.hashCode() : 0);
        h = 31 * h + timeLabel.hashCode();
        return h;
    }
}
