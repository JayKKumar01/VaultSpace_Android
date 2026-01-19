package com.github.jaykkumar01.vaultspace.views.creative.upload;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
public final class SweepTicker {

    public enum Mode{NONE,IDLE,COMPLETION}

    private static final long COMPLETION_DURATION_MS=2000L;
    private static final float IDLE_SPEED=0.0005f;

    private final View host;

    private Mode mode=Mode.NONE;
    private long startMs;
    private float sweepX=Float.NaN;
    private int color;

    public SweepTicker(@NonNull View host){
        this.host=host;
    }

    public void startIdle(@ColorInt int color){
        if(mode==Mode.IDLE)return;
        mode=Mode.IDLE;
        startMs=0L;
        sweepX=Float.NaN;
        this.color=color;
    }

    public void startCompletion(@ColorInt int color){
        if(mode==Mode.COMPLETION)return;
        mode=Mode.COMPLETION;
        startMs=0L;
        sweepX=Float.NaN;
        this.color=color;
    }

    public void stop(){
        if(mode==Mode.NONE)return;
        mode=Mode.NONE;
        startMs=0L;
        sweepX=Float.NaN;
    }

    public boolean isActive(){
        return mode!=Mode.NONE;
    }

    public float getSweepX(){
        return sweepX;
    }

    public int getColor(){
        return color;
    }

    /** @return true if still animating */
    public boolean tick(long nowMs){
        if(mode==Mode.NONE)return false;
        if(startMs==0L)startMs=nowMs;

        int w=host.getWidth();
        if(w<=0)return true;

        long elapsed=nowMs-startMs;

        if(mode==Mode.IDLE){
            float p=(elapsed*IDLE_SPEED);
            p-= (int)p; // cheaper than %
            sweepX=-w+(2f*w*p);
            return true;
        }

        if(elapsed>=COMPLETION_DURATION_MS){
            stop();
            return false;
        }

        float p=elapsed/(float)COMPLETION_DURATION_MS;
        sweepX=-w+(2f*w*p);
        return true;
    }
}
