package com.github.jaykkumar01.vaultspace.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.consent.PrimaryAccountConsentHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.dashboard.albums.AlbumsVaultUiHelper;
import com.github.jaykkumar01.vaultspace.dashboard.files.FilesVaultUiHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardBlockingHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardProfileHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.ExpandVaultHelper;
import com.github.jaykkumar01.vaultspace.interfaces.VaultSectionUi;
import com.github.jaykkumar01.vaultspace.models.VaultStorageState;
import com.github.jaykkumar01.vaultspace.views.creative.StorageBarView;
import com.github.jaykkumar01.vaultspace.views.popups.BlockingOverlayView;

@SuppressLint("SetTextI18n")
public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Dashboard";
    private static final String EXTRA_FROM_LOGIN = "FROM_LOGIN";

    private enum AuthState {
        UNINITIALIZED, INIT, CHECKING, GRANTED, EXIT
    }

    private AuthState authState = AuthState.UNINITIALIZED;

    /* Core */
    private UserSession userSession;
    private String primaryEmail;
    private PrimaryAccountConsentHelper consentHelper;
    private boolean isFromLogin;
    private BlockingOverlayView blockingOverlay;

    /* Helpers */
    private DashboardBlockingHelper blockingHelper;
    private DashboardProfileHelper profileHelper;
    private ExpandVaultHelper expandVaultHelper;

    /* UI */
    private StorageBarView storageBar;
    private TextView segmentAlbums, segmentFiles;
    private FrameLayout albumsContainer, filesContainer;
    private View btnExpandVault, btnLogout;

    /* ---------------- Vault Section UI ---------------- */

    private VaultSectionUi albumsUi;
    private VaultSectionUi filesUi;

    /* ---------------- State ---------------- */

    private VaultViewMode currentViewMode;

    public enum VaultViewMode {
        ALBUMS,
        FILES
    }

    /* ==========================================================
     * Lifecycle
     * ========================================================== */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        applyWindowInsets();

        initIntent();
        initCore();
        initViews();
        initHelpers();
        initEarlyListeners();
        initVaultSections();
        initBackHandling();

        Log.d(TAG, "onCreate()");
        moveToState(AuthState.INIT);
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    /* ==========================================================
     * Initialization only
     * ========================================================== */

    private void initIntent() {
        isFromLogin = getIntent().getBooleanExtra(EXTRA_FROM_LOGIN, false);
    }

    private void initCore() {
        blockingOverlay = BlockingOverlayView.attach(this);
        userSession = new UserSession(this);
        consentHelper = new PrimaryAccountConsentHelper(this);
    }

    private void initViews() {
        storageBar = findViewById(R.id.storageBar);
        segmentAlbums = findViewById(R.id.segmentAlbums);
        segmentFiles = findViewById(R.id.segmentFiles);
        albumsContainer = findViewById(R.id.albumsContainer);
        filesContainer = findViewById(R.id.filesContainer);
        btnExpandVault = findViewById(R.id.btnExpandVault);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void initHelpers() {
        blockingHelper = new DashboardBlockingHelper(
                blockingOverlay,
                () -> exitToLogin("You have been logged out")
        );
        profileHelper = new DashboardProfileHelper(this);
        expandVaultHelper = new ExpandVaultHelper(this);
    }

    private void initEarlyListeners() {
        btnLogout.setOnClickListener(v -> blockingHelper.confirmLogout());
    }

    /* ---------------- Init: Vault Sections ---------------- */

    private void initVaultSections() {
        albumsUi = new AlbumsVaultUiHelper(this, albumsContainer, blockingOverlay);
        filesUi = new FilesVaultUiHelper(this, filesContainer, blockingOverlay);
    }

    private void initBackHandling() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (authState == AuthState.EXIT) return;

                        boolean consumedByVaultUi = false;

                        if (currentViewMode == VaultViewMode.ALBUMS && albumsUi != null) {
                            consumedByVaultUi = albumsUi.onBackPressed();
                        } else if (currentViewMode == VaultViewMode.FILES && filesUi != null) {
                            consumedByVaultUi = filesUi.onBackPressed();
                        }

                        if (consumedByVaultUi) return;

                        if (blockingOverlay.isConfirmVisible()){
                            blockingOverlay.dismissConfirm();
                            return;
                        }

                        if (authState == AuthState.INIT ||
                                authState == AuthState.CHECKING) {
                            blockingHelper.confirmCancelSetup();
                        } else if (authState == AuthState.GRANTED) {
                            blockingHelper.confirmExit(DashboardActivity.this::finish);
                        }
                    }
                });
    }

    /* ==========================================================
     * GRANTED activation
     * ========================================================== */

    private void activateGrantedState() {
        blockingHelper.resetAll();

        applyViewMode(VaultViewMode.ALBUMS);
        segmentAlbums.setOnClickListener(v -> applyViewMode(VaultViewMode.ALBUMS));
        segmentFiles.setOnClickListener(v -> applyViewMode(VaultViewMode.FILES));

        profileHelper.attach(isFromLogin);
        expandVaultHelper.observeVaultStorage(this::onVaultStorageState);

        btnExpandVault.setOnClickListener(v -> {
            blockingHelper.showLoading();
            expandVaultHelper.launchExpandVault(expandAccountListener);
        });
    }
    /* ---------------- View Mode ---------------- */

    private void applyViewMode(VaultViewMode mode) {
        if (currentViewMode == mode) return;

        currentViewMode = mode;
        boolean showAlbums = mode == VaultViewMode.ALBUMS;

        segmentAlbums.setSelected(showAlbums);
        segmentFiles.setSelected(!showAlbums);

        albumsContainer.setVisibility(showAlbums ? View.VISIBLE : View.GONE);
        filesContainer.setVisibility(showAlbums ? View.GONE : View.VISIBLE);

        if (showAlbums) {
            albumsUi.show();
        } else {
            filesUi.show();
        }
    }

    /* ==========================================================
     * State Machine (execution)
     * ========================================================== */

    private void moveToState(AuthState newState) {
        if (authState == newState) return;

        Log.d(TAG, "AuthState transition: " + authState + " → " + newState);
        authState = newState;

        switch (newState) {
            case INIT:
                handleInit();
                break;
            case CHECKING:
                handleChecking();
                break;
            case GRANTED:
                activateGrantedState();
                break;
        }
    }

    private void handleInit() {
        blockingHelper.showLoading();

        primaryEmail = userSession.getPrimaryAccountEmail();
        if (primaryEmail == null) {
            exitToLogin("Session expired. Please login again.");
            return;
        }

        moveToState(isFromLogin ? AuthState.GRANTED : AuthState.CHECKING);
    }

    private void handleChecking() {
        blockingHelper.showLoading();
        consentHelper.checkConsentsSilently(primaryEmail, result -> {
            switch (result) {
                case GRANTED:
                    moveToState(AuthState.GRANTED);
                    break;
                case RECOVERABLE:
                case FAILED:
                    exitToLogin("Permissions were revoked. Please login again.");
                    break;
            }
        });
    }

    /* ==========================================================
     * Callbacks
     * ========================================================== */

    private void onVaultStorageState(VaultStorageState state) {
        storageBar.setUsage(state.used, state.total, state.unit);
    }

    private final ExpandVaultHelper.ExpandAccountListener expandAccountListener =
            new ExpandVaultHelper.ExpandAccountListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Expand Vault success");
                    blockingHelper.clearLoading();
                }

                @Override
                public void onError(@NonNull String message) {
                    Log.e(TAG, "Expand Vault error: " + message);
                    blockingHelper.clearLoading();
                    showToast(message);
                }

            };

    /* ==========================================================
     * Exit
     * ========================================================== */

    private void exitToLogin(String message) {
        if (authState == AuthState.EXIT) return;
        authState = AuthState.EXIT;

        blockingHelper.resetAll();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        boolean internetWorking = true;
        if (internetWorking) {
            userSession.clearSession();
        }
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (albumsUi != null) albumsUi.onRelease();
        if (filesUi != null) filesUi.onRelease();
    }
}
