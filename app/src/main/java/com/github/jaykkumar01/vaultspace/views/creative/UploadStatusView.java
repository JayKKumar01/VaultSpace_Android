package com.github.jaykkumar01.vaultspace.views.creative;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.github.jaykkumar01.vaultspace.R;

public class UploadStatusView extends CardView {

    public enum State {
        UPLOADING,
        COMPLETED,
        FAILED
    }

    // Views
    private TextView tvMediaInfo;
    private TextView tvFailedCount;
    private View ivWarning;
    private View progressBar;
    private View progressFill;
    private TextView tvUploadingState;
    private TextView tvUploadRatio;
    private TextView tvCancelUpload;

    // State
    private int photoCount;
    private int videoCount;
    private int totalCount;
    private int uploadedCount;
    private int failedCount;
    private State state = State.UPLOADING;

    private float progressFraction;

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
        setCardBackgroundColor(getContext().getColor(R.color.vs_surface_soft_translucent));

        LayoutInflater.from(getContext())
                .inflate(R.layout.view_upload_status, this, true);

        tvMediaInfo = findViewById(R.id.tvUploadMediaInfo);
        tvFailedCount = findViewById(R.id.tvFailedCount);
        ivWarning = findViewById(R.id.ivUploadWarning);
        progressBar = findViewById(R.id.uploadProgressBar);
        progressFill = findViewById(R.id.uploadProgressFill);
        tvUploadingState = findViewById(R.id.tvUploadingState);
        tvUploadRatio = findViewById(R.id.tvUploadRatio);
        tvCancelUpload = findViewById(R.id.tvCancelUpload);

        progressBar.addOnLayoutChangeListener(
                (v, l, t, r, b, ol, ot, or, ob) -> applyProgress()
        );

        render();
    }

    /* ================= Public API ================= */

    public void show() {
        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }

    public void setMediaCounts(int photos, int videos) {
        this.photoCount = Math.max(0, photos);
        this.videoCount = Math.max(0, videos);
        renderMediaInfo();
    }

    public void setTotalCount(int total) {
        this.totalCount = Math.max(0, total);
        updateProgress();
    }

    public void setUploadedCount(int uploaded) {
        this.uploadedCount = Math.max(0, uploaded);
        updateProgress();
    }

    public void setFailedCount(int failed) {
        this.failedCount = Math.max(0, failed);
        renderFailures();
    }

    public void setState(State state) {
        this.state = state;
        renderState();
    }

    public void setOnCancelClickListener(OnClickListener l) {
        tvCancelUpload.setOnClickListener(l);
    }

    /* ================= Rendering ================= */

    private void render() {
        renderMediaInfo();
        renderFailures();
        renderState();
        updateProgress();
    }

    private void renderMediaInfo() {
        tvMediaInfo.setText(
                photoCount + " photos · " + videoCount + " videos"
        );
    }

    private void renderFailures() {
        boolean show = failedCount > 0;
        ivWarning.setVisibility(show ? VISIBLE : GONE);
        tvFailedCount.setVisibility(show ? VISIBLE : GONE);
        if (show) {
            tvFailedCount.setText(String.valueOf(failedCount));
        }
    }

    private void renderState() {
        switch (state) {
            case UPLOADING:
                tvUploadingState.setText("Uploading…");
                tvCancelUpload.setVisibility(VISIBLE);
                break;

            case COMPLETED:
                tvUploadingState.setText("Completed");
                tvCancelUpload.setVisibility(GONE);
                break;

            case FAILED:
                tvUploadingState.setText("Upload failed");
                tvCancelUpload.setVisibility(VISIBLE);
                break;
        }
    }

    private void updateProgress() {
        if (totalCount <= 0) {
            progressFraction = 0f;
        } else {
            progressFraction = clamp01(uploadedCount / (float) totalCount);
        }

        tvUploadRatio.setText(uploadedCount + " / " + totalCount);
        applyProgress();
    }

    private void applyProgress() {
        int width = progressBar.getWidth();
        if (width <= 0) return;

        progressFill.getLayoutParams().width =
                (int) (width * progressFraction);
        progressFill.requestLayout();
    }

    /* ================= Utils ================= */

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
