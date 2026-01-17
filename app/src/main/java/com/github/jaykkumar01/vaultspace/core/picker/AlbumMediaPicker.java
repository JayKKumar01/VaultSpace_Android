package com.github.jaykkumar01.vaultspace.core.picker;

import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
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

    private ActivityResultLauncher<PickVisualMediaRequest> pickerLauncher;

    public AlbumMediaPicker(AppCompatActivity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
        this.resolver = new UploadSelectionResolver(activity);

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

                    List<UploadSelection> selections = new ArrayList<>();
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
