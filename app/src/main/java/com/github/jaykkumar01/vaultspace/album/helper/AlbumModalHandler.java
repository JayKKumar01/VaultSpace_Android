package com.github.jaykkumar01.vaultspace.album.helper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmSpec;
import com.github.jaykkumar01.vaultspace.views.popups.confirm.ConfirmView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums.DismissResult;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.uploadfailures.UploadFailureListSpec;

import java.util.List;

/**
 * AlbumModalHandler
 * <p>
 * Coordinates album-related modal decisions.
 * Does NOT handle loading, error classification, or UI state.
 */
public final class AlbumModalHandler {

    private final ModalHost modalHost;

    private final ConfirmSpec retryLoadSpec;
    private final ConfirmSpec cancelUploadSpec;
    private final UploadFailureListSpec uploadFailureSpec;

    public AlbumModalHandler(@NonNull ModalHost modalHost) {
        this.modalHost = modalHost;

        retryLoadSpec = createRetryLoadSpec();
        cancelUploadSpec = createCancelUploadSpec();
        uploadFailureSpec = createUploadFailureSpec();

    }

    /* ---------- Public API ---------- */

    public void showRetryLoad(Runnable onRetry, Runnable onExit) {
        retryLoadSpec.setPositiveAction(onRetry);
        retryLoadSpec.setNegativeAction(onExit);
        modalHost.request(retryLoadSpec);
    }

    public void showCancelConfirm(Runnable onCancel) {
        cancelUploadSpec.setPositiveAction(onCancel);
        modalHost.request(cancelUploadSpec);
    }

    public void showUploadFailures(@NonNull List<UploadFailureEntity> failures, @NonNull Runnable onOk) {
        int count = failures.size();

        String title = count == 1
                ? "1 upload failed"
                : count + " uploads failed";

        uploadFailureSpec.setTitle(title);
        uploadFailureSpec.setFailures(failures);
        uploadFailureSpec.setOnOk(onOk);

        modalHost.request(uploadFailureSpec);
    }


    public void dismissAll() {
        modalHost.dismiss(retryLoadSpec, DismissResult.SYSTEM);
        modalHost.dismiss(cancelUploadSpec, DismissResult.SYSTEM);
        modalHost.dismiss(uploadFailureSpec,DismissResult.SYSTEM);
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
                "Your uploaded moments are safe. The rest won’t upload.",
                true,
                ConfirmView.RISK_WARNING,
                null,
                null // negative handled via dismiss
        );
        spec.setPositiveText("Cancel");
        spec.setNegativeText("Continue");
        return spec;
    }

    private static UploadFailureListSpec createUploadFailureSpec() {
        return new UploadFailureListSpec();
    }
}
