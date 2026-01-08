package com.github.jaykkumar01.vaultspace.dashboard;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.views.FolderActionView;
import com.github.jaykkumar01.vaultspace.views.EmptyStateView;
import com.github.jaykkumar01.vaultspace.views.LoadingStateView;

/* ---------------- Contract ---------------- */

interface VaultSectionUi {
    void show();
    boolean onBackPressed();
    default void release() {}
}

/* ---------------- Base Helper ---------------- */

public abstract class BaseVaultSectionUiHelper implements VaultSectionUi {

    protected final Context context;
    protected final FrameLayout container;

    protected LoadingStateView loadingView;
    protected EmptyStateView emptyView;
    protected View contentView;

    protected FolderActionView folderActionView;

    protected BaseVaultSectionUiHelper(Context context, FrameLayout container) {
        this.context = context;
        this.container = container;
        initBaseUi();
    }

    private void initBaseUi() {
        container.removeAllViews();

        loadingView = new LoadingStateView(context);
        emptyView = new EmptyStateView(context);
        contentView = LayoutInflater.from(context)
                .inflate(R.layout.view_mock_content, container, false);

        container.addView(loadingView);
        container.addView(emptyView);
        container.addView(contentView);

        showLoading();
    }

    /* ---------------- States ---------------- */

    protected void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
    }

    protected void showEmpty() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
    }

    protected void showContent() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
    }

    /* ---------------- Overlay ---------------- */

    protected void showCreatePopup(
            String title,
            String hint,
            String positiveText,
            String debugOwner,
            FolderActionView.Callback callback
    ) {
        Activity activity = (Activity) context;
        FrameLayout root = activity.findViewById(android.R.id.content);

        if (folderActionView == null) {
            folderActionView = new FolderActionView(context);
            root.addView(folderActionView);
        }

        folderActionView.show(title, hint, positiveText, debugOwner, callback);
    }

    protected void hideFolderActionPopup() {
        if (folderActionView != null && folderActionView.isVisible()) {
            folderActionView.hide();
        }
    }
}
