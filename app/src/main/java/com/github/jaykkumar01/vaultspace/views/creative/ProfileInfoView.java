package com.github.jaykkumar01.vaultspace.views.creative;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.github.jaykkumar01.vaultspace.R;

public class ProfileInfoView extends ConstraintLayout {

    private ImageView ivProfile;
    private TextView tvName;
    private TextView tvEmail;

    private String currentEmail;
    private String currentName;

    public ProfileInfoView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ProfileInfoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ProfileInfoView(@NonNull Context context,
                           @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        /* ---------------- Profile Image ---------------- */

        ivProfile = new ImageView(context);
        ivProfile.setId(generateViewId());
        ivProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivProfile.setBackgroundResource(R.drawable.bg_profile_circle);
        ivProfile.setClipToOutline(true);
        ivProfile.setElevation(dpToPx(4));

        addView(ivProfile, new LayoutParams(dpToPx(40), dpToPx(40)));

        /* ---------------- Name ---------------- */

        tvName = new TextView(context);
        tvName.setId(generateViewId());
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvName.setTextColor(context.getColor(R.color.vs_text_header));
        tvName.setTypeface(tvName.getTypeface(), android.graphics.Typeface.BOLD);
        tvName.setMaxLines(1);
        tvName.setSingleLine(true);
        tvName.setEllipsize(TextUtils.TruncateAt.END);


        addView(tvName, new LayoutParams(0, LayoutParams.WRAP_CONTENT));

        /* ---------------- Email ---------------- */

        tvEmail = new TextView(context);
        tvEmail.setId(generateViewId());
        tvEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvEmail.setTextColor(context.getColor(R.color.vs_text_content));
        tvEmail.setMaxLines(1);
        tvEmail.setSingleLine(true);
        tvEmail.setEllipsize(TextUtils.TruncateAt.END);


        addView(tvEmail, new LayoutParams(0, LayoutParams.WRAP_CONTENT));

        /* ---------------- Constraints ---------------- */

        ConstraintSet set = new ConstraintSet();
        set.clone(this);

        set.connect(ivProfile.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        set.connect(ivProfile.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);

        set.connect(tvName.getId(), ConstraintSet.START, ivProfile.getId(), ConstraintSet.END, dpToPx(10));
        set.connect(tvName.getId(), ConstraintSet.TOP, ivProfile.getId(), ConstraintSet.TOP);
        set.connect(tvName.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

        set.connect(tvEmail.getId(), ConstraintSet.START, tvName.getId(), ConstraintSet.START);
        set.connect(tvEmail.getId(), ConstraintSet.TOP, tvName.getId(), ConstraintSet.BOTTOM, dpToPx(1));
        set.connect(tvEmail.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

        set.applyTo(this);

        /* ---------------- Defaults ---------------- */

        ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
    }

    /* ==========================================================
     * Public API (Dashboard-only)
     * ========================================================== */

    /** Write-once */
    public void setEmail(@NonNull String email) {
        if (email.equals(currentEmail)) return;
        currentEmail = email;
        tvEmail.setText(email);
    }

    /** Patchable */
    public void setName(@NonNull String name) {
        if (name.equals(currentName)) return;
        currentName = name;
        tvName.setText(name);
    }

    /** Patchable */
    public void setProfileImage(@Nullable Bitmap bitmap) {
        if (bitmap == null) return;

        Drawable current = ivProfile.getDrawable();
        if (current instanceof BitmapDrawable) {
            Bitmap currentBitmap = ((BitmapDrawable) current).getBitmap();
            if (currentBitmap == bitmap) return;
        }

        ivProfile.setImageDrawable(
                new BitmapDrawable(getResources(), bitmap)
        );
    }

    /* ==========================================================
     * Utils
     * ========================================================== */

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
