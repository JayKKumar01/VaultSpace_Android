package com.github.jaykkumar01.vaultspace.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.views.CreateFolderView;
import com.github.jaykkumar01.vaultspace.views.EmptyStateView;
import com.github.jaykkumar01.vaultspace.views.LoadingStateView;

/* ---------------- Contract ---------------- */

interface VaultSectionUi {
    void show();
    default void release() {}
}

/* ---------------- Base UI Helper ---------------- */

public abstract class BaseVaultSectionUiHelper implements VaultSectionUi {

    protected final Context context;
    protected final FrameLayout container;

    protected LoadingStateView loadingView;
    protected EmptyStateView emptyView;
    protected View contentView;
    protected CreateFolderView createFolderView;

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
        createFolderView = new CreateFolderView(context);

        container.addView(loadingView);
        container.addView(emptyView);
        container.addView(contentView);
        container.addView(createFolderView);

        showLoading();
    }

    /* ---------------- State helpers ---------------- */

    protected void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
        createFolderView.setVisibility(View.GONE);
    }

    protected void showEmpty() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        createFolderView.setVisibility(View.GONE);
    }

    protected void showContent() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.VISIBLE);
        createFolderView.setVisibility(View.GONE);
    }

    protected void showCreateFolder(String hint, CreateFolderView.Callback callback) {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
        createFolderView.show(hint, callback);
    }

    protected void hideCreateFolder() {
        createFolderView.hide();
    }
}
