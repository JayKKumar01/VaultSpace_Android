package com.github.jaykkumar01.vaultspace.views.creative.upload;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable snapshot describing how UploadStatusView should look.
 * This class contains NO logic and NO rendering code.
 */
public final class UploadStatusRenderModel {

    public enum State {
        UPLOADING,
        FAILED_RETRYABLE,
        FAILED_NO_ACCESS,
        COMPLETED
    }

    @NonNull
    public final State state;

    @NonNull
    public final String mediaInfoText;
    @NonNull
    public final String uploadingStateText;
    @NonNull
    public final String uploadRatioText;
    public final boolean showRetryWarning;
    public final boolean showNoAccessWarning;

    @NonNull
    public final String failedCountText;
    @NonNull
    public final String noAccessCountText;

    @NonNull
    public final String actionText;
    public final int actionBackgroundRes;
    @NonNull
    public final View.OnClickListener actionClick;

    @NonNull
    public final float[] progressFractions;
    @Nullable
    public final View.OnClickListener dismissClick;

    public UploadStatusRenderModel(
            @NonNull State state,
            @NonNull String mediaInfoText,
            @NonNull String uploadingStateText,
            @NonNull String uploadRatioText,
            boolean showRetryWarning,
            boolean showNoAccessWarning,
            @NonNull String failedCountText,
            @NonNull String noAccessCountText,
            @NonNull String actionText,
            int actionBackgroundRes,
            @NonNull View.OnClickListener actionClick,
            @Nullable View.OnClickListener dismissClick,
            @NonNull float[] progressFractions
    ) {
        this.state = state;
        this.mediaInfoText = mediaInfoText;
        this.uploadingStateText = uploadingStateText;
        this.uploadRatioText = uploadRatioText;
        this.showRetryWarning = showRetryWarning;
        this.showNoAccessWarning = showNoAccessWarning;
        this.failedCountText = failedCountText;
        this.noAccessCountText = noAccessCountText;
        this.actionText = actionText;
        this.actionBackgroundRes = actionBackgroundRes;
        this.actionClick = actionClick;
        this.dismissClick = dismissClick;
        this.progressFractions = progressFractions;
    }
}
