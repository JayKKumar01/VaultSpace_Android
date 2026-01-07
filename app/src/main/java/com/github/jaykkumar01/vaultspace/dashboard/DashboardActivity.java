package com.github.jaykkumar01.vaultspace.dashboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.consent.PrimaryAccountConsentHelper;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.login.LoginActivity;
import com.github.jaykkumar01.vaultspace.views.ActivityLoadingOverlay;
import com.github.jaykkumar01.vaultspace.views.ProfileInfoView;
import com.github.jaykkumar01.vaultspace.views.StorageBarView;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Dashboard";
    private static final String EXTRA_FROM_LOGIN = "FROM_LOGIN";

    private UserSession userSession;

    private String primaryEmail;
    private String profileName;

    private DashboardStorageBarHelper storageBarHelper;
    private PrimaryAccountConsentHelper primaryAccountConsentHelper;
    private ExpandVaultHelper expandVaultHelper;
    private ActivityLoadingOverlay loading;

    /* ---------------- View Mode (Albums / Folders) ---------------- */

    private TextView segmentAlbums;
    private TextView segmentFolders;

    private FrameLayout albumsContainer;
    private FrameLayout foldersContainer;

    private VaultViewMode currentViewMode = null;

    private View albumsEmptyView;
    private View foldersEmptyView;


    /* ---------------- Lifecycle ---------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        Log.d(TAG, "onCreate()");

        loading = new ActivityLoadingOverlay(this);

        initSession();
        initUI();
        initHelpers();

        boolean fromLogin = getIntent().getBooleanExtra(EXTRA_FROM_LOGIN, false);
        handleConsent(fromLogin);
    }

    /* ---------------- Init ---------------- */

    private void initSession() {
        userSession = new UserSession(this);

        primaryEmail = userSession.getPrimaryAccountEmail();
        profileName = userSession.getProfileName();

        Log.d(TAG, "Primary email = " + primaryEmail);
        Log.d(TAG, "Profile name = " + profileName);

        if (primaryEmail == null) {
            Log.e(TAG, "Primary email missing");
            forceLogout("Session expired. Please login again.");
        }
    }

    private void initUI() {
        ProfileInfoView profileInfoView = findViewById(R.id.profileInfo);
        new DashboardProfileInfoHelper(this, profileInfoView)
                .bindProfile(primaryEmail, profileName);

        StorageBarView storageBar = findViewById(R.id.storageBar);
        storageBarHelper = new DashboardStorageBarHelper(this, storageBar, primaryEmail);

        findViewById(R.id.btnExpandVault)
                .setOnClickListener(v -> onExpandVaultClicked());

        findViewById(R.id.btnLogout)
                .setOnClickListener(v -> logout());

        initSegmentUI();
    }

    private void initHelpers() {
        primaryAccountConsentHelper = new PrimaryAccountConsentHelper(this);
        expandVaultHelper = new ExpandVaultHelper(this, primaryEmail);
    }

    /* ---------------- Segment UI ---------------- */

    private void initSegmentUI() {
        segmentAlbums = findViewById(R.id.segmentAlbums);
        segmentFolders = findViewById(R.id.segmentFolders);

        albumsContainer = findViewById(R.id.albumsContainer);
        foldersContainer = findViewById(R.id.foldersContainer);

        albumsEmptyView = getLayoutInflater()
                .inflate(R.layout.view_empty_state, albumsContainer, false);

        foldersEmptyView = getLayoutInflater()
                .inflate(R.layout.view_empty_state, foldersContainer, false);

        albumsContainer.addView(albumsEmptyView);
        foldersContainer.addView(foldersEmptyView);

        bindAlbumsEmptyState();
        bindFoldersEmptyState();

        segmentAlbums.setOnClickListener(v ->
                applyViewMode(VaultViewMode.ALBUMS));

        segmentFolders.setOnClickListener(v ->
                applyViewMode(VaultViewMode.FOLDERS));

        applyViewMode(VaultViewMode.ALBUMS);
    }

    @SuppressLint("SetTextI18n")
    private void bindAlbumsEmptyState() {
        ((ImageView) albumsEmptyView.findViewById(R.id.ivEmptyIcon))
                .setImageResource(R.drawable.ic_album_empty);

        ((TextView) albumsEmptyView.findViewById(R.id.tvEmptyTitle))
                .setText("No albums yet");

        ((TextView) albumsEmptyView.findViewById(R.id.tvEmptySubtitle))
                .setText("Albums help you organize memories your way.");
    }

    @SuppressLint("SetTextI18n")
    private void bindFoldersEmptyState() {
        ((ImageView) foldersEmptyView.findViewById(R.id.ivEmptyIcon))
                .setImageResource(R.drawable.ic_folder_empty);

        ((TextView) foldersEmptyView.findViewById(R.id.tvEmptyTitle))
                .setText("No folders found");

        ((TextView) foldersEmptyView.findViewById(R.id.tvEmptySubtitle))
                .setText("Folders reflect how your files are stored in Drive.");
    }


    private void applyViewMode(VaultViewMode mode) {
        if (currentViewMode == mode) return;

        currentViewMode = mode;

        boolean isAlbums = (mode == VaultViewMode.ALBUMS);

        segmentAlbums.setSelected(isAlbums);
        segmentFolders.setSelected(!isAlbums);

        albumsContainer.setVisibility(isAlbums ? View.VISIBLE : View.GONE);
        foldersContainer.setVisibility(isAlbums ? View.GONE : View.VISIBLE);
    }

    /* ---------------- Expand Vault ---------------- */

    private void onExpandVaultClicked() {
        expandVaultHelper.launch(new ExpandVaultHelper.Callback() {
            @Override public void onStart() {
                loading.show();
            }

            @Override public void onTrustedAccountAdded() {
                loading.hide();
                storageBarHelper.loadAndBindStorage();
            }

            @Override public void onError(String message) {
                loading.hide();
                showToast(message);
            }

            @Override public void onEnd() {
                loading.hide();
            }
        });
    }

    /* ---------------- Consent Flow ---------------- */

    private void handleConsent(boolean fromLogin) {
        if (fromLogin) {
            Log.d(TAG, "Opened from Login — skipping consent check");
            storageBarHelper.loadAndBindStorage();
            return;
        }

        primaryAccountConsentHelper.checkConsentsSilently(primaryEmail, result -> {
            switch (result) {
                case GRANTED:
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
        Log.d(TAG, "Clearing session");
        userSession.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /* ---------------- Utils ---------------- */

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
