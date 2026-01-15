package com.github.jaykkumar01.vaultspace.album.coordinator;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.picker.AlbumMediaPicker;
import com.github.jaykkumar01.vaultspace.models.MediaSelection;

import java.util.List;

public final class AlbumActionCoordinator {
    private boolean released;

    /* ============================================================
     * Host contract
     * ============================================================ */

    public interface Callback {
        void onMediaSelected(List<MediaSelection> selections);
    }

    /* ============================================================
     * Fields
     * ============================================================ */

    private static final String TAG = "VaultSpace:AlbumAction";
    private final Callback callback;
    private final AlbumMediaPicker mediaPicker;

    /* ============================================================
     * Constructor
     * ============================================================ */

    public AlbumActionCoordinator(AppCompatActivity activity, Callback callback) {
        this.callback = callback;
        AlbumMediaPicker.Callback pickerCallback = new AlbumMediaPicker.Callback() {

            @Override
            public void onMediaPicked(List<MediaSelection> selections) {
                if (released) return;

                if (!selections.isEmpty()) {
                    callback.onMediaSelected(selections);
                }
            }

            @Override
            public void onPickCancelled() {
                if (released) return;
                Log.d(TAG, "Media pick cancelled");
            }
        };
        this.mediaPicker = new AlbumMediaPicker(activity, pickerCallback);
    }

    /* ============================================================
     * Media picker callback
     * ============================================================ */

    /* ============================================================
     * User intents
     * ============================================================ */

    public void onAddMediaClicked() {
        if (released) return;
        mediaPicker.launchPicker();
    }

    public void onMediaClicked(AlbumMedia media, int position) {
        if (released) return;

        Log.d(TAG, "Media clicked pos=" + position);

        // future:
        // openMediaViewer(media, position)
    }

    public void onMediaLongPressed(AlbumMedia media, int position) {
        if (released) return;

        Log.d(TAG, "Media long pressed pos=" + position);

        // future:
        // showContextMenu(media)
    }

    /* ============================================================
     * Internal helpers
     * ============================================================ */

    public void release() {
        released = true;
    }
}
