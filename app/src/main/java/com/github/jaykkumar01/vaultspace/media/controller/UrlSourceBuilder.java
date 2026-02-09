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
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.github.jaykkumar01.vaultspace.media.helper.DriveSingleFileCacheHelper;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.util.HashMap;
import java.util.Map;

@OptIn(markerClass = UnstableApi.class)
public final class UrlSourceBuilder {

    private final Context context;
    private final GoogleAccountCredential credential;

    UrlSourceBuilder(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.credential = GoogleCredentialFactory.forPrimaryDrive(this.context);
    }

    public AttachPayload build(@NonNull AlbumMedia media, @NonNull UrlPlaybackObserver observer) throws Exception {

        String token = credential.getToken();
        if (token == null || token.isEmpty())
            throw new IllegalStateException("Drive token missing");

        Map<String, String> headers = new HashMap<>(1);
        headers.put("Authorization", "Bearer " + token);

        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(headers)
                .setTransferListener(new TransferListener() {
                    long lastPosition = -1;

                    @Override
                    public void onTransferInitializing(@NonNull DataSource source, @NonNull DataSpec spec, boolean isNetwork) {
                        observer.onInit();
                    }

                    @Override
                    public void onTransferStart(@NonNull DataSource source, @NonNull DataSpec spec, boolean isNetwork) {

                        if (lastPosition != -1 && spec.position < lastPosition) {
                            observer.onPositionRewind();
                            return;
                        }

                        lastPosition = spec.position;
                        observer.onStart();
                    }

                    @Override
                    public void onBytesTransferred(@NonNull DataSource source, @NonNull DataSpec spec, boolean isNetwork, int bytesTransferred) {
                        observer.onData(bytesTransferred);
                    }

                    @Override
                    public void onTransferEnd(@NonNull DataSource source, @NonNull DataSpec spec, boolean isNetwork) {
                    }
                });

        DefaultMediaSourceFactory factory = new DefaultMediaSourceFactory(DriveSingleFileCacheHelper.wrap(context, media.fileId, http));

        MediaItem item = MediaItem.fromUri("https://www.googleapis.com/drive/v3/files/" + media.fileId + "?alt=media");

        return new AttachPayload(factory, item);
    }
}
