package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import com.github.jaykkumar01.vaultspace.interfaces.VaultSectionUi;
import com.github.jaykkumar01.vaultspace.views.popups.ConfirmActionView;
import com.github.jaykkumar01.vaultspace.views.states.EmptyStateView;
import com.github.jaykkumar01.vaultspace.views.popups.FolderActionView;
import com.github.jaykkumar01.vaultspace.views.popups.ItemActionView;
import com.github.jaykkumar01.vaultspace.views.states.LoadingStateView;

/* ---------------- Contract ---------------- */



/* ---------------- Base Helper ---------------- */

public abstract class BaseVaultSectionUiHelper implements VaultSectionUi {

    protected final Context context;
    protected final FrameLayout container;

    protected LoadingStateView loadingView;
    protected EmptyStateView emptyView;
    protected View contentView;

    protected FolderActionView folderActionView;
    protected ItemActionView itemActionView;

    // ✅ NEW: Confirm abstraction
    protected ConfirmActionView confirmActionView;

    protected BaseVaultSectionUiHelper(Context context, FrameLayout container) {
        this.context = context;
        this.container = container;
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

    /* ---------------- Folder Action ---------------- */

    protected void showFolderActionPopup(
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

    /* ---------------- Item Action ---------------- */

    protected void showItemActionPopup(
            String title,
            String[] actions,
            String debugOwner,
            ItemActionView.Callback callback
    ) {
        Activity activity = (Activity) context;
        FrameLayout root = activity.findViewById(android.R.id.content);

        if (itemActionView == null) {
            itemActionView = new ItemActionView(context);
            root.addView(itemActionView);
        }

        itemActionView.show(title, actions, debugOwner, callback);
    }

    protected void hideItemActionPopup() {
        if (itemActionView != null && itemActionView.isVisible()) {
            itemActionView.hide();
        }
    }

    /* ---------------- Confirm Action (NEW) ---------------- */

    protected void showConfirmActionPopup(
            String title,
            String message,
            String positiveText,
            int risk,
            String debugOwner,
            ConfirmActionView.Callback callback
    ) {
        Activity activity = (Activity) context;
        FrameLayout root = activity.findViewById(android.R.id.content);

        if (confirmActionView == null) {
            confirmActionView = new ConfirmActionView(context);
            root.addView(confirmActionView);
        }

        confirmActionView.show(
                title,
                message,
                positiveText,
                risk,
                debugOwner,
                callback
        );
    }

    protected void hideConfirmActionPopup() {
        if (confirmActionView != null && confirmActionView.isVisible()) {
            confirmActionView.hide();
        }
    }
}
