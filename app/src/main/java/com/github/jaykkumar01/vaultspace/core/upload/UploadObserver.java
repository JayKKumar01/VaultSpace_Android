package com.github.jaykkumar01.vaultspace.core.upload;

/**
 * UploadObserver
 *
 * Implemented by active album components to receive
 * upload snapshot updates for a specific album.
 *
 * Observers are registered/unregistered explicitly
 * by UploadManager.
 */
public interface UploadObserver {

    /**
     * Called when the UploadSnapshot for this album
     * changes in a user-observable way.
     *
     * Emits happen only when counts change or
     * snapshot is replaced.
     */
    void onSnapshot(UploadSnapshot snapshot);
    void onCancelled();
}
