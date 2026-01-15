package com.github.jaykkumar01.vaultspace.album.upload;

final class AlbumUploadOrchestratorHolder {
    private static AlbumUploadOrchestrator INSTANCE;

    static void set(AlbumUploadOrchestrator o) {
        INSTANCE = o;
    }

    static AlbumUploadOrchestrator get() {
        return INSTANCE;
    }
}
