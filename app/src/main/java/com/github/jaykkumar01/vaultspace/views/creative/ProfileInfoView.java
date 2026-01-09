package com.github.jaykkumar01.vaultspace.views.creative;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

    public ProfileInfoView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public ProfileInfoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ProfileInfoView(@NonNull Context context,
                           @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {

        // ---------------- Profile Image ----------------
        ivProfile = new ImageView(context);
        ivProfile.setId(generateViewId());
        ivProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivProfile.setBackgroundResource(R.drawable.bg_profile_circle);
        ivProfile.setClipToOutline(true);
        ivProfile.setElevation(dpToPx(4));

        addView(ivProfile, new LayoutParams(dpToPx(40), dpToPx(40)));

        // ---------------- Name ----------------
        tvName = new TextView(context);
        tvName.setId(generateViewId());
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvName.setTextColor(context.getColor(R.color.vs_text_header));
        tvName.setTypeface(tvName.getTypeface(), android.graphics.Typeface.BOLD);

        addView(tvName, new LayoutParams(0, LayoutParams.WRAP_CONTENT));

        // ---------------- Email ----------------
        tvEmail = new TextView(context);
        tvEmail.setId(generateViewId());
        tvEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvEmail.setTextColor(context.getColor(R.color.vs_text_content));

        addView(tvEmail, new LayoutParams(0, LayoutParams.WRAP_CONTENT));

        // ---------------- Constraints ----------------
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

        // ---------------- XML attrs (preview + defaults) ----------------
        if (attrs != null) {
            TypedArray a =
                    context.obtainStyledAttributes(attrs, R.styleable.ProfileInfoView);

            String name = a.getString(R.styleable.ProfileInfoView_profileName);
            String email = a.getString(R.styleable.ProfileInfoView_profileEmail);
            Drawable image = a.getDrawable(R.styleable.ProfileInfoView_profileImage);

            if (name != null) {
                tvName.setText(name);
            }

            if (email != null) {
                tvEmail.setText(email);
            }

            if (image != null) {
                ivProfile.setImageDrawable(image);
            } else if (isInEditMode()) {
                // Preview fallback only
                ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
            }

            a.recycle();
        }
    }

    /* ================= Public API ================= */

    public void setProfile(@Nullable Bitmap bitmap,
                           @NonNull String name,
                           @NonNull String email) {

        tvName.setText(name);
        tvEmail.setText(email);

        if (bitmap != null) {
            ivProfile.setImageDrawable(
                    new BitmapDrawable(getResources(), bitmap)
            );
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    public void setName(@NonNull String name) {
        tvName.setText(name);
    }

    public void setEmail(@NonNull String email) {
        tvEmail.setText(email);
    }

    public void setProfileImage(@Nullable Bitmap bitmap) {
        if (bitmap != null) {
            ivProfile.setImageDrawable(
                    new BitmapDrawable(getResources(), bitmap)
            );
        }
    }

    /* ================= Utils ================= */

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
