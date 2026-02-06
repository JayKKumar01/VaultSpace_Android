package com.github.jaykkumar01.vaultspace.core.selection;

import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class UriSelection {

    private final AppCompatActivity activity;
    private final UriSelectionListener callback;
    private ActivityResultLauncher<String[]> launcher;

    public UriSelection(AppCompatActivity activity, UriSelectionListener callback) {
        this.activity = activity;
        this.callback = callback;
        register();
    }

    private void register() {
        launcher = activity.registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris == null || uris.isEmpty()) return;

                    for (Uri uri : uris) {
                        activity.getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    }

                    callback.onSelected(uris);
                }
        );
    }

    /** Image + video URIs */
    public void selectMediaUris() {
        launcher.launch(new String[]{"image/*", "video/*"});
    }

    /** Any document URIs */
    public void selectAnyUris() {
        launcher.launch(new String[]{"*/*"});
    }

    public void releasePersistableUriPermission(Uri uri) {
        try {
            activity.getContentResolver().releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
            // Permission might already be released or never persisted
        }
    }
}
