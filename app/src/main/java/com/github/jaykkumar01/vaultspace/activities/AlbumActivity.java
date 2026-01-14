package com.github.jaykkumar01.vaultspace.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.AlbumDriveHelper;
import com.github.jaykkumar01.vaultspace.album.AlbumLoader;
import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.AlbumUiHelper;
import com.github.jaykkumar01.vaultspace.album.AlbumUiHelper.AlbumUiCallback;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.AlbumMediaEntry;
import com.github.jaykkumar01.vaultspace.views.creative.AlbumMetaInfoView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumActivity extends AppCompatActivity implements AlbumUiCallback {

    private static final String TAG = "VaultSpace:Album";

    public static final String EXTRA_ALBUM_ID = "album_id";
    public static final String EXTRA_ALBUM_NAME = "album_name";

    private enum UiState { UNINITIALIZED, LOADING, EMPTY, CONTENT, ERROR }

    private String albumId;
    private String albumName;

    private UiState state = UiState.UNINITIALIZED;
    private boolean released;

    private TextView tvAlbumName;
    private ImageView btnBack;
    private FrameLayout contentContainer;
    private AlbumMetaInfoView albumMetaInfo;

    private AlbumUiHelper albumUi;

    // Data
    private AlbumLoader albumLoader;

    private final List<AlbumMedia> currentMedia = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!readIntent()) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_album);
        applyWindowInsets();

        bindViews();
        bindHeader();
        setupBackHandling();

        albumLoader = new AlbumLoader(this, albumId);


        albumUi = new AlbumUiHelper(
                this,
                contentContainer,
                this
        );

        loadAlbum();

        Log.d(TAG, "Opened album: " + albumName + " (" + albumId + ")");
    }

    /* ---------------- Load flow ---------------- */

    private void loadAlbum() {
        if (released || state != UiState.UNINITIALIZED) return;

        moveToState(UiState.LOADING);

        albumLoader.load(new AlbumLoader.Callback() {
            @Override
            public void onDataLoaded() {
                if (released) return;
                renderFromCache();
            }

            @Override
            public void onError(Exception e) {
                if (released) return;

                Log.e(TAG, "Album load failed", e);
                moveToState(UiState.ERROR);
            }
        });
    }



    private void renderFromCache() {
        int photos = 0;
        int videos = 0;

        List<AlbumMedia> mediaList = new ArrayList<>();
        for (AlbumMedia m : albumLoader.getMedia()) {
            mediaList.add(m);
            if (m.isVideo) videos++;
            else photos++;
        }

        albumMetaInfo.setCounts(photos, videos);

        currentMedia.clear();
        currentMedia.addAll(mediaList);

        if (currentMedia.isEmpty()) {
            moveToState(UiState.EMPTY);
        } else {
            moveToState(UiState.CONTENT);
        }
    }



    /* ---------------- UI callbacks ---------------- */

    @Override
    public void onAddMediaClicked() {
        if (released) return;
        Log.d(TAG, "Add media requested for album=" + albumId);
        // picker / sheet later
    }

    @Override
    public void onMediaClicked(AlbumMedia media, int position) {
        if (released) return;
        Log.d(TAG, "Media clicked pos=" + position);
    }

    @Override
    public void onMediaLongPressed(AlbumMedia media, int position) {
        if (released) return;
        Log.d(TAG, "Media long-pressed pos=" + position);
    }

    private void moveToState(UiState newState) {
        if (state == newState) return;

        Log.d(TAG, "state " + state + " → " + newState);
        state = newState;

        switch (newState) {
            case LOADING:
                albumUi.showLoading();
                break;

            case EMPTY:
            case ERROR:
                albumUi.showEmpty();
                break;

            case CONTENT:
                albumUi.showContent(currentMedia);
                break;

            case UNINITIALIZED:
                // no-op
                break;
        }
    }



    /* ---------------- Intent ---------------- */

    private boolean readIntent() {
        albumId = getIntent().getStringExtra(EXTRA_ALBUM_ID);
        albumName = getIntent().getStringExtra(EXTRA_ALBUM_NAME);
        return albumId != null && !albumId.isEmpty();
    }

    /* ---------------- Views ---------------- */

    private void bindViews() {
        tvAlbumName = findViewById(R.id.tvAlbumName);
        btnBack = findViewById(R.id.btnBack);
        contentContainer = findViewById(R.id.stateContainer);
        albumMetaInfo = findViewById(R.id.albumMetaInfo);
    }

    private void bindHeader() {
        tvAlbumName.setText(albumName);
        btnBack.setOnClickListener(v -> finish());
    }

    /* ---------------- Back handling ---------------- */

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

    /* ---------------- Insets ---------------- */

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        released = true;
        if (albumLoader != null) albumLoader.release();
    }
}
