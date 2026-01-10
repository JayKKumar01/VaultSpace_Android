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
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.consent.PrimaryAccountConsentHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardBlockingHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardProfileHelper;
import com.github.jaykkumar01.vaultspace.views.creative.ProfileInfoView;
import com.github.jaykkumar01.vaultspace.views.creative.StorageBarView;

@SuppressLint("SetTextI18n")
public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Dashboard";
    private static final String EXTRA_FROM_LOGIN = "FROM_LOGIN";

    /* ==========================================================
     * Auth State
     * ========================================================== */

    private enum AuthState {
        UNINITIALIZED,
        INIT,
        CHECKING,
        GRANTED,
        EXIT
    }

    private AuthState authState = AuthState.UNINITIALIZED;

    /* ==========================================================
     * Core
     * ========================================================== */

    private UserSession userSession;
    private String primaryEmail;
    private PrimaryAccountConsentHelper consentHelper;

    /* ==========================================================
     * Blocking
     * ========================================================== */

    private DashboardBlockingHelper blocking;

    /* ==========================================================
     * UI
     * ========================================================== */
    private StorageBarView storageBar;
    private TextView segmentAlbums;
    private TextView segmentFiles;
    private FrameLayout albumsContainer;
    private FrameLayout filesContainer;
    private View btnExpandVault;
    private View btnLogout;

    /* ==========================================================
     * Helpers (post-granted shell)
     * ========================================================== */

    private DashboardProfileHelper profileHelper;

    /* ==========================================================
     * Lifecycle
     * ========================================================== */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        blocking = new DashboardBlockingHelper(
                this,
                () -> exitToLogin("You have been logged out")
        );

        setupBackHandling();

        Log.d(TAG, "onCreate()");
        moveToState(AuthState.INIT);
    }

    /* ==========================================================
     * Post-granted
     * ========================================================== */

    private void handleGranted() {
        blocking.resetAll();
        initViews();
        initUiShell();
    }

    private void initViews() {
        storageBar = findViewById(R.id.storageBar);
        segmentAlbums = findViewById(R.id.segmentAlbums);
        segmentFiles = findViewById(R.id.segmentFiles);
        albumsContainer = findViewById(R.id.albumsContainer);
        filesContainer = findViewById(R.id.filesContainer);
        btnExpandVault = findViewById(R.id.btnExpandVault);
        btnLogout = findViewById(R.id.btnLogout);
        profileHelper = new DashboardProfileHelper(this);
    }

    private void initUiShell() {
        segmentAlbums.setSelected(true);
        albumsContainer.setVisibility(View.VISIBLE);
        filesContainer.setVisibility(View.GONE);

        btnExpandVault.setEnabled(false);
        btnLogout.setOnClickListener(v -> blocking.confirmLogout());

        // Attach helpers that affect visible UI
        profileHelper.attach();
    }

    /* ==========================================================
     * Back handling
     * ========================================================== */

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {

                        if (authState == AuthState.EXIT) return;
                        if (blocking.handleBackPress()) return;

                        if (authState == AuthState.INIT ||
                                authState == AuthState.CHECKING) {
                            blocking.confirmCancelSetup();
                            return;
                        }

                        if (authState == AuthState.GRANTED) {
                            blocking.confirmExit(DashboardActivity.this::finish);
                        }
                    }
                });
    }

    /* ==========================================================
     * State machine
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
                handleGranted();
                break;
            case EXIT:
                break;
        }
    }

    private void handleInit() {
        blocking.showLoading();

        userSession = new UserSession(this);
        primaryEmail = userSession.getPrimaryAccountEmail();

        if (primaryEmail == null) {
            exitToLogin("Session expired. Please login again.");
            return;
        }

        consentHelper = new PrimaryAccountConsentHelper(this);
        boolean fromLogin =
                getIntent().getBooleanExtra(EXTRA_FROM_LOGIN, false);

        moveToState(fromLogin ? AuthState.GRANTED : AuthState.CHECKING);
    }

    private void handleChecking() {
        blocking.showLoading();
        consentHelper.checkConsentsSilently(
                primaryEmail,
                result -> {
                    switch (result) {
                        case GRANTED:
                            moveToState(AuthState.GRANTED);
                            break;
                        case RECOVERABLE:
                        case FAILED:
                            exitToLogin(
                                    "Permissions were revoked. Please login again."
                            );
                            break;
                    }
                }
        );
    }

    /* ==========================================================
     * Exit
     * ========================================================== */

    private void exitToLogin(String message) {
        if (authState == AuthState.EXIT) return;
        authState = AuthState.EXIT;

        blocking.resetAll();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        if (userSession != null) userSession.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
