package com.github.jaykkumar01.vaultspace.album.band;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

public final class Band {

    public final AlbumMedia first;
    public final AlbumMedia second; // null = solo
    public final String timeLabel;  // ⬅️ NEW

    public Band(AlbumMedia first, AlbumMedia second, String timeLabel) {
        this.first = first;
        this.second = second;
        this.timeLabel = timeLabel;
    }

    public boolean isSolo() {
        return second == null;
    }
}
