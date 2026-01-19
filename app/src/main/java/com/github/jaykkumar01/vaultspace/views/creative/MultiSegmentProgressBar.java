package com.github.jaykkumar01.vaultspace.views.creative;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MultiSegmentProgressBar extends View {

    private static final long PROGRESS_DURATION=180L;
    private static final long SWEEP_DURATION=2000L;
    private static final int SWEEP_COLOR_INITIAL=0x99FFFFFF;
    private static final int SWEEP_COLOR_FINAL=0xFFFFFFFF;

    private static final int SWEEP_NONE=0;
    private static final int SWEEP_IDLE=1;
    private static final int SWEEP_COMPLETION=2;

    private float[] target=new float[0];
    private float[] animated=new float[0];
    private int[] colors=new int[0];

    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint=new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path clipPath=new Path();
    private final RectF rect=new RectF();
    private float cornerRadius;

    private float sweepX=Float.NaN;
    private int sweepMode=SWEEP_NONE;
    private float lastSweepProgress=-1f;

    private UpsTickerAnimator progressTicker;
    private UpsTickerAnimator sweepTicker;

    public MultiSegmentProgressBar(Context c){super(c);init();}
    public MultiSegmentProgressBar(Context c,@Nullable AttributeSet a){super(c,a);init();}

    private void init(){
        bgPaint.setStyle(Paint.Style.FILL);
        cornerRadius=getResources().getDisplayMetrics().density*2f;
        progressTicker=new UpsTickerAnimator(this,PROGRESS_DURATION,60,15,this::onProgressTick);
        sweepTicker=new UpsTickerAnimator(this,SWEEP_DURATION,60,15,this::onSweepTick);
        syncBackgroundPaint();
    }

    /* ===== Public API ===== */

    public void setColors(int[] c){
        colors=c!=null?c.clone():new int[0];
        invalidate();
    }

    public void setFractions(float[] f){
        ensureCapacity(f);
        float sum=0f;
        for(int i=0;i<target.length;i++){
            float v=clamp01(f[i]);
            target[i]=v;
            sum+=v;
        }
        resetSweep();
        if(sum==0f)startIdleSweep();
        else if(sum>=0.999f)startCompletionSweep();
        progressTicker.start();
    }

    public void setFractionsImmediate(float[] f){
        ensureCapacity(f);
        float sum=0f;
        for(int i=0;i<target.length;i++){
            float v=clamp01(f[i]);
            target[i]=v;
            animated[i]=v;
            sum+=v;
        }
        resetSweep();
        if(sum==0f)startIdleSweep();
        else if(sum>=0.999f)startCompletionSweep();
        invalidate();
    }

    /* ===== Progress ===== */

    private void onProgressTick(float p){
        boolean done=true;
        for(int i=0;i<animated.length;i++){
            float v=animated[i]+(target[i]-animated[i])*0.25f;
            animated[i]=v;
            if(Math.abs(v-target[i])>0.001f)done=false;
        }
        invalidate();
        if(done)progressTicker.stop();
    }

    /* ===== Sweep ===== */

    private void resetSweep(){
        sweepTicker.stop();
        sweepMode=SWEEP_NONE;
        sweepX=Float.NaN;
        lastSweepProgress=-1f;
    }

    private void startIdleSweep(){
        sweepMode=SWEEP_IDLE;
        lastSweepProgress=-1f;
        sweepPaint.setColor(SWEEP_COLOR_INITIAL);
        sweepTicker.setOneShot(false);
        sweepTicker.start();
    }

    private void startCompletionSweep(){
        sweepMode=SWEEP_COMPLETION;
        lastSweepProgress=-1f;
        sweepPaint.setColor(SWEEP_COLOR_FINAL);
        sweepTicker.setOneShot(true);
        sweepTicker.start();
    }

    private void onSweepTick(float p){
        float w=getWidth();
        sweepX=-w+(2f*w*p);

        if(sweepMode==SWEEP_COMPLETION){
            if(lastSweepProgress>=0f&&p<lastSweepProgress){
                // cycle wrapped → time passed → remove sweep completely
                sweepTicker.stop();
                sweepX=Float.NaN;
                sweepMode=SWEEP_NONE;
                lastSweepProgress=-1f;
                invalidate();
                return;
            }
            lastSweepProgress=p;
        }

        invalidate();
    }

    /* ===== Draw ===== */

    @Override protected void onSizeChanged(int w,int h,int ow,int oh){
        rect.set(0,0,w,h);
        clipPath.reset();
        clipPath.addRoundRect(rect,cornerRadius,cornerRadius,Path.Direction.CW);
    }

    @Override protected void onDraw(@NonNull Canvas c){
        int w=getWidth(),h=getHeight();
        if(w<=0||h<=0)return;
        int save=c.save();
        c.clipPath(clipPath);
        c.drawRect(0,0,w,h,bgPaint);
        float x=0f;
        for(int i=0;i<animated.length;i++){
            float fw=animated[i]*w;
            if(fw<=0f)continue;
            paint.setColor(i<colors.length?colors[i]:0);
            c.drawRect(x,0,x+fw,h,paint);
            x+=fw;
            if(x>=w)break;
        }
        if(!Float.isNaN(sweepX)){
            float sw=w*0.45f;
            c.drawRect(sweepX,0,sweepX+sw,h,sweepPaint);
        }
        c.restoreToCount(save);
    }

    /* ===== Lifecycle ===== */

    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        progressTicker.onAttached();
        sweepTicker.onAttached();
        syncBackgroundPaint();
    }

    @Override protected void onDetachedFromWindow(){
        progressTicker.onDetached();
        sweepTicker.onDetached();
        super.onDetachedFromWindow();
    }

    @Override protected void onVisibilityChanged(@NonNull View v,int vis){
        super.onVisibilityChanged(v,vis);
        boolean visible=vis==VISIBLE;
        progressTicker.onVisibilityChanged(visible);
        sweepTicker.onVisibilityChanged(visible);
    }

    /* ===== Helpers ===== */

    private void ensureCapacity(float[] f){
        int n=f!=null?f.length:0;
        if(n==target.length)return;
        target=new float[n];
        animated=new float[n];
    }

    private void syncBackgroundPaint(){
        if(getBackground() instanceof android.graphics.drawable.ColorDrawable)
            bgPaint.setColor(((android.graphics.drawable.ColorDrawable)getBackground()).getColor());
    }

    private static float clamp01(float v){
        return v<0f?0f:Math.min(v,1f);
    }
}
