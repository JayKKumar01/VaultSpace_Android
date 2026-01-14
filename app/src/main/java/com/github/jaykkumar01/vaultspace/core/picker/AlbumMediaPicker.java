package com.github.jaykkumar01.vaultspace.core.picker;

import android.app.Activity;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.album.resolver.MediaSelectionResolver;
import com.github.jaykkumar01.vaultspace.models.MediaSelection;

import java.util.ArrayList;
import java.util.List;

public class AlbumMediaPicker {

    public interface Callback {
        void onMediaPicked(List<MediaSelection> selections);
        void onPickCancelled();
    }

    private final AppCompatActivity activity;
    private final Callback callback;
    private final MediaSelectionResolver resolver;

    private ActivityResultLauncher<PickVisualMediaRequest> pickerLauncher;

    public AlbumMediaPicker(AppCompatActivity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
        this.resolver = new MediaSelectionResolver(activity);

        registerPicker();
    }

    private void registerPicker() {
        pickerLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(),
                uris -> {
                    if (uris == null || uris.isEmpty()) {
                        callback.onPickCancelled();
                        return;
                    }

                    List<MediaSelection> selections = new ArrayList<>();
                    for (Uri uri : uris) {
                        selections.add(resolver.resolve(uri));
                    }

                    callback.onMediaPicked(selections);
                }
        );
    }

    public void launchPicker() {
        pickerLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                        .build()
        );


    }

}
