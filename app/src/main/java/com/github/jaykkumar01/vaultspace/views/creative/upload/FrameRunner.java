package com.github.jaykkumar01.vaultspace.views.creative.upload;

import android.view.View;
import androidx.annotation.NonNull;

public final class FrameRunner {

    public interface Callback {
        void onFrame(long nowMs);
    }

    private final View host;
    private final Callback callback;
    private final long frameIntervalMs;

    private boolean running;
    private long lastFrameMs;

    private final Runnable frameRunnable = new Runnable() {
        @Override public void run() {
            if (!running) return;

            long now = android.os.SystemClock.uptimeMillis();
            long delta = now - lastFrameMs;

            if (delta < frameIntervalMs) {
                host.postDelayed(this, frameIntervalMs - delta);
                return;
            }

            lastFrameMs = now;
            callback.onFrame(now);

            if (running) {
                host.postDelayed(this, frameIntervalMs);
            }
        }
    };

    public FrameRunner(@NonNull View host, int ups, @NonNull Callback callback) {
        this.host = host;
        this.callback = callback;
        this.frameIntervalMs = 1000L / ups;
    }

    public void start() {
        if (running) return;
        running = true;
        lastFrameMs = 0L;
        host.removeCallbacks(frameRunnable);
        host.post(frameRunnable);
    }

    public void stop() {
        if (!running) return;
        running = false;
        lastFrameMs = 0L;
        host.removeCallbacks(frameRunnable);
    }

    public boolean isRunning() {
        return running;
    }
}

