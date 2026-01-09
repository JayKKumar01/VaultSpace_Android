package com.github.jaykkumar01.vaultspace.utils;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

public final class AlbumUiUtils {

    private static final String TEMP_PREFIX = "temp_";

    private AlbumUiUtils() {}

    public static boolean isTempAlbum(AlbumInfo album) {
        return album != null
                && album.id != null
                && album.id.startsWith(TEMP_PREFIX);
    }
}
