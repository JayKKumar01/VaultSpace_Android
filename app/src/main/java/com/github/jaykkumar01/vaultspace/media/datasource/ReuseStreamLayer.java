package com.github.jaykkumar01.vaultspace.media.datasource;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

final class ReuseStreamLayer implements DriveStreamSource {

    private final DriveStreamSource upstream;

    private StreamSession activeSession;
    private InputStream activeStream;
    private long activePosition;
    private boolean logicallyClosed;

    ReuseStreamLayer(@NonNull DriveStreamSource upstream) {
        this.upstream = upstream;
    }

    @Override
    public synchronized StreamSession open(long position) throws IOException {
        if (canReuse(position)) {
            logicallyClosed = false;
            return wrap();
        }
        forceClose();
        StreamSession s = upstream.open(position);
        activeSession = s;
        activeStream = s.stream();
        activePosition = position;
        logicallyClosed = false;
        return wrap();
    }

    private boolean canReuse(long pos) {
        return activeSession != null && logicallyClosed && pos == activePosition;
    }

    private StreamSession wrap() {
        return new StreamSession() {
            @Override
            public InputStream stream() {
                return new TrackingStream(activeStream);
            }

            @Override
            public long length() {
                return activeSession.length();
            }

            @Override
            public void cancel() {
                logicallyClosed = true;
            }
        };
    }

    void forceClose() {
        if (activeSession != null) {
            try {
                activeSession.cancel();
            } catch (Exception ignored) {
            }
        }
        activeSession = null;
        activeStream = null;
        logicallyClosed = false;
    }

    private final class TrackingStream extends InputStream {
        private final InputStream delegate;

        TrackingStream(InputStream d) {
            this.delegate = d;
        }

        @Override
        public int read() throws IOException {
            int r = delegate.read();
            if (r != -1) activePosition++;
            return r;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int r = delegate.read(b, off, len);
            if (r > 0) activePosition += r;
            return r;
        }

        @Override
        public void close() {
        } // logical only
    }
}
