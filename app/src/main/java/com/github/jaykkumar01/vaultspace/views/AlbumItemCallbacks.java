package com.github.jaykkumar01.vaultspace.views;

import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

interface AlbumItemCallbacks {
        void onOverflowClicked(AlbumInfo album);
        void onLongPressed(AlbumInfo album);
    }