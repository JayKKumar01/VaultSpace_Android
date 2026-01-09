package com.github.jaykkumar01.vaultspace.interfaces;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

public interface AlbumItemCallbacks {
        void onOverflowClicked(AlbumInfo album);
        void onLongPressed(AlbumInfo album);
    }