package com.github.jaykkumar01.vaultspace.core.picker;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.album.resolver.UploadSelectionResolver;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;

import java.util.ArrayList;
import java.util.List;

public class AlbumMediaPicker {

    public interface Callback {
        void onMediaPicked(List<UploadSelection> selections);
        void onPickCancelled();
    }

    private final AppCompatActivity activity;
    private final Callback callback;
    private final UploadSelectionResolver resolver;

    private ActivityResultLauncher<String[]> pickerLauncher;

    public AlbumMediaPicker(AppCompatActivity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
        this.resolver = new UploadSelectionResolver(activity);
        registerPicker();
    }

    private void registerPicker() {
        pickerLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris == null || uris.isEmpty()) {
                        callback.onPickCancelled();
                        return;
                    }

                    ContentResolver resolver = activity.getContentResolver();
                    List<UploadSelection> selections = new ArrayList<>();

                    for (Uri uri : uris) {
                        // ✅ Persist permission (THIS is the fix)
                        resolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );

                        selections.add(this.resolver.resolve(uri));
                    }

                    callback.onMediaPicked(selections);
                }
        );
    }

    public void launchPicker() {
        pickerLauncher.launch(new String[]{
                "image/*",
                "video/*"
        });
    }
}
