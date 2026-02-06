package com.github.jaykkumar01.vaultspace.activities;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.drive.AlbumMediaRepository;
import com.github.jaykkumar01.vaultspace.media.base.MediaLoadCallback;
import com.github.jaykkumar01.vaultspace.media.controller.ImageMediaController;
import com.github.jaykkumar01.vaultspace.media.controller.VideoMediaController;
import com.github.jaykkumar01.vaultspace.views.states.LoadingStateView;

public final class MediaActivity extends AppCompatActivity implements MediaLoadCallback {

    private static final String TAG = "VaultSpace:Media";

    private String albumId;
    private String fileId;

    private ImageView imageView;
    private PlayerView playerView;
    private LoadingStateView loadingState;
    private ImageButton fullscreenButton;

    private ImageMediaController imageController;
    private VideoMediaController videoController;
    private AlbumMediaRepository repo;

    private OnBackPressedCallback backCallback;
    private OrientationEventListener orientationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!resolveIntent()) {
            finish();
            return;
        }
        setupWindow();
        enableTrueFullscreen();
        setupRepository();
        setupControllers();
        setupOrientationHandling();
        resolveMedia();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (videoController != null) videoController.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoController != null) videoController.onResume();
    }

    @Override
    protected void onPause() {
        if (videoController != null) videoController.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (videoController != null) videoController.onStop();
        super.onStop();
    }

    private void setupWindow() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_media);
        bindViews();
        setupBackHandling();
    }

    private void enableTrueFullscreen() {
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void bindViews() {
        imageView = findViewById(R.id.imageView);
        playerView = findViewById(R.id.playerView);
        loadingState = findViewById(R.id.loadingState);
        fullscreenButton = findViewById(R.id.video_fullscreen);

        findViewById(R.id.exo_back).setOnClickListener(v -> exit());
        fullscreenButton.setOnClickListener(v -> toggleOrientation());
    }

    private void setupRepository() {
        repo = AlbumMediaRepository.getInstance(this);
    }

    private void setupControllers() {
        imageController = new ImageMediaController(this, imageView);
        videoController = new VideoMediaController(this, playerView);
        imageController.setCallback(this);
        videoController.setCallback(this);
    }

    private void setupOrientationHandling() {
        orientationListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                updateFullscreenIcon(getResources().getConfiguration().orientation);
            }
        };
        if (orientationListener.canDetectOrientation()) orientationListener.enable();
        updateFullscreenIcon(getResources().getConfiguration().orientation);
    }

    private void toggleOrientation() {
        int current = getResources().getConfiguration().orientation;
        if (current == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private void updateFullscreenIcon(int orientation) {
        fullscreenButton.setImageResource(
                orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? R.drawable.ic_fullscreen_exit
                        : R.drawable.ic_fullscreen
        );
    }

    private void resolveMedia() {
        Log.d(TAG, "launch albumId=" + albumId + " fileId=" + fileId);
        AlbumMedia media = repo.getMediaById(albumId, fileId);
        if (media == null) {
            exit();
            return;
        }
        if (media.isVideo) {
            imageView.setVisibility(View.GONE);
            videoController.show(media);
        } else {
            playerView.setVisibility(View.GONE);
            imageController.show(media);
        }
    }

    @Override
    public void onMediaLoading() {
        loadingState.setText("Loading media…");
        loadingState.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMediaReady() {
        loadingState.setVisibility(View.GONE);
    }

    @Override
    public void onMediaError(Throwable t) {
        loadingState.setVisibility(View.GONE);
        exit();
    }

    private boolean resolveIntent() {
        albumId = getIntent().getStringExtra("albumId");
        fileId = getIntent().getStringExtra("fileId");
        return albumId != null && fileId != null && !albumId.isEmpty() && !fileId.isEmpty();
    }

    private void setupBackHandling() {
        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                exit();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    private void exit() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        finish();
        overridePendingTransition(R.anim.album_return_enter, R.anim.album_return_exit);
    }

    @Override
    protected void onDestroy() {
        if (backCallback != null) backCallback.remove();
        if (orientationListener != null) orientationListener.disable();
        if (imageController != null) imageController.release();
        if (videoController != null) videoController.release();
        super.onDestroy();
    }
}
