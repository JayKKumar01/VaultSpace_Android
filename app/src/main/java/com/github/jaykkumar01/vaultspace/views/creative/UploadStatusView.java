package com.github.jaykkumar01.vaultspace.views.creative;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.github.jaykkumar01.vaultspace.R;

public class UploadStatusView extends CardView {

    /* ================= Upload Headline Texts ================= */

    private static final String TEXT_STARTING = "Just getting started";

    private static final String TEXT_PROGRESS_LOW = "Making progress";

    private static final String TEXT_PROGRESS_HALF = "More than halfway";

    private static final String TEXT_ALMOST_DONE = "Almost there";

    private static final String TEXT_ONE_LEFT = "Just one more to go";

    private static final String TEXT_COMPLETED = "All memories are safe";

    /* ================= Action Labels ================= */

    private static final String ACTION_CANCEL = "Stop";
    private static final String ACTION_RETRY = "Try Again";
    private static final String ACTION_OK = "Done";

    /* ================= Formatting ================= */

    private static final String MEDIA_SEPARATOR = " photos · ";
    private static final String MEDIA_SUFFIX = " videos";
    private static final String RATIO_SEPARATOR = " / ";
    private String textUploading;


    /* ================= Internal State ================= */

    private enum State {
        UPLOADING,
        FAILED,
        COMPLETED
    }

    /* ================= Views ================= */

    private TextView tvMediaInfo;
    private TextView tvFailedCount;
    private View ivWarning;

    private View progressBar;
    private View progressFill;
    private TextView tvUploadRatio;
    private TextView tvUploadingState;

    private AppCompatButton btnAction;

    /* ================= Data ================= */

    private int photoCount;
    private int videoCount;
    private int totalCount;
    private int uploadedCount;
    private int failedCount;

    private float progressFraction;
    private boolean progressBarReady;


    /* ================= Constructors ================= */

    public UploadStatusView(Context context) {
        this(context, null);
    }

    public UploadStatusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setRadius(dp(8));
        setCardElevation(dp(6));
        setUseCompatPadding(true);
        setCardBackgroundColor(
                getContext().getColor(R.color.vs_surface_soft_translucent)
        );

        LayoutInflater.from(getContext())
                .inflate(R.layout.view_upload_status, this, true);

        bindViews();

        hide();
    }


    private void bindViews() {
        tvMediaInfo = findViewById(R.id.tvUploadMediaInfo);
        tvFailedCount = findViewById(R.id.tvFailedCount);
        ivWarning = findViewById(R.id.ivUploadWarning);

        progressBar = findViewById(R.id.uploadProgressBar);
        progressFill = findViewById(R.id.uploadProgressFill);
        tvUploadRatio = findViewById(R.id.tvUploadRatio);
        tvUploadingState = findViewById(R.id.tvUploadingState);

        btnAction = findViewById(R.id.tvAction);

        progressFill.post(() -> {
            progressFill.setPivotX(0f);
            progressFill.setPivotY(progressFill.getHeight() / 2f);
        });

    }

    /* ================= Visibility ================= */

    public void show() {
        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }

    /* ================= Data APIs ================= */

    public void setMediaCounts(int photos, int videos) {
        photoCount = Math.max(0, photos);
        videoCount = Math.max(0, videos);
        renderMediaInfo();
    }

    public void setTotalCount(int total) {
        totalCount = Math.max(0, total);
        updateProgress();
    }

    public void setUploadedCount(int uploaded) {
        uploadedCount = Math.max(0, uploaded);
        updateProgress();
    }

    public void setFailedCount(int failed) {
        failedCount = Math.max(0, failed);
        renderFailures();
    }

    /* ================= State Rendering APIs ================= */

    public void renderUploading(OnClickListener onAction, int completed, int total) {
        textUploading = resolveProgressText(completed, total);
        setState(State.UPLOADING);
        configureAction(ACTION_CANCEL, R.drawable.bg_upload_action_cancel, onAction);
    }

    public void renderFailed(OnClickListener onAction) {
        setState(State.FAILED);
        configureAction(ACTION_RETRY, R.drawable.bg_upload_action_retry, onAction);
    }

    public void renderCompleted(OnClickListener onAction) {
        setState(State.COMPLETED);
        configureAction(ACTION_OK, R.drawable.bg_upload_action_ok, onAction);
    }

    private String resolveProgressText(int completed, int total) {

        if (total <= 0) {
            return TEXT_STARTING;
        }

        if (completed <= 0) {
            return TEXT_STARTING;
        }

        int remaining = total - completed;

        if (remaining == 1) {
            return TEXT_ONE_LEFT;
        }

        float fraction = completed / (float) total;

        if (fraction >= 0.8f) {
            return TEXT_ALMOST_DONE;
        }

        if (fraction >= 0.5f) {
            return TEXT_PROGRESS_HALF;
        }

        return TEXT_PROGRESS_LOW;
    }


    /* ================= Internal Rendering ================= */

    private void setState(State newState) {

        switch (newState) {
            case UPLOADING:
                tvUploadingState.setText(textUploading);
                break;

            case FAILED:
                tvUploadingState.setText(TEXT_ALMOST_DONE);
                break;

            case COMPLETED:
                tvUploadingState.setText(TEXT_COMPLETED);
                break;
        }

        btnAction.setVisibility(VISIBLE);
        renderFailures();
        updateProgress();
    }

    private void configureAction(
            String text,
            int backgroundRes,
            @Nullable OnClickListener action
    ) {
        btnAction.setText(text);
        btnAction.setBackgroundResource(backgroundRes);
        btnAction.setOnClickListener(action);
    }

    private void renderMediaInfo() {
        String mediaInfo = photoCount + MEDIA_SEPARATOR + videoCount + MEDIA_SUFFIX;
        tvMediaInfo.setText(mediaInfo);
    }

    private void renderFailures() {
        boolean show = failedCount > 0;

        ivWarning.setVisibility(show ? VISIBLE : GONE);
        tvFailedCount.setVisibility(show ? VISIBLE : GONE);

        if (show) {
            tvFailedCount.setText(String.valueOf(failedCount));
        }
    }

    /* ================= Progress ================= */

    private void updateProgress() {
        progressFraction = totalCount <= 0
                ? 0f
                : clamp01(uploadedCount / (float) totalCount);

        String uploadRatio = uploadedCount + RATIO_SEPARATOR + totalCount;
        tvUploadRatio.setText(uploadRatio);
        applyProgress();
    }

    private void applyProgress() {
        progressFill.setScaleX(progressFraction);
    }


    /* ================= Utils ================= */

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
