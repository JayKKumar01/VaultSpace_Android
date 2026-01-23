package com.github.jaykkumar01.vaultspace.core.selection;

import android.net.Uri;
import androidx.annotation.NonNull;
import java.util.List;

public interface UriSelectionListener {
    void onSelected(@NonNull List<Uri> uris);
}
