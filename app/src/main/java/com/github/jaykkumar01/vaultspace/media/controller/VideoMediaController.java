package com.github.jaykkumar01.vaultspace.media.controller;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSink;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.FileDataSource;
import androidx.media3.datasource.cache.CacheDataSink;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.helper.VideoMediaDriveHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@UnstableApi
public final class VideoMediaController {

    private static final String TAG = "VaultSpace:VideoCache";
    private static final long CACHE_WARM_BYTES = 2L * 1024 * 1024;

    private final PlayerView view;
    private final VideoMediaDriveHelper driveHelper;
    private final StandaloneDatabaseProvider databaseProvider;

    private SimpleCache cache;
    private File cacheDir;
    private ExoPlayer player;

    private AlbumMedia media;
    private MediaLoadCallback callback;

    private boolean playWhenReady = true;
    private long resumePosition = 0L;

    public VideoMediaController(@NonNull AppCompatActivity activity,@NonNull PlayerView playerView){
        this.view = playerView;
        this.driveHelper = new VideoMediaDriveHelper(activity);
        this.databaseProvider = new StandaloneDatabaseProvider(activity);
        view.setVisibility(View.GONE);
    }

    public void setCallback(MediaLoadCallback callback){
        this.callback = callback;
    }

    public void show(@NonNull AlbumMedia media){
        this.media = media;
        view.setVisibility(View.GONE);
    }

    public void onStart(){
        if(player == null && media != null) prepare();
    }

    public void onResume(){
        if(player != null) player.setPlayWhenReady(playWhenReady);
    }

    public void onPause(){
        releasePlayerInternal();
    }

    public void onStop(){
        releasePlayerInternal();
    }

    public void release(){
        releasePlayerInternal();
        releaseCache();
        driveHelper.release();
        callback = null;
        media = null;
    }

    private void prepare(){
        if(media == null) return;

        view.setVisibility(View.GONE);
        if(callback != null) callback.onMediaLoading();

        driveHelper.resolve(media,new VideoMediaDriveHelper.Callback(){
            @Override
            public void onReady(@NonNull String url,@NonNull String token){
                DefaultMediaSourceFactory factory =
                        buildMediaSourceFactory(view.getContext(),token);

                player = new ExoPlayer.Builder(view.getContext())
                        .setMediaSourceFactory(factory)
                        .build();

                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                player.setPlayWhenReady(playWhenReady);
                player.addListener(playerListener());

                view.setPlayer(player);
                player.setMediaItem(MediaItem.fromUri(url));
                player.prepare();

                if(resumePosition > 0) player.seekTo(resumePosition);
            }

            @Override
            public void onError(@NonNull Exception e){
                if(callback != null) callback.onMediaError(e);
            }
        });
    }

    private Player.Listener playerListener(){
        return new Player.Listener(){
            @Override
            public void onPlaybackStateChanged(int state){
                if(state == Player.STATE_READY && player != null){
                    view.setVisibility(View.VISIBLE);
                    if(callback != null) callback.onMediaReady();
                    player.removeListener(this);
                }
            }
        };
    }

    private DefaultMediaSourceFactory buildMediaSourceFactory(Context c,String token){
        DefaultHttpDataSource.Factory http =
                new DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setDefaultRequestProperties(buildHeaders(token));

        initCache(c);

        DataSink.Factory limitedSinkFactory = new DataSink.Factory(){
            @NonNull
            @Override
            public DataSink createDataSink(){
                Log.d(TAG,"cache warm start limit=" + (CACHE_WARM_BYTES / 1024 / 1024) + "MB");
                return new LimitedCacheDataSink(
                        new CacheDataSink(cache,CacheDataSink.DEFAULT_FRAGMENT_SIZE),
                        CACHE_WARM_BYTES
                );
            }
        };

        CacheDataSource.Factory cacheFactory =
                new CacheDataSource.Factory()
                        .setCache(cache)
                        .setUpstreamDataSourceFactory(http)
                        .setCacheReadDataSourceFactory(new FileDataSource.Factory())
                        .setCacheWriteDataSinkFactory(limitedSinkFactory)
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        return new DefaultMediaSourceFactory(cacheFactory);
    }

    private void initCache(Context c){
        if(cache != null) return;
        cacheDir = new File(c.getCacheDir(),"video_tmp_"+System.nanoTime());
        cacheDir.mkdirs();
        cache = new SimpleCache(
                cacheDir,
                new NoOpCacheEvictor(),
                databaseProvider
        );
    }

    private void releaseCache(){
        if(cache == null) return;
        File oldDir = cacheDir;
        try{ cache.release(); }catch(Exception ignored){}
        cache = null;
        cacheDir = null;
        new Thread(() -> deleteDir(oldDir)).start();
    }

    private void releasePlayerInternal(){
        if(player == null) return;
        resumePosition = player.getCurrentPosition();
        playWhenReady = player.getPlayWhenReady();
        player.release();
        player = null;
    }

    private static void deleteDir(File dir){
        if(dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if(files != null) for(File f : files){
            if(f.isDirectory()) deleteDir(f);
            else f.delete();
        }
        dir.delete();
    }

    private static Map<String,String> buildHeaders(String token){
        Map<String,String> h = new HashMap<>();
        h.put("Authorization","Bearer "+token);
        h.put("Accept-Encoding","identity");
        h.put("Accept","*/*");
        h.put("Connection","keep-alive");
        h.put("Range","bytes=0-");
        return h;
    }

    private static final class LimitedCacheDataSink implements DataSink {

        private final DataSink upstream;
        private final long maxBytes;
        private long written;
        private boolean loggedStop;

        LimitedCacheDataSink(DataSink upstream,long maxBytes){
            this.upstream = upstream;
            this.maxBytes = maxBytes;
        }

        @Override
        public void open(@NonNull DataSpec dataSpec) throws IOException{
            if(written < maxBytes) upstream.open(dataSpec);
        }

        @Override
        public void write(@NonNull byte[] buffer, int offset, int length) throws IOException{
            if(written >= maxBytes){
                if(!loggedStop){
                    Log.d(TAG,"cache hard stop at " + (written / 1024 / 1024) + "MB");
                    loggedStop = true;
                }
                return;
            }
            int allowed = (int)Math.min(length,maxBytes - written);
            upstream.write(buffer,offset,allowed);
            written += allowed;
        }

        @Override
        public void close() throws IOException{
            upstream.close();
        }
    }
}
