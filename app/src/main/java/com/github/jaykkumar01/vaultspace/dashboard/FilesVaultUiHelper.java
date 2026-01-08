package com.github.jaykkumar01.vaultspace.dashboard;

import android.content.Context;
import android.widget.FrameLayout;

import com.github.jaykkumar01.vaultspace.R;

public class FilesVaultUiHelper extends BaseVaultSectionUiHelper {

    public FilesVaultUiHelper(Context context, FrameLayout container) {
        super(context, container);

        loadingView.setText("Loading files…");

        emptyView.setIcon(R.drawable.ic_files_empty);
        emptyView.setTitle("No files found");
        emptyView.setSubtitle("Files reflect how your data is stored in Drive.");
        emptyView.setPrimaryAction("Upload Files", v -> {});
        emptyView.setSecondaryAction("Create Folder", v -> {});
    }

    @Override
    public void show() {
        showLoading();

        // mocked for now
        container.post(() -> {
            boolean hasFiles = false;
            if (hasFiles) {
                showContent();
            } else {
                showEmpty();
            }
        });
    }
}
