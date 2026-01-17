package com.github.jaykkumar01.vaultspace.views.creative;

import android.animation.ValueAnimator;
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
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.github.jaykkumar01.vaultspace.R;

public class UploadStatusView extends FrameLayout {

    /* ================= Upload Headline Texts ================= */

    private static final String TEXT_STARTING = "Just getting started";
    private static final String TEXT_PROGRESS_LOW = "Making progress";
    private static final String TEXT_PROGRESS_HALF = "More than halfway";
    private static final String TEXT_ALMOST_DONE = "Almost there";
    private static final String TEXT_ONE_LEFT = "Just one more to go";
    private static final String TEXT_COMPLETED = "All memories are safe";
    private static final String TEXT_NO_ACCESS = "Some items need access permission";


    /* ================= Action Labels ================= */

    private static final String ACTION_CANCEL = "Stop";
    private static final String ACTION_RETRY = "Try Again";
    private static final String ACTION_OK = "Done";
    private static final String ACTION_INFO = "See Info";


    /* ================= Formatting ================= */

    private static final String MEDIA_SEPARATOR = " photos · ";
    private static final String MEDIA_SUFFIX = " videos";
    private static final String RATIO_SEPARATOR = " / ";

    private String textUploading;

    /* ================= Internal State ================= */

    private enum State {
        UPLOADING,
        FAILED_RETRYABLE,
        FAILED_NO_ACCESS,
        COMPLETED
    }

    /* ================= Root Containers ================= */

    private FrameLayout cardContainer;   // actual card UI
    private ImageView ivDismiss;          // overlay dismiss

    /* ================= Views ================= */

    private TextView tvMediaInfo;
    private TextView tvFailedCount;
    private View ivWarning;
    private TextView tvNoAccessCount;
    private View ivNoAccessWarning;


    private View progressBar;
    private View progressSuccessFill;
    private View progressFailedFill;

    private TextView tvUploadRatio;
    private TextView tvUploadingState;

    private AppCompatButton btnAction;

    /* ================= Data ================= */

    private int photoCount;
    private int videoCount;
    private int totalCount;
    private int uploadedCount;
    private int failedCount;
    private int noAccessCount;

    private int dismissOverlap;


    private float uploadedFraction;
    private float failedFraction;

    private float animatedUploaded;
    private float animatedFailed;

    private ValueAnimator progressAnimator;

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

        // Root must allow overlay
        setClipChildren(false);
        setClipToPadding(false);

        // ---------------- CARD CONTAINER ----------------

        cardContainer = new FrameLayout(getContext());
        cardContainer.setClipChildren(false);
        cardContainer.setClipToPadding(false);
        cardContainer.setElevation(dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(
                ContextCompat.getColor(
                        getContext(),
                        R.color.vs_surface_soft_translucent
                )
        );
        cardContainer.setBackground(bg);

        dismissOverlap = (int) dp(14);

        LayoutParams cardLp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        cardLp.topMargin = 0; // start flush
        cardContainer.setLayoutParams(cardLp);

        // Inflate existing XML INTO cardContainer
        LayoutInflater.from(getContext())
                .inflate(R.layout.view_upload_status, cardContainer, true);

        addView(cardContainer);

        // ---------------- DISMISS BUTTON ----------------

        ivDismiss = createDismissImage();

        bindViews();
        hide();
    }

    private void updateCardOffsets(boolean dismissVisible) {

        LayoutParams lp = (LayoutParams) cardContainer.getLayoutParams();

        int target = dismissVisible ? dismissOverlap : 0;

        boolean changed = false;

        if (lp.topMargin != target) {
            lp.topMargin = target;
            changed = true;
        }

        if (lp.rightMargin != target) {
            lp.rightMargin = target;
            changed = true;
        }

        if (changed) {
            cardContainer.setLayoutParams(lp);
        }
    }



    /* ================= View Binding ================= */

    private void bindViews() {

        tvMediaInfo = cardContainer.findViewById(R.id.tvUploadMediaInfo);
        tvFailedCount = cardContainer.findViewById(R.id.tvFailedCount);
        ivWarning = cardContainer.findViewById(R.id.ivUploadWarning);
        tvNoAccessCount=cardContainer.findViewById(R.id.tvNoAccessCount);
        ivNoAccessWarning=cardContainer.findViewById(R.id.ivNoAccessWarning);


        progressBar = cardContainer.findViewById(R.id.uploadProgressBar);
        progressSuccessFill = cardContainer.findViewById(R.id.uploadProgressSuccessFill);
        progressFailedFill = cardContainer.findViewById(R.id.uploadProgressFailedFill);

        tvUploadRatio = cardContainer.findViewById(R.id.tvUploadRatio);
        tvUploadingState = cardContainer.findViewById(R.id.tvUploadingState);

        btnAction = cardContainer.findViewById(R.id.tvAction);

        // Progress pivots
        progressSuccessFill.post(() -> {
            progressSuccessFill.setPivotX(0f);
            progressSuccessFill.setPivotY(progressSuccessFill.getHeight() / 2f);
        });

        progressFailedFill.post(() -> {
            progressFailedFill.setPivotX(0f);
            progressFailedFill.setPivotY(progressFailedFill.getHeight() / 2f);
        });
    }

    /* ================= Dismiss Button ================= */

    private ImageView createDismissImage() {

        ImageView iv = new ImageView(getContext());
        iv.setImageResource(R.drawable.ic_close);
        iv.setVisibility(GONE);
        iv.setClickable(true);
        iv.setFocusable(true);

        iv.setBackground(
                ContextCompat.getDrawable(
                        getContext(),
                        R.drawable.bg_dismiss_circle
                )
        );

        iv.setElevation(dp(10));

        int padding = (int) dp(6);
        iv.setPadding(padding, padding, padding, padding);

        int size = (int) dp(36);
        LayoutParams lp = new LayoutParams(size, size);
        lp.gravity = Gravity.TOP | Gravity.END;

        iv.setLayoutParams(lp);
        iv.setOnClickListener(v -> hide());

        addView(iv);
        expandTouchArea(iv, 16); // expands to ~60–64dp touch target

        return iv;
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
        updateProgress();
    }

    public void setNoAccessCount(int noAccess){
        noAccessCount = Math.max(0, noAccess);
        renderNoAccessBadge();
    }



    /* ================= State Rendering ================= */

    public void renderUploading(OnClickListener onAction, int completed, int total) {
        textUploading = resolveProgressText(completed, total);
        setState(State.UPLOADING);
        configureAction(ACTION_CANCEL, R.drawable.bg_upload_action_cancel, onAction);
    }

    public void renderFailed(OnClickListener onAction) {
        setState(State.FAILED_RETRYABLE);
        configureAction(ACTION_RETRY, R.drawable.bg_upload_action_retry, onAction);
    }

    public void renderNoAccess(OnClickListener onAction){
        setState(State.FAILED_NO_ACCESS);
        configureAction(ACTION_INFO, R.drawable.bg_upload_action_info, onAction);
    }


    public void renderCompleted(OnClickListener onAction) {
        setState(State.COMPLETED);
        configureAction(ACTION_OK, R.drawable.bg_upload_action_ok, onAction);
    }

    private String resolveProgressText(int completed, int total) {
        if (total <= 0 || completed <= 0) return TEXT_STARTING;

        int remaining = total - completed;
        if (remaining == 1) return TEXT_ONE_LEFT;

        float fraction = completed / (float) total;
        if (fraction >= 0.8f) return TEXT_ALMOST_DONE;
        if (fraction >= 0.5f) return TEXT_PROGRESS_HALF;

        return TEXT_PROGRESS_LOW;
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


    /* ================= Rendering ================= */

    private void configureAction(
            String text,
            int bg,
            @Nullable OnClickListener action
    ) {
        btnAction.setText(text);
        btnAction.setBackgroundResource(bg);
        btnAction.setOnClickListener(action);
    }

    private void renderMediaInfo() {
        String mediaInfo = photoCount + MEDIA_SEPARATOR + videoCount + MEDIA_SUFFIX;
        tvMediaInfo.setText(mediaInfo);
    }

    private void renderFailures() {
        boolean show = failedCount > 0 && failedCount > noAccessCount;
        ivWarning.setVisibility(show ? VISIBLE : GONE);
        tvFailedCount.setVisibility(show ? VISIBLE : GONE);
        if (show) tvFailedCount.setText(String.valueOf(failedCount - noAccessCount));
    }


    private void renderNoAccessBadge(){
        boolean show=noAccessCount>0;
        ivNoAccessWarning.setVisibility(show?VISIBLE:GONE);
        tvNoAccessCount.setVisibility(show?VISIBLE:GONE);
        if(show)tvNoAccessCount.setText(String.valueOf(noAccessCount));
    }


    /* ================= Progress ================= */

    private void updateProgress() {

        if (totalCount <= 0) {
            uploadedFraction = 0f;
            failedFraction = 0f;
        } else {
            uploadedFraction = clamp01(uploadedCount / (float) totalCount);
            failedFraction = clamp01(failedCount / (float) totalCount);
            failedFraction = Math.min(failedFraction, 1f - uploadedFraction);
        }

        String ratio = uploadedCount + RATIO_SEPARATOR + totalCount;
        tvUploadRatio.setText(ratio);

        animateProgress();
    }

    private void animateProgress() {

        if (progressBar.getWidth() == 0) {
            progressBar.post(this::animateProgress);
            return;
        }

        if (progressAnimator != null) {
            progressAnimator.cancel();
        }

        float startUploaded = animatedUploaded;
        float startFailed = animatedFailed;

        progressAnimator = ValueAnimator.ofFloat(0f, 1f);
        progressAnimator.setDuration(180);
        progressAnimator.setInterpolator(new FastOutSlowInInterpolator());

        progressAnimator.addUpdateListener(anim -> {
            float t = (float) anim.getAnimatedValue();
            animatedUploaded = lerp(startUploaded, uploadedFraction, t);
            animatedFailed = lerp(startFailed, failedFraction, t);
            renderAnimatedProgress();
        });

        progressAnimator.start();
    }

    private void renderAnimatedProgress() {

        float barWidth = progressBar.getWidth();
        float uploadedWidth = barWidth * animatedUploaded;

        progressSuccessFill.setScaleX(animatedUploaded);
        progressSuccessFill.setTranslationX(0f);

        progressFailedFill.setScaleX(animatedFailed);
        progressFailedFill.setTranslationX(uploadedWidth);
    }

    private void expandTouchArea(final View view, final int extraDp) {
        final View parent = (View) view.getParent();
        if (parent == null) return;

        parent.post(() -> {
            Rect rect = new Rect();
            view.getHitRect(rect);

            int extra = (int) dp(extraDp);
            rect.top -= extra;
            rect.bottom += extra;
            rect.left -= extra;
            rect.right += extra;

            parent.setTouchDelegate(new TouchDelegate(rect, view));
        });
    }


    /* ================= Utils ================= */

    private float lerp(float s, float e, float t) {
        return s + (e - s) * t;
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
