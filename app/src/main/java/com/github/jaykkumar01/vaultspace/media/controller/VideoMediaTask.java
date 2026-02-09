package com.github.jaykkumar01.vaultspace.media.controller;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

interface VideoMediaTask {

    void start(@NonNull AlbumMedia media, @NonNull Callback callback);

    void cancel();

    interface Callback {

        // URL optimistic attach OR final attach
        void onAttachReady(@NonNull AttachPayload payload);

        // Task succeeded, session may COMMIT
        void onHealthy();

        // Task failed but recoverable (URL → DOWNLOAD)
        void onUnhealthy();

        // Hard failure → ABORT
        void onError(@NonNull Exception e);
    }
}
