//package com.github.jaykkumar01.vaultspace.media.datasource.bu;
//
//import androidx.annotation.OptIn;
//import androidx.media3.common.util.UnstableApi;
//import androidx.media3.datasource.DataSpec;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//
//interface DriveSource {
//
//    @OptIn(markerClass = UnstableApi.class)
//    long open(DataSpec spec) throws IOException;
//
//    int read(byte[] buffer, int offset, int length) throws IOException;
//
//    Map<String, List<String>> getResponseHeaders();
//
//    void close();
//}
