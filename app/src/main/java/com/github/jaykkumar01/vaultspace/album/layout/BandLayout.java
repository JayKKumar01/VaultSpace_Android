package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.model.MediaFrame;
import java.util.Objects;

public final class BandLayout {

    // semantic
    public final String timeLabel;

    // presentation
    public boolean showTimeLabel;

    // geometry
    public final int bandHeight;
    public final MediaFrame[] frames;

    // motion
    public float rotationDeg;

    public BandLayout(String timeLabel,int bandHeight,MediaFrame[] frames) {
        this.timeLabel = timeLabel;
        this.bandHeight = bandHeight;
        this.frames = frames;
        this.showTimeLabel = false;
        this.rotationDeg = 0f;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BandLayout other)) return false;

        if (frames.length != other.frames.length) return false;

        for (int i = 0; i < frames.length; i++) {
            String a = frames[i].media.fileId;
            String b = other.frames[i].media.fileId;
            if (!Objects.equals(a, b)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 1;
        for (MediaFrame f : frames) {
            h = 31 * h + f.media.fileId.hashCode();
        }
        return h;
    }
}
