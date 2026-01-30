package com.github.jaykkumar01.vaultspace.album.model;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;

public final class AlbumItem {

    public final AlbumMedia media;
    public final int adapterPosition;

    public AlbumItem(AlbumMedia media,int adapterPosition){
        this.media=media;
        this.adapterPosition=adapterPosition;
    }
}
