package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.model.MediaFrame;

public final class BandLayout {

    // semantic
    public final String timeLabel;

    // presentation
    public boolean showTimeLabel;

    // geometry
    public final int bandHeight;
    public final MediaFrame[] frames;

    // motion (assigned after river transform)
    public float rotationDeg;   // assign-once, render-only

    public BandLayout(
            String timeLabel,
            int bandHeight,
            MediaFrame[] frames
    ) {
        this.timeLabel = timeLabel;
        this.bandHeight = bandHeight;
        this.frames = frames;

        // default: visible, normalization may suppress
        this.showTimeLabel = true;

        this.rotationDeg = 0f;
    }
}
