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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.AlbumLoader;
import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.helper.AlbumModalCoordinator;
import com.github.jaykkumar01.vaultspace.album.helper.AlbumUiHelper;
import com.github.jaykkumar01.vaultspace.album.helper.AlbumUiHelper.AlbumUiCallback;
import com.github.jaykkumar01.vaultspace.views.creative.AlbumMetaInfoView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;

import java.util.ArrayList;
import java.util.List;

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
    private SwipeRefreshLayout swipeRefresh;

    private AlbumUiHelper albumUi;
    private AlbumLoader albumLoader;
    private AlbumModalCoordinator modalCoordinator;

    private final List<AlbumMedia> currentMedia = new ArrayList<>();

    private final AlbumLoader.Callback loaderCallback = new AlbumLoader.Callback() {
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
    };

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
        setupRefresh();
        bindHeader();
        setupBackHandling();

        ModalHost modalHost = ModalHost.attach(this);
        modalCoordinator = new AlbumModalCoordinator(
                modalHost,
                this::refreshAlbum,
                this::finish
        );

        initAlbumUi();
        loadAlbum();

        Log.d(TAG, "Opened album: " + albumName + " (" + albumId + ")");
    }

    /* ---------------- Load & Refresh ---------------- */

    private void initAlbumUi() {
        albumLoader = new AlbumLoader(this, albumId);
        albumUi = new AlbumUiHelper(this, contentContainer, this);
    }

    private void loadAlbum() {
        if (released || state != UiState.UNINITIALIZED) return;
        moveToState(UiState.LOADING);
        albumLoader.load(loaderCallback);
    }

    private void refreshAlbum() {
        if (released) {
            swipeRefresh.setRefreshing(false);
            return;
        }
        moveToState(UiState.LOADING);
        albumLoader.refresh(loaderCallback);
    }

    /* ---------------- Render ---------------- */

    private void renderFromCache() {
        int photos = 0;
        int videos = 0;

        List<AlbumMedia> mediaList = albumLoader.getMedia();
        for (AlbumMedia m : mediaList) {
            if (m.isVideo) videos++;
            else photos++;
        }

        albumMetaInfo.setCounts(photos, videos);

        currentMedia.clear();
        currentMedia.addAll(mediaList);

        moveToState(currentMedia.isEmpty() ? UiState.EMPTY : UiState.CONTENT);
    }

    /* ---------------- UI State ---------------- */

    private void moveToState(UiState newState) {
        if (state == newState) return;

        Log.d(TAG, "state " + state + " → " + newState);
        state = newState;

        if (newState != UiState.LOADING) {
            swipeRefresh.setRefreshing(false);
        }

        switch (newState) {
            case LOADING:
                albumUi.showLoading();
                break;

            case EMPTY:
                albumUi.showEmpty();
                break;

            case ERROR:
                modalCoordinator.showRetryLoad();
                break;

            case CONTENT:
                modalCoordinator.dismissAll();
                albumUi.showContent(currentMedia);
                break;

            case UNINITIALIZED:
                break;
        }
    }

    /* ---------------- UI Callbacks ---------------- */

    @Override
    public void onAddMediaClicked() {
        if (released) return;
        Log.d(TAG, "Add media requested for album=" + albumId);
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

    /* ---------------- Setup ---------------- */

    private void setupRefresh() {
        swipeRefresh.setOnRefreshListener(this::refreshAlbum);
        swipeRefresh.setColorSchemeResources(
                R.color.vs_accent_primary,
                R.color.vs_brand_text
        );
    }

    private boolean readIntent() {
        albumId = getIntent().getStringExtra(EXTRA_ALBUM_ID);
        albumName = getIntent().getStringExtra(EXTRA_ALBUM_NAME);
        return albumId != null && !albumId.isEmpty();
    }

    private void bindViews() {
        tvAlbumName = findViewById(R.id.tvAlbumName);
        btnBack = findViewById(R.id.btnBack);
        contentContainer = findViewById(R.id.stateContainer);
        albumMetaInfo = findViewById(R.id.albumMetaInfo);
        swipeRefresh = findViewById(R.id.swipeRefresh);
    }

    private void bindHeader() {
        tvAlbumName.setText(albumName);
        btnBack.setOnClickListener(v -> finish());
    }

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
        if (modalCoordinator != null) modalCoordinator.dismissAll();
        if (albumLoader != null) albumLoader.release();
    }
}
