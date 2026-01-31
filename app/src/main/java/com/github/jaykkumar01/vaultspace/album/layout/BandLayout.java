package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.model.MediaFrame;

public final class BandLayout {

    public final String timeLabel;
    public final int bandHeight;
    public final MediaFrame[] frames;

    // motion (assigned after river transform)
    public float rotationDeg;   // ← assign-once, render-only

    public BandLayout(
            String timeLabel,
            int bandHeight,
            MediaFrame[] frames
    ) {
        this.timeLabel = timeLabel;
        this.bandHeight = bandHeight;
        this.frames = frames;
        this.rotationDeg = 0f;
    }
}

