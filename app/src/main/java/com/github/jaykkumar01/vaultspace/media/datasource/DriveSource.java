package com.github.jaykkumar01.vaultspace.media.datasource;

import java.io.IOException;

interface DriveSource {
    byte[] fetchRange(long position,int length) throws IOException;
}
