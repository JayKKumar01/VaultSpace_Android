package com.github.jaykkumar01.vaultspace.views.creative.upload_status;

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

public final class UploadStatusView extends FrameLayout {

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

    /* ================= Renderer ================= */

    private final UploadStatusRenderer renderer = new UploadStatusRenderer();

    /* ================= Layout ================= */

    private int dismissOverlap;

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

    /* ================= Public API (Controller-safe) ================= */

    public void setMediaCounts(int photos, int videos) {
        renderer.setMediaCounts(photos, videos);
    }

    public void setTotalCount(int total) {
        renderer.setTotalCount(total);
    }

    public void setUploadedCount(int uploaded) {
        renderer.setUploadedCount(uploaded);
    }

    public void setFailedCount(int failed) {
        renderer.setFailedCount(failed);
    }

    public void setNoAccessCount(int noAccess) {
        renderer.setNoAccessCount(noAccess);
    }

    public void renderUploading(OnClickListener action, int completed, int total) {
        apply(renderer.renderUploading(action, completed, total));
    }

    public void renderFailed(OnClickListener action) {
        apply(renderer.renderFailed(action));
    }

    public void renderNoAccess(OnClickListener action) {
        apply(renderer.renderNoAccess(action));
    }

    public void renderCompleted(OnClickListener action) {
        apply(renderer.renderCompleted(action));
    }

    public void hide() {
        if (getVisibility() != GONE) setVisibility(GONE);
    }

    /* ================= Render Applier ================= */

    private void apply(UploadStatusRenderModel m) {

        tvMediaInfo.setText(m.mediaInfoText);
        tvUploadingState.setText(m.uploadingStateText);
        tvUploadRatio.setText(m.uploadRatioText);

        ivDismiss.setVisibility(m.showDismiss ? VISIBLE : GONE);
        updateCardOffsets(m.showDismiss);

        ivWarning.setVisibility(m.showRetryWarning ? VISIBLE : GONE);
        tvFailedCount.setVisibility(m.showRetryWarning ? VISIBLE : GONE);
        if (m.showRetryWarning) tvFailedCount.setText(m.failedCountText);

        ivNoAccessWarning.setVisibility(m.showNoAccessWarning ? VISIBLE : GONE);
        tvNoAccessCount.setVisibility(m.showNoAccessWarning ? VISIBLE : GONE);
        if (m.showNoAccessWarning) tvNoAccessCount.setText(m.noAccessCountText);

        btnAction.setText(m.actionText);
        btnAction.setBackgroundResource(m.actionBackgroundRes);
        btnAction.setOnClickListener(m.actionClick);
        btnAction.setVisibility(VISIBLE);

        progressBar.setFractions(m.progressFractions);

        if (getVisibility() != VISIBLE) setVisibility(VISIBLE);
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

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
