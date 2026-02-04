package com.github.jaykkumar01.vaultspace.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.model.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.coordinator.AlbumActionCoordinator;
import com.github.jaykkumar01.vaultspace.album.helper.AlbumModalHandler;
import com.github.jaykkumar01.vaultspace.album.helper.AlbumUiController;
import com.github.jaykkumar01.vaultspace.album.listener.AlbumActionListenerImpl;
import com.github.jaykkumar01.vaultspace.album.listener.AlbumMediaDeltaListenerImpl;
import com.github.jaykkumar01.vaultspace.album.listener.AlbumStateListenerImpl;
import com.github.jaykkumar01.vaultspace.album.listener.AlbumUiCallbackImpl;
import com.github.jaykkumar01.vaultspace.album.listener.UploadObserverImpl;
import com.github.jaykkumar01.vaultspace.album.listener.UploadStatusCallbackImpl;
import com.github.jaykkumar01.vaultspace.core.drive.AlbumMediaRepository;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.core.upload.UploadOrchestrator;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadObserver;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSnapshot;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;
import com.github.jaykkumar01.vaultspace.core.upload.controller.UploadStatusController;
import com.github.jaykkumar01.vaultspace.views.creative.AlbumMetaInfoView;
import com.github.jaykkumar01.vaultspace.views.creative.upload.UploadStatusView;
import com.github.jaykkumar01.vaultspace.views.creative.upload.item.ProgressStackView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;

import java.util.List;

public class AlbumActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Album";
    public static final String EXTRA_ALBUM_ID = "album_id";
    public static final String EXTRA_ALBUM_NAME = "album_name";

    /* ---------- Intent ---------- */
    private String albumId, albumName;

    /* ---------- UI Views ---------- */
    private TextView tvAlbumName;
    private ImageView btnBack;
    private FrameLayout contentContainer;
    private AlbumMetaInfoView albumMetaInfo;
    private UploadStatusView uploadStatusView;
    private ProgressStackView progressStackView;

    /* ---------- UI State ---------- */
    private enum UiState {IDLE, LOADING, EMPTY, CONTENT, ERROR}

    private UiState uiState = UiState.IDLE;
    private boolean released;
    private Iterable<AlbumMedia> currentMedia;

    /* ---------- UI Helpers ---------- */
    private AlbumUiController uiController;
    private ModalHost modalHost;
    private AlbumModalHandler albumModalHandler;

    /* ---------- Domain ---------- */
    private AlbumMediaRepository repo;
    private TrustedAccountsRepository trustedAccountsRepo;

    /* ---------- Actions & Uploads ---------- */
    private AlbumActionCoordinator actionCoordinator;
    private UploadOrchestrator uploadOrchestrator;
    private UploadStatusController uploadStatusController;
    private boolean shouldClearGroup;

    /* ---------- Listeners ---------- */

    private final AlbumMediaRepository.AlbumStateListener stateListener =
            new AlbumStateListenerImpl(this::handleAlbumLoading, this::handleAlbumMedia, this::handleAlbumError);

    private final AlbumMediaRepository.MediaDeltaListener deltaListener =
            new AlbumMediaDeltaListenerImpl(this::handleMediaAdded, this::handleMediaRemoved);

    private final UploadObserver uploadObserver =
            new UploadObserverImpl(this::handleUploadSnapshot, this::handleUploadCancelled,
                    this::handleUploadSuccess, this::handleUploadFailure, this::handleUploadProgress);

    private final AlbumUiController.Callback uiCallback =
            new AlbumUiCallbackImpl(this::handleAddMediaClicked, this::handleMediaClicked, this::handleMediaLongPressed);

    private final AlbumActionCoordinator.Listener actionListener =
            new AlbumActionListenerImpl(this::handleMediaSelected, this::handleMediaResolved);

    private final UploadStatusController.Callback uploadStatusCallback =
            new UploadStatusCallbackImpl(this::handleUploadCancel, this::handleUploadRetry,
                    this::handleUploadAcknowledge, this::handleUploadNoAccess);

    /* ---------- Lifecycle ---------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (noIntent()) {
            finish();
            return;
        }

        setupWindow();
        setupViews();
        setupUiHelpers();
        setupRepository();
        setupActions();
        setupUploads();

        repo.openAlbum(this, albumId);
        Log.d(TAG, "Opened album: " + albumName + " (" + albumId + ")");
    }

    /* ---------- Setup ---------- */

    private void setupWindow() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_album);
        applyWindowInsets();
    }

    private void setupViews() {
        bindViews();
        bindHeader();
        setupBackHandling();
    }

    private void setupUiHelpers() {
        modalHost = ModalHost.attach(this);
        albumModalHandler = new AlbumModalHandler(modalHost);
        uiController = new AlbumUiController(this, contentContainer, uiCallback, albumId);
    }

    private void setupRepository() {
        repo = AlbumMediaRepository.getInstance(this);
        trustedAccountsRepo = TrustedAccountsRepository.getInstance(this);
        repo.addAlbumStateListener(albumId, stateListener);
        repo.addDeltaListener(albumId, deltaListener);
        repo.addCountListener(albumId, this::onCountChanged);
    }

    private void setupActions() {
        actionCoordinator = new AlbumActionCoordinator(this, albumId, actionListener);
    }

    private void setupUploads() {
        uploadStatusController = new UploadStatusController(uploadStatusView, progressStackView, uploadStatusCallback);
        uploadOrchestrator = UploadOrchestrator.getInstance(this);
        uploadOrchestrator.registerObserver(albumId, albumName, uploadObserver);
    }

    /* ---------- Repository Callbacks ---------- */

    private void handleAlbumLoading() {
        if (released) return;
        transitionTo(UiState.LOADING);
    }

    private void handleAlbumMedia(Iterable<AlbumMedia> media) {
        if (released) return;
        currentMedia = media;
        transitionTo(isMediaEmpty() ? UiState.EMPTY : UiState.CONTENT);
    }

    private void handleAlbumError(Exception e) {
        if (released) return;
        transitionTo(UiState.ERROR);
    }

    private void handleMediaAdded(AlbumMedia media) {
        if (released || (uiState != UiState.EMPTY && uiState != UiState.CONTENT)) return;

        if (uiState == UiState.CONTENT) {
            uiController.onMediaAdded(media);
            return;
        }

        // EMPTY → CONTENT is structural
        transitionTo(UiState.CONTENT);


    }

    private void handleMediaRemoved(String mediaId) {
        if (released || (uiState != UiState.EMPTY && uiState != UiState.CONTENT)) return;

        if (uiState == UiState.CONTENT && isMediaEmpty()) {
            transitionTo(UiState.EMPTY);
            return;
        }

        uiController.onMediaRemoved(mediaId);

    }

    private void onCountChanged(int photos, int videos) {
        albumMetaInfo.setCounts(photos, videos);
    }

    /* ---------- UI Callbacks ---------- */

    private void handleAddMediaClicked() {
        trustedAccountsRepo.getAccounts(accounts -> {
            if (!accounts.iterator().hasNext()) {
                Toast.makeText(this, "Add a trusted account first to upload media.", Toast.LENGTH_SHORT).show();
                return;
            }
            actionCoordinator.onAddMediaClicked();
        });
    }

    private void handleMediaClicked(AlbumMedia m) {
        albumModalHandler.showMediaPreview(m);
    }

    private void handleMediaLongPressed(AlbumMedia m) {
        albumModalHandler.showActionList(
                () -> actionCoordinator.onDownloadMedia(m),
                () -> albumModalHandler.showDeleteConfirm(() -> {
                    albumModalHandler.showLoading();
                    actionCoordinator.onDeleteMedia(
                            m.fileId,
                            () -> {
                                repo.removeMedia(albumId, m);
                                albumModalHandler.clearLoading();
                            },
                            () -> albumModalHandler.clearLoading());
                }));
    }

    /* ---------- Action Callbacks ---------- */

    private void handleMediaSelected(int size) {
        albumModalHandler.showLoading();
    }

    private void handleMediaResolved(List<UploadSelection> selections) {
        albumModalHandler.clearLoading();
        shouldClearGroup = false;
        uploadOrchestrator.enqueue(albumId, albumName, selections);
    }

    /* ---------- Upload Observer ---------- */

    private void handleUploadSnapshot(UploadSnapshot s) {
        if (s == null) return;
        Log.d(TAG, "Upload snapshot → album=" + s.groupId + " uploaded=" + s.uploaded +
                " failed=" + s.failed + " total=" + s.total + " inProgress=" + s.isInProgress());
        uploadStatusController.onSnapshot(s);
    }

    private void handleUploadCancelled() {
        uploadStatusController.onCancelled();
    }

    private void handleUploadSuccess(UploadedItem item) {
    }

    private void handleUploadFailure(UploadSelection s) {
        if (s != null) uploadStatusController.onFailure(s);
    }

    private void handleUploadProgress(UploadSelection selection, long up, long total) {
        uploadStatusController.onProgress(selection, up, total);
    }

    /* ---------- Upload UI Callbacks ---------- */

    private void handleUploadCancel() {
        albumModalHandler.showCancelConfirm(() -> uploadOrchestrator.cancelUploads(albumId));
    }

    private void handleUploadRetry() {
        uploadOrchestrator.retryUploads(albumId, albumName);
    }

    private void handleUploadAcknowledge() {
        uploadOrchestrator.clearGroup(albumId);
    }

    private void handleUploadNoAccess() {
        shouldClearGroup = true;
        uploadOrchestrator.getFailuresForGroup(albumId, failures -> {
            if (failures == null || failures.isEmpty()) return;
            albumModalHandler.showUploadFailures(failures, () -> {
                uploadOrchestrator.clearGroup(albumId);
                shouldClearGroup = false;
            });
        });
    }

    /* ---------- State Machine ---------- */

    private void transitionTo(UiState next) {
        if (!isValidTransition(uiState, next)) {
            Log.w(TAG, "Invalid state transition: " + uiState + " → " + next);
            return;
        }
        if (uiState == next) return;


        uiState = next;

        switch (next) {
            case LOADING -> uiController.showLoading();
            case EMPTY -> renderEmpty();
            case CONTENT -> renderContent();
            case ERROR -> renderError();
            case IDLE -> {
            }
        }

    }


    private boolean isMediaEmpty() {
        return currentMedia == null || !currentMedia.iterator().hasNext();
    }


    private boolean isValidTransition(UiState from, UiState to) {
        return switch (from) {
            case IDLE ->
                    to == UiState.LOADING || to == UiState.EMPTY || to == UiState.CONTENT || to == UiState.ERROR;
            case LOADING -> to == UiState.EMPTY || to == UiState.CONTENT || to == UiState.ERROR;
            case EMPTY -> to == UiState.CONTENT || to == UiState.LOADING || to == UiState.ERROR;
            case CONTENT -> to == UiState.EMPTY || to == UiState.LOADING || to == UiState.ERROR;
            case ERROR -> to == UiState.LOADING;
        };
    }

    private void renderEmpty() {
        Log.d(TAG, "renderEmpty called");
        uiController.showEmpty();
    }

    private void renderContent() {
        Log.d(TAG, "renderContent called");
        if (currentMedia == null) return;
        uiController.showContent(currentMedia);
    }


    private void renderError() {
        albumModalHandler.showRetryLoad(this::refreshAlbum, this::finish);
    }

    /* ---------- Misc ---------- */

    private void refreshAlbum() {
        repo.refreshAlbum(this, albumId);
    }

    private boolean noIntent() {
        albumId = getIntent().getStringExtra(EXTRA_ALBUM_ID);
        albumName = getIntent().getStringExtra(EXTRA_ALBUM_NAME);
        return albumId == null || albumName == null || albumId.isEmpty() || albumName.isEmpty();
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
                overridePendingTransition(R.anim.album_return_enter, R.anim.album_return_exit);
            }
        });
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, i) -> {
            Insets b = i.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return i;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        released = true;
        albumModalHandler.dismissAll();
        actionCoordinator.release();
        uploadOrchestrator.unregisterObserver(albumId);
        if (shouldClearGroup) uploadOrchestrator.clearGroup(albumId);
        repo.removeAlbumStateListener(albumId, stateListener);
        repo.removeDeltaListener(albumId, deltaListener);
        repo.removeCountListener(albumId, this::onCountChanged);
    }
}
