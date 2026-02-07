package com.github.jaykkumar01.vaultspace.views.creative.image;

import android.view.Choreographer;

public final class PanFlingHelper implements Choreographer.FrameCallback {

    private static final float DECAY = 0.92f;        // tuned
    private static final float MIN_VELOCITY = 60f;   // px/sec cutoff

    public interface Callback {
        void onDelta(float dx, float dy);
        void onStop();
    }

    private final Callback callback;

    private float vx, vy;            // px/sec
    private long lastFrameNs;
    private boolean running;

    public PanFlingHelper(Callback callback){
        this.callback = callback;
    }

    public boolean isRunning(){
        return running;
    }

    public void start(float velocityX, float velocityY){
        vx = velocityX;
        vy = velocityY;

        if(Math.abs(vx) < MIN_VELOCITY && Math.abs(vy) < MIN_VELOCITY){
            stop();
            return;
        }

        running = true;
        lastFrameNs = 0;
        Choreographer.getInstance().postFrameCallback(this);
    }

    public void stop(){
        if(!running) return;

        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
        callback.onStop();
    }

    @Override
    public void doFrame(long frameTimeNs){
        if(!running) return;

        if(lastFrameNs == 0){
            lastFrameNs = frameTimeNs;
            Choreographer.getInstance().postFrameCallback(this);
            return;
        }

        float dt = (frameTimeNs - lastFrameNs) * 1e-9f; // seconds
        lastFrameNs = frameTimeNs;

        float dx = vx * dt;
        float dy = vy * dt;

        callback.onDelta(dx, dy);

        vx *= DECAY;
        vy *= DECAY;

        if(Math.abs(vx) < MIN_VELOCITY && Math.abs(vy) < MIN_VELOCITY){
            stop();
        } else {
            Choreographer.getInstance().postFrameCallback(this);
        }
    }
}
