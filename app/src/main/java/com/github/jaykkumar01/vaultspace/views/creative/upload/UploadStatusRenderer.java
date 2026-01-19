package com.github.jaykkumar01.vaultspace.views.creative.upload;

import android.view.View;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.R;

import java.util.Arrays;

/**
 * UploadStatusRenderer
 *
 * Optimized, diff-aware renderer.
 * Emits a new RenderModel ONLY when something actually changes.
 */
public final class UploadStatusRenderer {

    /* ================= Text ================= */

    private static final String TEXT_STARTING = "Just getting started";
    private static final String TEXT_PROGRESS_LOW = "Making progress";
    private static final String TEXT_PROGRESS_HALF = "More than halfway";
    private static final String TEXT_ALMOST_DONE = "Almost there";
    private static final String TEXT_ONE_LEFT = "Just one more to go";
    private static final String TEXT_COMPLETED = "All memories are safe";
    private static final String TEXT_NO_ACCESS = "Some items need access permission";

    /* ================= Actions ================= */

    private static final String ACTION_CANCEL = "Stop";
    private static final String ACTION_RETRY = "Try Again";
    private static final String ACTION_OK = "Done";
    private static final String ACTION_INFO = "See Info";

    /* ================= Formatting ================= */

    private static final String MEDIA_SEPARATOR = " photos · ";
    private static final String MEDIA_SUFFIX = " videos";
    private static final String RATIO_SEPARATOR = " / ";

    /* ================= Cached State ================= */

    private int photoCount = -1;
    private int videoCount = -1;
    private int totalCount = -1;
    private int uploadedCount = -1;
    private int failedCount = -1;
    private int noAccessCount = -1;

    private float[] lastFractions = new float[]{-1f, -1f, -1f};

    private UploadStatusRenderModel lastModel;

    /* ================= Public Data API ================= */

    public void setMediaCounts(int photos, int videos) {
        photoCount = Math.max(0, photos);
        videoCount = Math.max(0, videos);
    }

    public void setTotalCount(int total) {
        totalCount = Math.max(0, total);
    }

    public void setUploadedCount(int uploaded) {
        uploadedCount = Math.max(0, uploaded);
    }

    public void setFailedCount(int failed) {
        failedCount = Math.max(0, failed);
    }

    public void setNoAccessCount(int noAccess) {
        noAccessCount = Math.max(0, noAccess);
    }

    /* ================= Render Entry Points ================= */

    @NonNull
    public UploadStatusRenderModel renderUploading(
            @NonNull View.OnClickListener action,
            int completed,
            int total
    ) {
        return render(
                UploadStatusRenderModel.State.UPLOADING,
                resolveProgressText(completed, total),
                ACTION_CANCEL,
                R.drawable.bg_upload_action_cancel,
                action
        );
    }

    @NonNull
    public UploadStatusRenderModel renderFailed(
            @NonNull View.OnClickListener action
    ) {
        return render(
                UploadStatusRenderModel.State.FAILED_RETRYABLE,
                TEXT_ALMOST_DONE,
                ACTION_RETRY,
                R.drawable.bg_upload_action_retry,
                action
        );
    }

    @NonNull
    public UploadStatusRenderModel renderNoAccess(
            @NonNull View.OnClickListener action
    ) {
        return render(
                UploadStatusRenderModel.State.FAILED_NO_ACCESS,
                TEXT_NO_ACCESS,
                ACTION_INFO,
                R.drawable.bg_upload_action_info,
                action
        );
    }

    @NonNull
    public UploadStatusRenderModel renderCompleted(
            @NonNull View.OnClickListener action
    ) {
        return render(
                UploadStatusRenderModel.State.COMPLETED,
                TEXT_COMPLETED,
                ACTION_OK,
                R.drawable.bg_upload_action_ok,
                action
        );
    }

    /* ================= Core Render Logic ================= */

    private UploadStatusRenderModel render(
            UploadStatusRenderModel.State state,
            String uploadingText,
            String actionText,
            int actionBg,
            View.OnClickListener actionClick
    ) {

        float[] fractions = computeFractions();

        boolean showRetry = failedCount > 0 && failedCount > noAccessCount;
        boolean showNoAccess = noAccessCount > 0;

        UploadStatusRenderModel model = new UploadStatusRenderModel(
                state,
                buildMediaInfoText(),
                uploadingText,
                buildRatioText(),
                state == UploadStatusRenderModel.State.FAILED_RETRYABLE,
                showRetry,
                showNoAccess,
                showRetry ? String.valueOf(failedCount - noAccessCount) : "",
                showNoAccess ? String.valueOf(noAccessCount) : "",
                actionText,
                actionBg,
                actionClick,
                fractions
        );

        if (isSameAsLast(model)) return lastModel;

        lastModel = model;
        return model;
    }

    /* ================= Diffing ================= */

    private boolean isSameAsLast(UploadStatusRenderModel m) {
        if (lastModel == null) return false;

        if (lastModel.state != m.state) return false;
        if (!lastModel.mediaInfoText.equals(m.mediaInfoText)) return false;
        if (!lastModel.uploadingStateText.equals(m.uploadingStateText)) return false;
        if (!lastModel.uploadRatioText.equals(m.uploadRatioText)) return false;

        if (lastModel.showDismiss != m.showDismiss) return false;
        if (lastModel.showRetryWarning != m.showRetryWarning) return false;
        if (lastModel.showNoAccessWarning != m.showNoAccessWarning) return false;

        if (!lastModel.failedCountText.equals(m.failedCountText)) return false;
        if (!lastModel.noAccessCountText.equals(m.noAccessCountText)) return false;

        if (!Arrays.equals(lastFractions, m.progressFractions)) return false;

        lastFractions = m.progressFractions.clone();
        return true;
    }

    /* ================= Calculations ================= */

    private float[] computeFractions() {
        float success = 0f, retry = 0f, noAccess = 0f;

        if (totalCount > 0) {
            success = clamp01(uploadedCount / (float) totalCount);
            noAccess = clamp01(noAccessCount / (float) totalCount);
            retry = clamp01((failedCount - noAccessCount) / (float) totalCount);

            float remaining = 1f - success - noAccess;
            if (retry > remaining) retry = remaining;
        }

        return new float[]{ success, retry, noAccess };
    }

    private String buildMediaInfoText() {
        return photoCount + MEDIA_SEPARATOR + videoCount + MEDIA_SUFFIX;
    }

    private String buildRatioText() {
        return uploadedCount + RATIO_SEPARATOR + totalCount;
    }

    private String resolveProgressText(int completed, int total) {
        if (total <= 0 || completed <= 0) return TEXT_STARTING;
        int remaining = total - completed;
        if (remaining == 1) return TEXT_ONE_LEFT;
        float f = completed / (float) total;
        if (f >= 0.8f) return TEXT_ALMOST_DONE;
        if (f >= 0.5f) return TEXT_PROGRESS_HALF;
        return TEXT_PROGRESS_LOW;
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
