package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.TransferListener;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.cache.DriveSingleFileCacheHelper;
import com.github.jaykkumar01.vaultspace.media.proxy.DriveProxyServer;

@OptIn(markerClass = UnstableApi.class)
public final class UrlSourceBuilder {

    private final Context appContext;
    private final DriveProxyServer proxy;

    UrlSourceBuilder(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.proxy = new DriveProxyServer(appContext);
    }

    public AttachPayload build(
            @NonNull AlbumMedia media,
            @NonNull UrlPlaybackObserver observer
    ) throws Exception {

        /* ---------------- proxy lifecycle ---------------- */

        proxy.start();
        proxy.registerFile(media.fileId, media.sizeBytes);

        /* ---------------- HTTP (proxy → no auth headers) ---------------- */

        DefaultHttpDataSource.Factory http =
                new DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setTransferListener(new TransferListener() {

                            long lastPosition = -1;

                            @Override
                            public void onTransferInitializing(
                                    @NonNull DataSource source,
                                    @NonNull DataSpec spec,
                                    boolean isNetwork
                            ) {
                                observer.onInit();
                            }

                            @Override
                            public void onTransferStart(
                                    @NonNull DataSource source,
                                    @NonNull DataSpec spec,
                                    boolean isNetwork
                            ) {
                                if (lastPosition != -1 && spec.position < lastPosition) {
                                    observer.onPositionRewind();
                                    return;
                                }
                                lastPosition = spec.position;
                                observer.onStart();
                            }

                            @Override
                            public void onBytesTransferred(
                                    @NonNull DataSource source,
                                    @NonNull DataSpec spec,
                                    boolean isNetwork,
                                    int bytesTransferred
                            ) {
                                observer.onData(bytesTransferred);
                            }

                            @Override
                            public void onTransferEnd(
                                    @NonNull DataSource source,
                                    @NonNull DataSpec spec,
                                    boolean isNetwork
                            ) {}
                        });

        /* ---------------- cache ---------------- */

        CacheDataSource.Factory cachedFactory =
                DriveSingleFileCacheHelper.wrap(
                        appContext,
                        media.fileId,
                        http
                );

        DefaultMediaSourceFactory mediaSourceFactory =
                new DefaultMediaSourceFactory(cachedFactory);

        /* ---------------- proxy URL ---------------- */

        String proxyUrl = proxy.getProxyUrl(media.fileId);
        MediaItem item = MediaItem.fromUri(proxyUrl);

        return new AttachPayload(
                mediaSourceFactory,
                cachedFactory,
                item
        );
    }

    /* ---------------- lifecycle hook (IMPORTANT) ---------------- */

    public void release() {
        proxy.stop();
    }
}
