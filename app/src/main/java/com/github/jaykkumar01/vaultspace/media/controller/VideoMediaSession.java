package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

final class VideoMediaSession {

    /* ---------------- state ---------------- */

    enum State {
        IDLE,
        CHECK_CACHE,
        URL_PREPARE,
        URL_WAIT,
        DOWNLOAD,
        COMMITTED,
        ABORTED
    }

    private final AtomicReference<State> state =
            new AtomicReference<>(State.IDLE);

    /* ---------------- deps ---------------- */

    private final Context context;
    private final VideoMediaController controller;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler main =
            new Handler(Looper.getMainLooper());

    /* ---------------- runtime ---------------- */

    private VideoMediaTask currentTask;
    private AlbumMedia media;

    /* ---------------- constructor ---------------- */

    VideoMediaSession(@NonNull Context context,
                      @NonNull VideoMediaController controller) {
        this.context = context.getApplicationContext();
        this.controller = controller;
    }

    /* ---------------- entry ---------------- */

    void start(@NonNull AlbumMedia media) {
        if (!state.compareAndSet(State.IDLE, State.CHECK_CACHE)) return;
        this.media = media;
        transition(State.CHECK_CACHE);
    }

    public void onPlayerReady() {
        if (currentTask instanceof UrlTask) {
            ((UrlTask) currentTask).onPlayerReady();
        }
    }

    /* ---------------- release ---------------- */

    void release() {
        State prev = state.getAndSet(State.ABORTED);

        if (prev == State.ABORTED || prev == State.COMMITTED) return;

        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }

        executor.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }

    /* ---------------- transitions ---------------- */

    private void transition(@NonNull State next) {
        if (state.get() == State.ABORTED) return;

        state.set(next);

        switch (next) {

            case CHECK_CACHE:
                runCacheTask();
                break;

            case URL_PREPARE:
                runUrlTask();
                break;

            case DOWNLOAD:
                runDownloadTask();
                break;

            default:
                break;
        }
    }

    /* ---------------- tasks ---------------- */

    private void runCacheTask() {
        currentTask = new CacheTask(context);
        currentTask.start(
                media,
                taskCallback(State.CHECK_CACHE, State.URL_PREPARE)
        );
    }

    private void runUrlTask() {
        currentTask = new UrlTask(context);
        currentTask.start(
                media,
                taskCallback(State.URL_PREPARE, State.DOWNLOAD)
        );
    }

    private void runDownloadTask() {
        currentTask = new DownloadTask(context);
        currentTask.start(
                media,
                taskCallback(State.DOWNLOAD, State.ABORTED)
        );
    }

    /* ---------------- callback adapter ---------------- */

    private VideoMediaTask.Callback taskCallback(
            @NonNull State successFrom,
            @NonNull State failureTo) {

        return new VideoMediaTask.Callback() {

            @Override
            public void onAttachReady(@NonNull AttachPayload payload) {
                if (state.get() == State.ABORTED) return;

                // 🚨 ALWAYS marshal to main thread
                main.post(() -> {
                    if (state.get() == State.ABORTED) return;
                    if (payload.mediaSourceFactory != null && payload.mediaItem != null) {
//                        controller.attach(payload.mediaSourceFactory, payload.mediaItem);
                    }
                });
            }

            @Override
            public void onHealthy() {
                state.compareAndSet(successFrom, State.COMMITTED);
            }

            @Override
            public void onUnhealthy() {
                if (!state.compareAndSet(successFrom, failureTo)) return;
                transition(failureTo);
            }

            @Override
            public void onError(@NonNull Exception e) {
                state.set(State.ABORTED);
            }
        };
    }
}
