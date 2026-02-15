package com.github.jaykkumar01.vaultspace.dashboard.files;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.dashboard.base.BaseSectionUi;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.form.FormSpec;
import com.github.jaykkumar01.vaultspace.views.states.FilesContentView;

public class FilesUi extends BaseSectionUi {

    private static final String TAG = "VaultSpace:FilesUI";

    public FilesUi(Context context, FrameLayout container, ModalHost hostView) {
        super(context, container, hostView);

        loadingView.setText("Loading files…");

        emptyView.setIcon(R.drawable.ic_files_empty);
        emptyView.setTitle("No files found");
        emptyView.setSubtitle("Files reflect how your data is stored in Drive.");

        emptyView.setPrimaryAction("Upload Files", v ->
                Log.d(TAG, "EmptyView → Upload Files clicked (stub)")
        );

        emptyView.setSecondaryAction("Create Folder", v -> {
            Log.d(TAG, "EmptyView → Create Folder clicked");
            showCreateFolderPopup();
        });

    }

    @Override
    protected View createContentView(Context context) {
        return new FilesContentView(context);
    }

    @Override
    public void show() {
        showLoading();

        // mocked state for now
        container.post(() -> {
            boolean hasFiles = false;
            Log.d(TAG, "mock show(), hasFiles=" + hasFiles);
            if (hasFiles) showContent();
            else showEmpty();
        });
    }

    /* ---------------- Popup (stub) ---------------- */

    private void showCreateFolderPopup() {
        hostView.request(new FormSpec(
                "Create Folder",
                "Folder name",
                "Create",
                this::createFolder,
                null
        ));
    }

    private void createFolder(String name) {
        Log.d(TAG, "CreateFolderView → onCreate: " + name + " (stub)");
    }


    @Override
    public void onRelease() {
        Log.d(TAG, "release");
    }
}
