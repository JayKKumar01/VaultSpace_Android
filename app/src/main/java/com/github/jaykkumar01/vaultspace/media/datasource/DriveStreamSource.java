package com.github.jaykkumar01.vaultspace.media.datasource;

import java.io.IOException;
import java.io.InputStream;

public interface DriveStreamSource {

    StreamSession open(long position) throws IOException;

    interface StreamSession {
        InputStream stream();
        long length();     // exact bytes available from this open
        void cancel();
    }

}
