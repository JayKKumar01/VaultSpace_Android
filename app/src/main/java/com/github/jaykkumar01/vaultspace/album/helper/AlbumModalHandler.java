package com.github.jaykkumar01.vaultspace.album.helper;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums.DismissResult;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.list.ListSpec;
import com.github.jaykkumar01.vaultspace.views.popups.loading.LoadingSpec;
import com.github.jaykkumar01.vaultspace.views.popups.uploadfailures.UploadFailureListSpec;

import java.util.Arrays;
import java.util.List;

/**
 * AlbumModalHandler
 * <p>
 * Coordinates album-related modal decisions.
 * Does NOT handle loading, error classification, or UI state.
 */
public final class AlbumModalHandler {

    private final ModalHost modalHost;
    private final LoadingSpec loadingSpec;
    private final ConfirmSpec retryLoadSpec;
    private final ConfirmSpec cancelUploadSpec;
    private final ConfirmSpec deleteMediaSpec;
    private final UploadFailureListSpec uploadFailureSpec;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AlbumModalHandler(@NonNull ModalHost modalHost) {
        this.modalHost = modalHost;

        this.loadingSpec = new LoadingSpec();
        this.retryLoadSpec = createRetryLoadSpec();
        this.cancelUploadSpec = createCancelUploadSpec();
        this.deleteMediaSpec = createDeleteMediaSpec();
        this.uploadFailureSpec = new UploadFailureListSpec();
    }

    /* ---------- Public API ---------- */

    public void showActionList(Runnable onDownload, Runnable onDelete){
        runOnMainThread(() -> modalHost.request(new ListSpec(
                "Media Action",
                Arrays.asList("Download", "Delete"),
                index -> {
                    if (index == 0) {
                        onDownload.run();
                    } else {
                        onDelete.run();
                    }
                },
                null

        )));
    }

    public void showRetryLoad(Runnable onRetry, Runnable onExit) {
        runOnMainThread(() -> {
            retryLoadSpec.setPositiveAction(onRetry);
            retryLoadSpec.setNegativeAction(onExit);
            modalHost.request(retryLoadSpec);
        });
    }

    public void showCancelConfirm(Runnable onPositive) {
        runOnMainThread(() -> {
            cancelUploadSpec.setPositiveAction(onPositive);
            modalHost.request(cancelUploadSpec);
        });
    }

    public void showDeleteConfirm(Runnable onPositive) {
        runOnMainThread(() -> {
            deleteMediaSpec.setPositiveAction(onPositive);
            modalHost.request(deleteMediaSpec);
        });
    }


    public void showUploadFailures(
            @NonNull List<UploadSelection> failures,
            @NonNull Runnable onOk
    ) {
        runOnMainThread(() -> {
            String title =
                    failures.size() == 1
                            ? "This item needs access"
                            : "Some items need access";

            uploadFailureSpec.setTitle(title);
            uploadFailureSpec.setFailures(failures);
            uploadFailureSpec.setOnOk(onOk);

            modalHost.request(uploadFailureSpec);
        });
    }

    public void dismissAll() {
        runOnMainThread(() -> {
            modalHost.dismissAll(DismissResult.SYSTEM);
            modalHost.dismiss(loadingSpec, DismissResult.SYSTEM);
            modalHost.dismiss(retryLoadSpec, DismissResult.SYSTEM);
            modalHost.dismiss(cancelUploadSpec, DismissResult.SYSTEM);
            modalHost.dismiss(deleteMediaSpec, DismissResult.SYSTEM);
            modalHost.dismiss(uploadFailureSpec, DismissResult.SYSTEM);
        });
    }

    public void showLoading() {
        runOnMainThread(() -> modalHost.request(loadingSpec));
    }

    public void clearLoading() {
        runOnMainThread(() -> modalHost.dismiss(loadingSpec, DismissResult.SYSTEM));
    }

    /* ---------- Private Helper ---------- */

    private void runOnMainThread(Runnable action) {
        mainHandler.post(action);
    }

    /* ---------- Spec Builders ---------- */

    private static ConfirmSpec createRetryLoadSpec() {
        ConfirmSpec spec = new ConfirmSpec(
                "Unable to load album",
                "Please check your connection and try again.",
                true,
                ConfirmView.RISK_NEUTRAL,
                null,
                null
        );
        spec.setPositiveText("Retry");
        spec.setNegativeText("Back");
        spec.setCancelable(false);
        return spec;
    }

    private static ConfirmSpec createCancelUploadSpec() {
        ConfirmSpec spec = new ConfirmSpec(
                "Cancel upload?",
                "Your uploaded moments are safe. The rest won't upload.",
                true,
                ConfirmView.RISK_WARNING,
                null,
                null // negative handled via dismiss
        );
        spec.setPositiveText("Cancel");
        spec.setNegativeText("Continue");
        return spec;
    }

    private static ConfirmSpec createDeleteMediaSpec() {
        ConfirmSpec spec = new ConfirmSpec(
                "Delete media?",
                "This action cannot be undone.",
                true,
                ConfirmView.RISK_DESTRUCTIVE,
                null,
                null
        );
        spec.setPositiveText("Delete");
        spec.setNegativeText("Cancel");
        return spec;
    }


    public void showMediaPreview(AlbumMedia m) {
    }
}