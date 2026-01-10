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

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.consent.PrimaryAccountConsentHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardBlockingHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardProfileHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.ExpandVaultHelper;
import com.github.jaykkumar01.vaultspace.models.VaultStorageState;
import com.github.jaykkumar01.vaultspace.views.creative.StorageBarView;

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

    /* Helpers */
    private DashboardBlockingHelper blocking;
    private DashboardProfileHelper profileHelper;
    private ExpandVaultHelper expandVaultHelper;

    /* UI */
    private StorageBarView storageBar;
    private TextView segmentAlbums, segmentFiles;
    private FrameLayout albumsContainer, filesContainer;
    private View btnExpandVault, btnLogout;

    /* ==========================================================
     * Lifecycle
     * ========================================================== */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        initIntent();
        initCore();
        initViews();
        initHelpers();
        initEarlyListeners();
        initBackHandling();

        Log.d(TAG, "onCreate()");
        moveToState(AuthState.INIT);
    }

    /* ==========================================================
     * Initialization only
     * ========================================================== */

    private void initIntent() {
        isFromLogin = getIntent().getBooleanExtra(EXTRA_FROM_LOGIN, false);
    }

    private void initCore() {
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
        blocking = new DashboardBlockingHelper(
                this,
                () -> exitToLogin("You have been logged out")
        );
        profileHelper = new DashboardProfileHelper(this);
        expandVaultHelper = new ExpandVaultHelper(this);
    }

    private void initEarlyListeners() {
        btnLogout.setOnClickListener(v -> blocking.confirmLogout());
    }

    private void initBackHandling() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (authState == AuthState.EXIT) return;
                        if (blocking.handleBackPress()) return;

                        if (authState == AuthState.INIT ||
                                authState == AuthState.CHECKING) {
                            blocking.confirmCancelSetup();
                        } else if (authState == AuthState.GRANTED) {
                            blocking.confirmExit(DashboardActivity.this::finish);
                        }
                    }
                });
    }

    /* ==========================================================
     * GRANTED activation
     * ========================================================== */

    private void activateGrantedState() {
        blocking.resetAll();

        segmentAlbums.setSelected(true);
        albumsContainer.setVisibility(View.VISIBLE);
        filesContainer.setVisibility(View.GONE);

        profileHelper.attach(isFromLogin);
        expandVaultHelper.observeVaultStorage(this::onVaultStorageState);

        btnExpandVault.setOnClickListener(v -> {
            blocking.showLoading();
            expandVaultHelper.launchExpandVault(expandActionListener);
        });
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
        blocking.showLoading();

        primaryEmail = userSession.getPrimaryAccountEmail();
        if (primaryEmail == null) {
            exitToLogin("Session expired. Please login again.");
            return;
        }

        moveToState(isFromLogin ? AuthState.GRANTED : AuthState.CHECKING);
    }

    private void handleChecking() {
        blocking.showLoading();
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

    private final ExpandVaultHelper.ExpandActionListener expandActionListener =
            new ExpandVaultHelper.ExpandActionListener() {
                @Override
                public void onSuccess() {
                    blocking.clearLoading();
                }

                @Override
                public void onError(@NonNull String message) {
                    blocking.clearLoading();
                    Toast.makeText(
                            DashboardActivity.this,
                            message,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            };

    /* ==========================================================
     * Exit
     * ========================================================== */

    private void exitToLogin(String message) {
        if (authState == AuthState.EXIT) return;
        authState = AuthState.EXIT;

        blocking.resetAll();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        userSession.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
