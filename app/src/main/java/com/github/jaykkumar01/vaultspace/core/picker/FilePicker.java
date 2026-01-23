package com.github.jaykkumar01.vaultspace.core.picker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class FilePicker {

    private final AppCompatActivity activity;
    private final FilePickerCallback callback;

    private ActivityResultLauncher<String[]> pickerLauncher;

    public FilePicker(AppCompatActivity activity, FilePickerCallback callback) {
        this.activity = activity;
        this.callback = callback;
        registerPicker();
    }

    private void registerPicker() {
        pickerLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris == null || uris.isEmpty()) {
                        return;
                    }
                    callback.onPicked(uris);

//                    ContentResolver resolver = activity.getContentResolver();
//                    List<UploadSelection> selections = new ArrayList<>();
//
//                    for (Uri uri : uris) {
//                        // ✅ Persist permission (THIS is the fix)
//                        resolver.takePersistableUriPermission(
//                                uri,
//                                Intent.FLAG_GRANT_READ_URI_PERMISSION
//                        );
//
//                        selections.add(this.resolver.resolve(uri));
//                    }
//
//                    callback.onMediaPicked(selections);
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
