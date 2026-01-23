package com.github.jaykkumar01.vaultspace.core.picker;

import android.net.Uri;

import java.util.List;

public interface FilePickerCallback {
    void onPicked(List<Uri> uris);
}