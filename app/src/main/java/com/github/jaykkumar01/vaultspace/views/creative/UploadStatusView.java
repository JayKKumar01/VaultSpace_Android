package com.github.jaykkumar01.vaultspace.views.creative;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.R;

public class UploadStatusView extends FrameLayout {

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

    /* ================= State ================= */

    private enum State {
        UPLOADING,
        FAILED_RETRYABLE,
        FAILED_NO_ACCESS,
        COMPLETED
    }

    /* ================= Views ================= */

    private FrameLayout cardContainer;
    private ImageView ivDismiss;

    private TextView tvMediaInfo;
    private TextView tvFailedCount;
    private TextView tvNoAccessCount;
    private TextView tvUploadRatio;
    private TextView tvUploadingState;

    private View ivWarning;
    private View ivNoAccessWarning;

    private AppCompatButton btnAction;
    private MultiSegmentProgressBar progressBar;

    /* ================= Data ================= */

    private final float[] progressFractions = new float[3];

    private int photoCount;
    private int videoCount;
    private int totalCount;
    private int uploadedCount;
    private int failedCount;
    private int noAccessCount;
    private int dismissOverlap;

    private String textUploading;

    /* ================= Constructors ================= */

    public UploadStatusView(Context context) {
        this(context, null);
    }

    public UploadStatusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /* ================= Init ================= */

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);

        cardContainer = new FrameLayout(getContext());
        cardContainer.setClipChildren(false);
        cardContainer.setClipToPadding(false);
        cardContainer.setElevation(dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(ContextCompat.getColor(getContext(), R.color.vs_surface_soft_translucent));
        cardContainer.setBackground(bg);

        dismissOverlap = (int) dp(14);

        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardContainer.setLayoutParams(lp);

        LayoutInflater.from(getContext()).inflate(R.layout.view_upload_status, cardContainer, true);
        addView(cardContainer);

        ivDismiss = createDismissImage();
        bindViews();
        hide();
    }

    /* ================= Binding ================= */

    private void bindViews() {
        tvMediaInfo = cardContainer.findViewById(R.id.tvUploadMediaInfo);
        tvFailedCount = cardContainer.findViewById(R.id.tvFailedCount);
        tvNoAccessCount = cardContainer.findViewById(R.id.tvNoAccessCount);
        tvUploadRatio = cardContainer.findViewById(R.id.tvUploadRatio);
        tvUploadingState = cardContainer.findViewById(R.id.tvUploadingState);

        ivWarning = cardContainer.findViewById(R.id.ivUploadWarning);
        ivNoAccessWarning = cardContainer.findViewById(R.id.ivNoAccessWarning);

        progressBar = cardContainer.findViewById(R.id.uploadProgressBar);
        progressBar.setColors(new int[]{
                ContextCompat.getColor(getContext(), R.color.vs_accent_primary),
                ContextCompat.getColor(getContext(), R.color.vs_warning),
                ContextCompat.getColor(getContext(), R.color.vs_danger_strong)
        });

        btnAction = cardContainer.findViewById(R.id.tvAction);
    }

    /* ================= Visibility ================= */

    private void show() {
        renderMediaInfo();
        renderFailures();
        renderNoAccessBadge();
        updateProgress();
        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }

    /* ================= Public API ================= */

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

    /* ================= State Rendering ================= */

    public void renderUploading(OnClickListener action, int completed, int total) {
        textUploading = resolveProgressText(completed, total);
        setState(State.UPLOADING);
        configureAction(ACTION_CANCEL, R.drawable.bg_upload_action_cancel, action);
    }

    public void renderFailed(OnClickListener action) {
        setState(State.FAILED_RETRYABLE);
        configureAction(ACTION_RETRY, R.drawable.bg_upload_action_retry, action);
    }

    public void renderNoAccess(OnClickListener action) {
        setState(State.FAILED_NO_ACCESS);
        configureAction(ACTION_INFO, R.drawable.bg_upload_action_info, action);
    }

    public void renderCompleted(OnClickListener action) {
        setState(State.COMPLETED);
        configureAction(ACTION_OK, R.drawable.bg_upload_action_ok, action);
    }

    /* ================= Rendering ================= */

    private void configureAction(String text, int bg, @Nullable OnClickListener action) {
        btnAction.setText(text);
        btnAction.setBackgroundResource(bg);
        btnAction.setOnClickListener(action);
        show();
    }

    private void setState(State state) {
        switch (state) {
            case UPLOADING:
                tvUploadingState.setText(textUploading);
                ivDismiss.setVisibility(GONE);
                updateCardOffsets(false);
                break;
            case FAILED_RETRYABLE:
                tvUploadingState.setText(TEXT_ALMOST_DONE);
                ivDismiss.setVisibility(VISIBLE);
                updateCardOffsets(true);
                break;
            case FAILED_NO_ACCESS:
                tvUploadingState.setText(TEXT_NO_ACCESS);
                ivDismiss.setVisibility(GONE);
                updateCardOffsets(false);
                break;
            case COMPLETED:
                tvUploadingState.setText(TEXT_COMPLETED);
                ivDismiss.setVisibility(GONE);
                updateCardOffsets(false);
                break;
        }
        btnAction.setVisibility(VISIBLE);
        renderFailures();
        renderNoAccessBadge();
    }

    private void renderMediaInfo() {
        tvMediaInfo.setText(photoCount + MEDIA_SEPARATOR + videoCount + MEDIA_SUFFIX);
    }

    private void renderFailures() {
        boolean show = failedCount > 0 && failedCount > noAccessCount;
        ivWarning.setVisibility(show ? VISIBLE : GONE);
        tvFailedCount.setVisibility(show ? VISIBLE : GONE);
        if (show) tvFailedCount.setText(String.valueOf(failedCount - noAccessCount));
    }

    private void renderNoAccessBadge() {
        boolean show = noAccessCount > 0;
        ivNoAccessWarning.setVisibility(show ? VISIBLE : GONE);
        tvNoAccessCount.setVisibility(show ? VISIBLE : GONE);
        if (show) tvNoAccessCount.setText(String.valueOf(noAccessCount));
    }

    /* ================= Progress ================= */

    private void updateProgress() {
        float success = 0f, retry = 0f, noAccess = 0f;
        if (totalCount > 0) {
            success = clamp01(uploadedCount / (float) totalCount);
            noAccess = clamp01(noAccessCount / (float) totalCount);
            retry = clamp01((failedCount - noAccessCount) / (float) totalCount);
            float remaining = 1f - success - noAccess;
            if (retry > remaining) retry = remaining;
        }
        progressFractions[0] = success;
        progressFractions[1] = retry;
        progressFractions[2] = noAccess;
        progressBar.setFractions(progressFractions);
        tvUploadRatio.setText(uploadedCount + RATIO_SEPARATOR + totalCount);
    }

    /* ================= Helpers ================= */

    private void updateCardOffsets(boolean visible) {
        LayoutParams lp = (LayoutParams) cardContainer.getLayoutParams();
        int target = visible ? dismissOverlap : 0;
        if (lp.topMargin != target || lp.rightMargin != target) {
            lp.topMargin = target;
            lp.rightMargin = target;
            cardContainer.setLayoutParams(lp);
        }
    }

    private ImageView createDismissImage() {
        ImageView iv = new ImageView(getContext());
        iv.setImageResource(R.drawable.ic_close);
        iv.setVisibility(GONE);
        iv.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_dismiss_circle));
        iv.setElevation(dp(10));
        int p = (int) dp(6), s = (int) dp(36);
        iv.setPadding(p, p, p, p);
        LayoutParams lp = new LayoutParams(s, s);
        lp.gravity = Gravity.TOP | Gravity.END;
        iv.setLayoutParams(lp);
        iv.setOnClickListener(v -> hide());
        addView(iv);
        expandTouchArea(iv, 16);
        return iv;
    }

    private void expandTouchArea(View view, int extraDp) {
        View parent = (View) view.getParent();
        if (parent == null) return;
        parent.post(() -> {
            Rect r = new Rect();
            view.getHitRect(r);
            int e = (int) dp(extraDp);
            r.top -= e; r.bottom += e; r.left -= e; r.right += e;
            parent.setTouchDelegate(new TouchDelegate(r, view));
        });
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

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
