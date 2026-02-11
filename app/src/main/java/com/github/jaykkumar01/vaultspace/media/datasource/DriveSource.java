package com.github.jaykkumar01.vaultspace.media.datasource;

import java.io.IOException;
import java.io.InputStream;

interface DriveSource {
    InputStream openStream(long position) throws IOException;
    void close();
}
