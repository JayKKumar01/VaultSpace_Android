package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import com.github.jaykkumar01.vaultspace.interfaces.VaultSectionUi;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.states.EmptyStateView;
import com.github.jaykkumar01.vaultspace.views.states.LoadingStateView;

/* ---------------- Contract ---------------- */



/* ---------------- Base Helper ---------------- */

public abstract class BaseVaultSectionUiHelper implements VaultSectionUi {

    protected final Context context;
    protected final FrameLayout container;

    protected LoadingStateView loadingView;
    protected EmptyStateView emptyView;
    protected View contentView;

//    protected FolderActionView folderActionView;
//    protected ItemActionView itemActionView;

    // ✅ NEW: Confirm abstraction
    protected ModalHost hostView;

    protected BaseVaultSectionUiHelper(Context context, FrameLayout container, ModalHost hostView) {
        this.context = context;
        this.container = container;
        this.hostView = hostView;
        initBaseUi();
    }

    /* ---------------- Init ---------------- */

    private void initBaseUi() {
        container.removeAllViews();

        loadingView = new LoadingStateView(context);
        emptyView = new EmptyStateView(context);
        contentView = createContentView(context);

        container.addView(loadingView);
        container.addView(emptyView);
        container.addView(contentView);

        showLoading();
    }

    /**
     * Section-specific content view (Albums / Files)
     */
    protected abstract View createContentView(Context context);

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
}
