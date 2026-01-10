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
import com.github.jaykkumar01.vaultspace.dashboard.albums.AlbumsVaultUiHelper;
import com.github.jaykkumar01.vaultspace.dashboard.files.FilesVaultUiHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardProfileInfoHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardStorageBarHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.ExpandVaultHelperOld;
import com.github.jaykkumar01.vaultspace.interfaces.VaultSectionUi;
import com.github.jaykkumar01.vaultspace.views.creative.ProfileInfoView;
import com.github.jaykkumar01.vaultspace.views.creative.StorageBarView;
import com.github.jaykkumar01.vaultspace.views.popups.ActivityLoadingOverlay;

@SuppressLint("SetTextI18n")
public class DashboardActivity1 extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Dashboard";
    private static final String EXTRA_FROM_LOGIN = "FROM_LOGIN";

    /* ---------------- Session ---------------- */

    private UserSession userSession;
    private String primaryEmail;
    private String profileName;

    /* ---------------- Views ---------------- */

    private ProfileInfoView profileInfoView;
    private StorageBarView storageBar;
    private ActivityLoadingOverlay loading;

    private TextView segmentAlbums;
    private TextView segmentFiles;

    private FrameLayout albumsContainer;
    private FrameLayout filesContainer;

    /* ---------------- Helpers ---------------- */

    private DashboardProfileInfoHelper profileInfoHelper;
    private DashboardStorageBarHelper storageBarHelper;
    private PrimaryAccountConsentHelper primaryAccountConsentHelper;
    private ExpandVaultHelperOld expandVaultHelperOld;

    /* ---------------- Vault Section UI ---------------- */

    private VaultSectionUi albumsUi;
    private VaultSectionUi filesUi;

    /* ---------------- State ---------------- */

    private VaultViewMode currentViewMode;

    public enum VaultViewMode {
        ALBUMS,
        FILES
    }

    /* ---------------- Lifecycle ---------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        Log.d(TAG, "onCreate()");

        loading = new ActivityLoadingOverlay(this);

        initSession();
        initViews();
        initHelpers();
        initVaultSections();
        setupBackHandling();
        initClickListeners();

        boolean fromLogin = getIntent().getBooleanExtra(EXTRA_FROM_LOGIN, false);
        handleConsent(fromLogin);

        applyViewMode(VaultViewMode.ALBUMS);
    }

    /* ---------------- Init: Session ---------------- */

    private void initSession() {
        userSession = new UserSession(this);

        primaryEmail = userSession.getPrimaryAccountEmail();
        profileName = userSession.getProfileName();

        if (primaryEmail == null) {
            forceLogout("Session expired. Please login again.");
            return;
        }

        Log.d(TAG, "Primary email = " + primaryEmail);
        Log.d(TAG, "Profile name = " + profileName);
    }

    /* ---------------- Init: Views ---------------- */

    private void initViews() {
        profileInfoView = findViewById(R.id.profileInfo);
        storageBar = findViewById(R.id.storageBar);

        segmentAlbums = findViewById(R.id.segmentAlbums);
        segmentFiles = findViewById(R.id.segmentFiles);

        albumsContainer = findViewById(R.id.albumsContainer);
        filesContainer = findViewById(R.id.filesContainer);
    }

    /* ---------------- Init: Helpers ---------------- */

    private void initHelpers() {
        profileInfoHelper = new DashboardProfileInfoHelper(this, profileInfoView, primaryEmail);
        storageBarHelper = new DashboardStorageBarHelper(this, storageBar, primaryEmail);
        primaryAccountConsentHelper = new PrimaryAccountConsentHelper(this);
        expandVaultHelperOld = new ExpandVaultHelperOld(this, primaryEmail);
    }

    /* ---------------- Init: Vault Sections ---------------- */

    private void initVaultSections() {
        albumsUi = new AlbumsVaultUiHelper(this, albumsContainer);
        filesUi = new FilesVaultUiHelper(this, filesContainer);
    }

    /* ---------------- Init: Click Listeners ---------------- */

    private void initClickListeners() {
        segmentAlbums.setOnClickListener(v -> applyViewMode(VaultViewMode.ALBUMS));
        segmentFiles.setOnClickListener(v -> applyViewMode(VaultViewMode.FILES));

        findViewById(R.id.btnExpandVault).setOnClickListener(v -> onExpandVaultClicked());
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
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

    /* ---------------- Expand Vault ---------------- */

    private void onExpandVaultClicked() {
        expandVaultHelperOld.launch(new ExpandVaultHelperOld.Callback() {
            @Override public void onStart() { loading.show(); }

            @Override
            public void onTrustedAccountAdded() {
                loading.hide();
                storageBarHelper.loadAndBindStorage();
            }

            @Override
            public void onError(String message) {
                loading.hide();
                showToast(message);
            }

            @Override public void onEnd() { loading.hide(); }
        });
    }

    /* ---------------- Consent ---------------- */

    private void handleConsent(boolean fromLogin) {
        profileInfoHelper.bindProfile(profileName);

        if (fromLogin) {
            storageBarHelper.loadAndBindStorage();
            return;
        }

        primaryAccountConsentHelper.checkConsentsSilently(primaryEmail, result -> {
            switch (result) {
                case GRANTED:
                    profileInfoHelper.bindProfileLatest(updatedName -> profileName = updatedName);
                    storageBarHelper.loadAndBindStorage();
                    break;

                case RECOVERABLE:
                case FAILED:
                    forceLogout("Permissions were revoked. Please login again.");
                    break;
            }
        });
    }

    /* ---------------- Logout ---------------- */

    private void forceLogout(String reason) {
        showToast(reason);
        logout();
    }

    private void logout() {
        userSession.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /* ---------------- Utils ---------------- */

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }


    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                boolean consumed = false;

                if (currentViewMode == VaultViewMode.ALBUMS && albumsUi != null) {
                    consumed = albumsUi.onBackPressed();
                } else if (currentViewMode == VaultViewMode.FILES && filesUi != null) {
                    consumed = filesUi.onBackPressed();
                }

                if (!consumed) {
                    // No UI layer wants it → default behavior
                    finish();
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (albumsUi != null) albumsUi.release();
        if (filesUi != null) filesUi.release();
    }

}
