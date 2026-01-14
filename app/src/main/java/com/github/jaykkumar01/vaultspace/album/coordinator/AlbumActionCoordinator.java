package com.github.jaykkumar01.vaultspace.album.coordinator;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.picker.AlbumMediaPicker;
import com.github.jaykkumar01.vaultspace.models.MediaSelection;

import java.util.List;

public class AlbumActionCoordinator {

    /* ============================================================
     * Host contract
     * ============================================================ */

    public interface Host {
        boolean isReleased();
        String getAlbumId();
    }

    /* ============================================================
     * Dependencies
     * ============================================================ */

    private static final String TAG = "VaultSpace:AlbumAction";

    private final Host host;
    private final AlbumMediaPicker mediaPicker;

    /* ============================================================
     * Constructor
     * ============================================================ */

    public AlbumActionCoordinator(
            Host host,
            AppCompatActivity activity
    ) {
        this.host = host;

        // Media picker is owned here, not by Activity
        this.mediaPicker = new AlbumMediaPicker(
                activity,
                mediaPickerCallback
        );
    }

    /* ============================================================
     * Media picker callback
     * ============================================================ */

    private final AlbumMediaPicker.Callback mediaPickerCallback =
            new AlbumMediaPicker.Callback() {

                @Override
                public void onMediaPicked(List<MediaSelection> selections) {
                    if (host.isReleased()) return;

                    Log.d(TAG, "Media picked for album=" + host.getAlbumId());
                    Log.d(TAG, "Picked count=" + selections.size());

                    for (MediaSelection selection : selections) {
                        Log.d(TAG, selection.toString());
                    }

                    // future:
                    // startUpload(selections)
                }

                @Override
                public void onPickCancelled() {
                    if (host.isReleased()) return;
                    Log.d(TAG, "Media pick cancelled");
                }
            };

    /* ============================================================
     * User intent handlers
     * ============================================================ */

    public void onAddMediaClicked() {
        if (host.isReleased()) return;

        Log.d(TAG, "Add media requested for album=" + host.getAlbumId());
        mediaPicker.launchPicker();
    }

    public void onMediaClicked(AlbumMedia media, int position) {
        if (host.isReleased()) return;

        Log.d(TAG, "Media clicked pos=" + position);

        // future:
        // openMediaViewer(media, position)
    }

    public void onMediaLongPressed(AlbumMedia media, int position) {
        if (host.isReleased()) return;

        Log.d(TAG, "Media long pressed pos=" + position);

        // future:
        // showContextMenu(media)
    }
}
