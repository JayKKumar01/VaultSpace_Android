package com.github.jaykkumar01.vaultspace.media.proxy;

public final class HlsSegmenter {

    public static final int SEGMENT_SIZE = 2 * 1024 * 1024; // 2MB
    private static final float SEGMENT_DURATION = 2.0f;

    private final long fileSize;

    public HlsSegmenter(long fileSize) {
        this.fileSize = fileSize;
    }

    public int segmentCount() {
        return (int) Math.ceil((double) fileSize / SEGMENT_SIZE);
    }

    public long start(int index) {
        return (long) index * SEGMENT_SIZE;
    }

    public long end(int index) {
        return Math.min(start(index) + SEGMENT_SIZE - 1, fileSize - 1);
    }

    public String buildPlaylist() {
        StringBuilder m = new StringBuilder();
        m.append("#EXTM3U\n");
        m.append("#EXT-X-VERSION:7\n");
        m.append("#EXT-X-TARGETDURATION:2\n");
        m.append("#EXT-X-PLAYLIST-TYPE:VOD\n");
        m.append("#EXT-X-MEDIA-SEQUENCE:0\n");

        for (int i = 0; i < segmentCount(); i++) {
            m.append("#EXTINF:").append(SEGMENT_DURATION).append(",\n");
            m.append("seg_").append(i).append(".m4s\n");
        }

        m.append("#EXT-X-ENDLIST\n");
        return m.toString();
    }
}
