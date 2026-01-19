package com.github.jaykkumar01.vaultspace.views.creative.upload;

public final class ProgressTicker {

    private final long durationMs;
    private long startMs;
    private boolean running;

    public ProgressTicker(long durationMs){
        this.durationMs=durationMs;
    }

    public void start(long nowMs){
        startMs=nowMs;
        running=true;
    }

    public void stop(){
        running=false;
    }

    public boolean isRunning(){
        return running;
    }

    /** @return normalized t in [0..1] */
    public float tick(long nowMs){
        if(!running)return 1f;
        float t=(nowMs-startMs)/(float)durationMs;
        if(t>=1f){
            running=false;
            return 1f;
        }
        return t;
    }
}
