package com.github.jaykkumar01.vaultspace.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.ui.PlayerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.helper.DriveResolver;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.core.drive.AlbumMediaRepository;
import com.github.jaykkumar01.vaultspace.media.controller.ImageMediaController;
import com.github.jaykkumar01.vaultspace.media.controller.VideoMediaController;
import com.github.jaykkumar01.vaultspace.media.helper.ImageMediaDriveHelper;

public class MediaActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Media";

    /* ============================================================
     * Intent
     * ============================================================ */

    private String albumId;
    private String fileId;

    /* ============================================================
     * UI
     * ============================================================ */

    private ImageView imageView;
    private PlayerView playerView;

    /* ============================================================
     * Controllers
     * ============================================================ */

    private ImageMediaController imageController;
    private VideoMediaController videoController;

    /* ============================================================
     * Domain
     * ============================================================ */

    private AlbumMediaRepository repo;

    /* ============================================================
     * Lifecycle
     * ============================================================ */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (noIntent()) {
            finish();
            return;
        }

        setupWindow();        // binds views
        setupRepository();    // init repo + drive helper
        setupControllers();   // create controllers AFTER views
        resolveMedia();
    }

    /* ============================================================
     * Setup
     * ============================================================ */

    private void setupWindow() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_media);
        applyWindowInsets();
        setupBackHandling();
        bindViews();
    }

    private void bindViews() {
        imageView = findViewById(R.id.imageView);
        playerView = findViewById(R.id.playerView);
    }

    private void setupRepository() {
        repo = AlbumMediaRepository.getInstance(this);
    }

    private void setupControllers() {
        imageController = new ImageMediaController(this,imageView);
        videoController = new VideoMediaController(this, playerView);
    }

    /* ============================================================
     * Media resolution
     * ============================================================ */

    private void resolveMedia() {
        Log.d(TAG, "launch albumId=" + albumId + " fileId=" + fileId);

        AlbumMedia media = repo.getMediaById(albumId, fileId);

        if (media == null) {
            Log.w(TAG, "media not found (cache miss or not initialized)");
            finish();
            return;
        }

        logResolvedMedia(media);

        if (media.isVideo) {
            imageView.setVisibility(View.GONE);
            videoController.show(media);
        } else {
            playerView.setVisibility(View.GONE);
            imageController.show(media);
        }
    }

    /* ============================================================
     * Logging
     * ============================================================ */

    private void logResolvedMedia(AlbumMedia m) {
        Log.d(TAG,
                "media resolved → " +
                        "id=" + m.fileId +
                        " name=" + m.name +
                        " video=" + m.isVideo +
                        " aspect=" + m.aspectRatio +
                        " rotation=" + m.rotation +
                        " size=" + m.sizeBytes
        );
    }

    /* ============================================================
     * Intent
     * ============================================================ */

    private boolean noIntent() {
        albumId = getIntent().getStringExtra("albumId");
        fileId  = getIntent().getStringExtra("fileId");
        return albumId == null || fileId == null || albumId.isEmpty() || fileId.isEmpty();
    }

    /* ============================================================
     * Window insets
     * ============================================================ */

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, i) -> {
            Insets b = i.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return i;
        });
    }

    /* ============================================================
     * Back handling
     * ============================================================ */

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(
                        R.anim.album_return_enter,
                        R.anim.album_return_exit
                );
            }
        });
    }

    /* ============================================================
     * Cleanup
     * ============================================================ */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageController.release();
        videoController.release();
    }
}
