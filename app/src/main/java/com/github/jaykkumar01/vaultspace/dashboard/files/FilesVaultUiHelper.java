package com.github.jaykkumar01.vaultspace.dashboard.files;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.BaseVaultSectionUiHelper;
import com.github.jaykkumar01.vaultspace.views.states.FilesContentView;
import com.github.jaykkumar01.vaultspace.views.popups.FolderActionView;

public class FilesVaultUiHelper extends BaseVaultSectionUiHelper {

    private static final String TAG = "VaultSpace:FilesUI";

    public FilesVaultUiHelper(Context context, FrameLayout container) {
        super(context, container);

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
        showFolderActionPopup(
                "Create Folder",
                "Folder name",
                "Create",
                TAG,
                new FolderActionView.Callback() {
                    @Override
                    public void onCreate(String name) {
                        Log.d(TAG, "CreateFolderView → onCreate: " + name + " (stub)");
                        hideFolderActionPopup();
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "CreateFolderView → onCancel");
                        hideFolderActionPopup();
                    }
                }
        );
    }

    /* ---------------- Back ---------------- */

    @Override
    public boolean onBackPressed() {
        if (folderActionView != null && folderActionView.isVisible()) {
            Log.d(TAG, "onBackPressed() → dismiss popup");
            hideFolderActionPopup();
            return true;
        }
        return false;
    }

    @Override
    public void release() {
        Log.d(TAG, "release");
    }
}
