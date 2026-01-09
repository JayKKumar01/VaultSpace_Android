package com.github.jaykkumar01.vaultspace.views;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.R;

public class ItemActionView extends FrameLayout {

    private static final String TAG="VaultSpace:ItemActionView";

    public interface Callback{
        void onActionSelected(int index,String label);
        void onCancel();
    }

    private final LinearLayout card;
    private final TextView titleView;
    private Callback callback;
    private String debugOwner="unknown";

    public ItemActionView(@NonNull Context context){
        super(context);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        setClickable(true);
        setBackgroundColor(0x990D1117);

        card=new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20),dp(18),dp(20),dp(20));
        card.setElevation(dp(10));

        GradientDrawable bg=new GradientDrawable();
        bg.setColor(context.getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);

        LayoutParams cardParams=new LayoutParams(dp(280),ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.gravity=Gravity.CENTER;
        card.setLayoutParams(cardParams);

        titleView=new TextView(context);
        titleView.setTextSize(16);
        titleView.setTypeface(titleView.getTypeface(),android.graphics.Typeface.BOLD);
        titleView.setTextColor(context.getColor(R.color.vs_accent_primary));
        titleView.setLines(1);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);


        card.addView(titleView);
        card.addView(accentUnderline());

        addView(card);

        card.setOnClickListener(v->{});

        setOnClickListener(v->{
            Log.d(TAG,debugOwner+" → outside dismiss");
            Callback cb=callback;
            hide();
            if(cb!=null) cb.onCancel();
        });

        setVisibility(GONE);
    }

    /* ---------------- Public API ---------------- */

    public void show(String title,String[] actions,String debugOwner,Callback callback){
        this.callback=callback;
        this.debugOwner=debugOwner;

        card.removeAllViews();
        card.addView(titleView);
        titleView.setText(title);
        card.addView(accentUnderline());

        for(int i=0;i<actions.length;i++){
            final int index=i;
            TextView action=createActionView(actions[i]);
            action.setOnClickListener(v->{
                Log.d(TAG,debugOwner+" → action["+index+"]: "+actions[index]);
                Callback cb=this.callback;
                hide();
                if(cb!=null) cb.onActionSelected(index,actions[index]);
            });
            card.addView(action);
        }

        Log.d(TAG,debugOwner+" → show");

        setVisibility(VISIBLE);
        card.setScaleX(0.9f);
        card.setScaleY(0.9f);
        card.setAlpha(0f);
        card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(160).setInterpolator(new DecelerateInterpolator()).start();
    }

    public void hide(){
        card.animate().scaleX(0.9f).scaleY(0.9f).alpha(0f).setDuration(120).withEndAction(()->{
            setVisibility(GONE);
            callback=null;
        }).start();
    }

    public boolean isVisible(){
        return getVisibility()==VISIBLE;
    }

    /* ---------------- UI helpers ---------------- */

    private View accentUnderline(){
        View v=new View(getContext());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(32),dp(3));
        lp.topMargin=dp(6);
        lp.bottomMargin=dp(12);
        v.setLayoutParams(lp);
        v.setBackgroundColor(getContext().getColor(R.color.vs_accent_primary));
        return v;
    }

    private TextView createActionView(String text){
        TextView tv=new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(getContext().getColor(R.color.vs_text_header));
        tv.setPadding(dp(12),dp(14),dp(12),dp(14));

        GradientDrawable bg=new GradientDrawable();
        bg.setColor(getContext().getColor(R.color.vs_surface_soft));
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1),getContext().getColor(R.color.vs_accent_primary));
        tv.setBackground(bg);

        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin=dp(10);
        tv.setLayoutParams(lp);

        return tv;
    }

    /* ---------------- Utils ---------------- */

    private int dp(int v){
        return (int)(v*getResources().getDisplayMetrics().density);
    }
}
