package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.helper.DriveCacheWarmUpHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class UrlTask implements VideoMediaTask {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final UrlSourceBuilder builder;
    private UrlPlaybackObserver observer;
    private DriveCacheWarmUpHelper cacheWarmUpHelper;

    UrlTask(@NonNull Context context) {
        this.builder = new UrlSourceBuilder(context);
    }

    @Override
    public void start(@NonNull AlbumMedia media, @NonNull Callback callback) {

        executor.execute(() -> {
            if (cancelled.get()) return;

            observer = new UrlPlaybackObserver(media,new UrlPlaybackObserver.DecisionCallback() {
                @Override
                public void onHealthy() {
                    callback.onHealthy();
                }

                @Override
                public void onUnhealthy() {
                    callback.onUnhealthy();
                }
            });

            try {
                AttachPayload payload = builder.build(media, observer);

                callback.onAttachReady(payload);
                observer.start();

            } catch (Exception e) {
                callback.onUnhealthy();
            }

        });
    }

    public void onPlayerReady() {
        if (observer != null) observer.onPlayerReady();
    }


    @Override
    public void cancel() {
        cancelled.set(true);
        executor.shutdownNow();
        builder.release();
    }
}