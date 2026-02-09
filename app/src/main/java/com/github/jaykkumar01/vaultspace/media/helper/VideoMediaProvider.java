package com.github.jaykkumar01.vaultspace.media.helper;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.VideoMediaPrepareCallback;
import com.github.jaykkumar01.vaultspace.media.base.VideoMediaSource;
import com.github.jaykkumar01.vaultspace.media.source.DriveDownloadMediaSource;
import com.github.jaykkumar01.vaultspace.media.source.DriveUrlMediaSource;

public final class VideoMediaProvider {

    private static final String TAG = "VideoMediaProvider";

    private final VideoMediaSource source;

    @OptIn(markerClass = UnstableApi.class)
    public VideoMediaProvider(@NonNull Context context,
                              @NonNull AlbumMedia media) {

        boolean download = shouldDownload(media);

        Log.d(TAG,
                "select source=" + (download ? "DOWNLOAD" : "URL")
                        + " rotation=" + media.rotation
                        + " size=" + media.sizeBytes);

        source = download
                ? new DriveDownloadMediaSource(context)
                : new DriveUrlMediaSource(context);
    }

    public void prepare(@NonNull AlbumMedia media,
                        @NonNull VideoMediaPrepareCallback callback) {
        source.prepare(media, callback);
    }

    public void release() {
        source.release();
    }

    /* ---------------- decision logic ---------------- */

    private boolean shouldDownload(@NonNull AlbumMedia media) {
        if (!media.isVideo) return false;
        return Math.abs(media.rotation) > 180;
        //check codec also, h.264 is okay even with rotation
        // wide angles is givng issues, so need to detect the late videos then send to download
    }
}
