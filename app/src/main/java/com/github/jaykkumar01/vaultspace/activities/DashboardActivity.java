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
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardModalCoordinator;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardProfileHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.ExpandVaultHelper;
import com.github.jaykkumar01.vaultspace.interfaces.VaultSectionUi;

import com.github.jaykkumar01.vaultspace.models.MediaSelection;
import com.github.jaykkumar01.vaultspace.models.VaultStorageState;
import com.github.jaykkumar01.vaultspace.views.creative.StorageBarView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;

import java.util.List;
import java.util.Map;

@SuppressLint("SetTextI18n")
public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Dashboard";
    private static final String EXTRA_FROM_LOGIN = "FROM_LOGIN";

    public enum AuthState {
        UNINITIALIZED, INIT, CHECKING, GRANTED, EXIT
    }

    private AuthState authState = AuthState.UNINITIALIZED;

    /* Core */
    private UserSession userSession;
    private String primaryEmail;
    private PrimaryAccountConsentHelper consentHelper;
    private boolean isFromLogin;

    /* Modals */
    private ModalHost modalHost;
    private DashboardModalCoordinator modalCoordinator;

    /* Helpers */
    private DashboardProfileHelper profileHelper;
    private ExpandVaultHelper expandVaultHelper;

    /* UI */
    private StorageBarView storageBar;
    private TextView segmentAlbums, segmentFiles;
    private FrameLayout albumsContainer, filesContainer;
    private View btnExpandVault, btnLogout;

    /* Vault UI */
    private VaultSectionUi albumsUi;
    private VaultSectionUi filesUi;

    private VaultViewMode currentViewMode;

    public enum VaultViewMode {
        ALBUMS, FILES
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
        initVaultSections();
        initListeners();
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
     * Init
     * ========================================================== */

    private void initIntent() {
        isFromLogin = getIntent().getBooleanExtra(EXTRA_FROM_LOGIN, false);
    }

    private void initCore() {
        modalHost = ModalHost.attach(this);
        modalCoordinator = new DashboardModalCoordinator(
                modalHost,
                () -> exitToLogin("You have been logged out")
        );
        userSession = new UserSession(this);
        profileHelper = new DashboardProfileHelper(this);
        consentHelper = new PrimaryAccountConsentHelper(this);
        expandVaultHelper = new ExpandVaultHelper(this);
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

    private void initVaultSections() {
        albumsUi = new AlbumsVaultUiHelper(this, albumsContainer, modalHost);
        filesUi = new FilesVaultUiHelper(this, filesContainer, modalHost);
    }

    private void initListeners() {
        btnLogout.setOnClickListener(v -> modalCoordinator.confirmLogout());
    }

    private void initBackHandling() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (authState == AuthState.EXIT) return;

                        if (modalHost.onBackPressed()){
                            return;
                        }

                        if (currentViewMode == VaultViewMode.ALBUMS && albumsUi.onBackPressed()) return;
                        if (currentViewMode == VaultViewMode.FILES && filesUi.onBackPressed()) return;


                        modalCoordinator.handleBackPress(
                                authState,
                                DashboardActivity.this::finish,
                                () -> moveToState(AuthState.INIT)
                        );

                    }
                });
    }

    /* ==========================================================
     * State machine
     * ========================================================== */

    private void moveToState(AuthState newState) {
        if (authState == newState) return;

        Log.d(TAG, "AuthState: " + authState + " → " + newState);
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
        modalCoordinator.showLoading();

        primaryEmail = userSession.getPrimaryAccountEmail();
        if (primaryEmail == null) {
            exitToLogin("Session expired. Please login again.");
            return;
        }

        moveToState(isFromLogin ? AuthState.GRANTED : AuthState.CHECKING);
    }


    private void handleChecking() {
        modalCoordinator.showLoading();

        consentHelper.checkConsentsSilently(primaryEmail, result -> {
            switch (result) {
                case GRANTED:
                    moveToState(AuthState.GRANTED);
                    break;
                case TEMPORARY_UNAVAILABLE:
                    modalCoordinator.clearLoading();

                    modalCoordinator.confirmRetryConsent(
                            // Retry
                            () -> moveToState(AuthState.INIT),

                            // Exit
                            this::finish
                    );
                    break;
                default:
                    exitToLogin("Permissions were revoked. Please login again.");
            }
        });
    }

    private void activateGrantedState() {
        modalCoordinator.reset();
        profileHelper.attach(isFromLogin);


        // 🔍 DEBUG: log retry data (read all)
        logUploadRetries();

        applyViewMode(VaultViewMode.ALBUMS);
        segmentAlbums.setOnClickListener(v -> applyViewMode(VaultViewMode.ALBUMS));
        segmentFiles.setOnClickListener(v -> applyViewMode(VaultViewMode.FILES));

        expandVaultHelper.observeVaultStorage(this::onVaultStorageState);

        btnExpandVault.setOnClickListener(v -> {
            modalCoordinator.showLoading();
            expandVaultHelper.launchExpandVault(expandAccountListener);
        });
    }

    private void logUploadRetries() {
        Map<String, List<MediaSelection>> retries =
                userSession.getUploadRetryStore().getAllRetries();

        if (retries.isEmpty()) {
            Log.d(TAG, "UploadRetryStore: no retry entries");
            return;
        }

        Log.d(TAG, "UploadRetryStore: total albums with retry = " + retries.size());

        for (Map.Entry<String, List<MediaSelection>> entry
                : retries.entrySet()) {

            String albumId = entry.getKey();
            List<MediaSelection> list = entry.getValue();

            Log.d(TAG,
                    "UploadRetryStore: albumId=" + albumId +
                            ", retryCount=" + list.size());

            for (MediaSelection s : list) {
                Log.d(TAG, "  ↳ " + s);
            }
        }
    }


    /* ==========================================================
     * UI helpers
     * ========================================================== */

    private void applyViewMode(VaultViewMode mode) {
        if (currentViewMode == mode) return;

        currentViewMode = mode;
        boolean showAlbums = mode == VaultViewMode.ALBUMS;

        segmentAlbums.setSelected(showAlbums);
        segmentFiles.setSelected(!showAlbums);

        albumsContainer.setVisibility(showAlbums ? View.VISIBLE : View.GONE);
        filesContainer.setVisibility(showAlbums ? View.GONE : View.VISIBLE);

        if (showAlbums) albumsUi.show();
        else filesUi.show();
    }

    private void onVaultStorageState(VaultStorageState state) {
        storageBar.setUsage(state.used, state.total, state.unit);
    }

    private final ExpandVaultHelper.ExpandAccountListener expandAccountListener =
            new ExpandVaultHelper.ExpandAccountListener() {
                @Override public void onSuccess() {
                    modalCoordinator.clearLoading();
                }
                @Override public void onError(@NonNull String message) {
                    modalCoordinator.clearLoading();
                    showToast(message);
                }
            };

    /* ==========================================================
     * Exit
     * ========================================================== */

    private void exitToLogin(String message) {
        if (authState == AuthState.EXIT) return;
        authState = AuthState.EXIT;

        modalCoordinator.reset();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        userSession.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        albumsUi.onRelease();
        filesUi.onRelease();
    }
}
