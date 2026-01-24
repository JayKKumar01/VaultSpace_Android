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
import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.coordinator.AlbumActionCoordinator;
import com.github.jaykkumar01.vaultspace.album.helper.AlbumModalHandler;
import com.github.jaykkumar01.vaultspace.album.helper.AlbumUiController;
import com.github.jaykkumar01.vaultspace.core.drive.AlbumMediaRepository;
import com.github.jaykkumar01.vaultspace.core.upload.controller.UploadStatusController;
import com.github.jaykkumar01.vaultspace.core.upload.UploadOrchestrator;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadObserver;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSnapshot;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;
import com.github.jaykkumar01.vaultspace.views.creative.AlbumMetaInfoView;
import com.github.jaykkumar01.vaultspace.views.creative.upload.item.ProgressStackView;
import com.github.jaykkumar01.vaultspace.views.creative.upload.UploadStatusView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;

import java.util.List;

public class AlbumActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Album";

    public static final String EXTRA_ALBUM_ID = "album_id";
    public static final String EXTRA_ALBUM_NAME = "album_name";

    private enum UiState {UNINITIALIZED, LOADING, EMPTY, CONTENT, ERROR}

    private String albumId;
    private String albumName;

    private UiState state = UiState.UNINITIALIZED;
    private boolean released;

    private TextView tvAlbumName;
    private ImageView btnBack;
    private FrameLayout contentContainer;
    private AlbumMetaInfoView albumMetaInfo;
    private UploadStatusView uploadStatusView;
    private ProgressStackView progressStackView;

    private AlbumUiController albumUiController;
    private AlbumMediaRepository albumRepo;
    private ModalHost modalHost;
    private AlbumModalHandler albumModalHandler;

    private AlbumActionCoordinator actionCoordinator;
    private UploadOrchestrator uploadOrchestrator;

    private UploadStatusController uploadStatusController;

    private boolean shouldClearGroup;

    private final AlbumMediaRepository.Callback repoCallback = new AlbumMediaRepository.Callback() {
        @Override
        public void onLoaded() {
            if (released) return;
            renderFromCache();
        }

        @Override
        public void onError(Exception e) {
            if (released) return;
            Log.e(TAG, "Album load failed", e);
            transitionTo(UiState.ERROR);
        }
    };

    private final AlbumActionCoordinator.Listener actionListener = new AlbumActionCoordinator.Listener() {
        @Override
        public void onMediaSelected(int size) {
            // show something to user till it get's snapshot
        }

        @Override
        public void onMediaResolved(List<UploadSelection> selections) {
            shouldClearGroup = false;
            uploadOrchestrator.enqueue(albumId, albumName, selections);
        }
    };

    private final AlbumUiController.Callback uiCallback = new AlbumUiController.Callback() {
        @Override
        public void onAddMediaClicked() {
            actionCoordinator.onAddMediaClicked();
        }

        @Override
        public void onMediaClicked(AlbumMedia media, int position) {
            actionCoordinator.onMediaClicked(media, position);
        }

        @Override
        public void onMediaLongPressed(AlbumMedia media, int position) {
            actionCoordinator.onMediaLongPressed(media, position);
        }
    };

    private final UploadObserver uploadObserver = new UploadObserver() {
        @Override
        public void onSnapshot(UploadSnapshot snapshot) {
            if (snapshot == null) {
                return;
            }
            Log.d(
                    TAG,
                    "Upload snapshot → album=" + snapshot.groupId
                            + " uploaded=" + snapshot.uploaded
                            + " failed=" + snapshot.failed
                            + " total=" + snapshot.total
                            + " inProgress=" + snapshot.isInProgress()
            );
            uploadStatusController.onSnapshot(snapshot);
        }

        @Override
        public void onCancelled() {
            uploadStatusController.onCancelled();
        }

        @Override
        public void onSuccess(UploadedItem item) {
            if (item == null) {
                return;
            }
            AlbumMedia media = new AlbumMedia(item);

            if (state == UiState.CONTENT) {
                albumUiController.addMedia(media);
                return;
            }

            if (state != UiState.UNINITIALIZED) {
                renderFromCache();
            }
        }


        @Override
        public void onFailure(UploadSelection selection) {
            if (selection == null) {
                return;
            }
            uploadStatusController.onFailure(selection);
        }

        @Override
        public void onProgress(String uId, String name, long uploadedBytes, long totalBytes) {
            uploadStatusController.onProgress(uId, name, uploadedBytes, totalBytes);
        }
    };

    private final UploadStatusController.Callback uploadStatusCallback = new UploadStatusController.Callback() {
        @Override
        public void onCancelRequested() {
            albumModalHandler.showCancelConfirm(() -> uploadOrchestrator.cancelUploads(albumId));
        }

        @Override
        public void onRetryRequested() {
            uploadOrchestrator.retryUploads(albumId, albumName);
        }

        @Override
        public void onAcknowledge() {
            uploadOrchestrator.clearGroup(albumId);
        }

        @Override
        public void onNoAccessInfo() {
            shouldClearGroup = true;
            uploadOrchestrator.getFailuresForGroup(albumId, failures -> {
                if (failures == null || failures.isEmpty()) {
                    Log.d(TAG, "onNoAccessInfo(): no failures for groupId=" + albumId);
                    return;
                }


                albumModalHandler.showUploadFailures(failures, () -> {
                    Log.d(TAG, "onNoAccessInfo(): groupId=" + albumId + ", totalFailures=" + failures.size());

                    uploadOrchestrator.clearGroup(albumId);
                    shouldClearGroup = false;
                });
            });
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
        bindHeader();
        setupBackHandling();

        modalHost = ModalHost.attach(this);
        albumModalHandler = new AlbumModalHandler(modalHost);

        albumRepo = AlbumMediaRepository.getInstance(this);
        albumRepo.addCountListener(albumId,this::onCountChanged);

        albumUiController = new AlbumUiController(this, contentContainer, uiCallback);
        actionCoordinator = new AlbumActionCoordinator(this, albumId, actionListener);


        uploadStatusController = new UploadStatusController(uploadStatusView, progressStackView, uploadStatusCallback);

        uploadOrchestrator = UploadOrchestrator.getInstance(this);
        uploadOrchestrator.registerObserver(albumId, albumName, uploadObserver);


        loadAlbum();

        Log.d(TAG, "Opened album: " + albumName + " (" + albumId + ")");
    }

    private void onCountChanged(int photos,int videos) {
        albumMetaInfo.setCounts(photos,videos);
    }


    /* ---------------- Load / Refresh ---------------- */

    private void loadAlbum() {
        if (released || state != UiState.UNINITIALIZED) return;
        transitionTo(UiState.LOADING);
        albumRepo.loadAlbum(this, albumId, repoCallback);
    }

    private void refreshAlbum() {
        if (released) {
            return;
        }
        transitionTo(UiState.LOADING);
        albumRepo.refreshAlbum(this, albumId, repoCallback);
    }

    /* ---------------- Rendering ---------------- */

    private void renderFromCache() {
        List<AlbumMedia> snapshot = albumRepo.getMediaSnapshot(albumId);

        transitionTo(snapshot.isEmpty() ? UiState.EMPTY : UiState.CONTENT);
    }

    /* ---------------- State Machine ---------------- */

    private void transitionTo(UiState newState) {
        if (state == newState) return;

        Log.d(TAG, "state " + state + " → " + newState);
        state = newState;

        switch (newState) {
            case LOADING:
                renderLoading();
                break;

            case EMPTY:
                renderEmpty();
                break;

            case ERROR:
                renderError();
                break;

            case CONTENT:
                renderContent();
                break;

            case UNINITIALIZED:
                // no-op
                break;
        }
    }

    private void renderLoading() {
        albumUiController.showLoading();
    }

    private void renderEmpty() {
        albumUiController.showEmpty();
    }

    private void renderError() {
        albumModalHandler.showRetryLoad(this::refreshAlbum, this::finish);
    }

    private void renderContent() {
        albumModalHandler.dismissAll();
        albumUiController.showContent(albumRepo.getMediaSnapshot(albumId));

    }



    /* ---------------- Setup ---------------- */

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
        uploadStatusView = findViewById(R.id.uploadStatusView);
        progressStackView = findViewById(R.id.uploadItemProgress);
    }

    private void bindHeader() {
        tvAlbumName.setText(albumName);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                if (modalHost.onBackPressed()) return;

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
        albumModalHandler.dismissAll();
        actionCoordinator.release();
        uploadOrchestrator.unregisterObserver(albumId);
        if (shouldClearGroup) {
            uploadOrchestrator.clearGroup(albumId);
        }
        albumRepo.removeCountListener(albumId, this::onCountChanged);
    }
}
