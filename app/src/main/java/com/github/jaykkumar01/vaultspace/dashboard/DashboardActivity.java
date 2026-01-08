package com.github.jaykkumar01.vaultspace.dashboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

@SuppressLint("SetTextI18n")
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
    private TextView segmentFiles;

    private FrameLayout albumsContainer;
    private FrameLayout filesContainer;



    private VaultViewMode currentViewMode = null;

    private View albumsEmptyView;
    private View filesEmptyView;


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
        segmentFiles = findViewById(R.id.segmentFiles);

        albumsContainer = findViewById(R.id.albumsContainer);
        filesContainer = findViewById(R.id.filesContainer);

        segmentAlbums.setOnClickListener(v ->
                applyViewMode(VaultViewMode.ALBUMS));

        segmentFiles.setOnClickListener(v ->
                applyViewMode(VaultViewMode.FILES));


        albumsEmptyView = getLayoutInflater()
                .inflate(R.layout.view_empty_state, albumsContainer, false);

        filesEmptyView = getLayoutInflater()
                .inflate(R.layout.view_empty_state, filesContainer, false);

        albumsContainer.addView(albumsEmptyView);
        filesContainer.addView(filesEmptyView);

        bindAlbumsEmptyState();
        bindFilesEmptyState();

        applyViewMode(VaultViewMode.ALBUMS);
    }

    private void bindAlbumsEmptyState() {

        ImageView icon = albumsEmptyView.findViewById(R.id.ivEmptyIcon);
        TextView title = albumsEmptyView.findViewById(R.id.tvEmptyTitle);
        TextView subtitle = albumsEmptyView.findViewById(R.id.tvEmptySubtitle);
        Button primaryBtn = albumsEmptyView.findViewById(R.id.btnPrimaryAction);
        Button secondaryBtn = albumsEmptyView.findViewById(R.id.btnSecondaryAction);

        icon.setImageResource(R.drawable.ic_album_empty);
        title.setText("No albums yet");
        subtitle.setText("Albums help you organize memories your way.");

        // Primary CTA
        primaryBtn.setText("Create Album");
        primaryBtn.setOnClickListener(v -> {
            // TODO: launch create-album flow
            Toast.makeText(this, "Create Album clicked", Toast.LENGTH_SHORT).show();
        });
    }
    private void bindFilesEmptyState() {

        ImageView icon = filesEmptyView.findViewById(R.id.ivEmptyIcon);
        TextView title = filesEmptyView.findViewById(R.id.tvEmptyTitle);
        TextView subtitle = filesEmptyView.findViewById(R.id.tvEmptySubtitle);
        Button primaryBtn = filesEmptyView.findViewById(R.id.btnPrimaryAction);
        Button secondaryBtn = filesEmptyView.findViewById(R.id.btnSecondaryAction);

        icon.setImageResource(R.drawable.ic_files_empty);
        title.setText("No files found");
        subtitle.setText("Files reflect how your data is stored in Drive.");

        // Primary CTA → Upload
        primaryBtn.setText("Upload Files");
        primaryBtn.setOnClickListener(v -> {
            // TODO: launch file picker
            Toast.makeText(this, "Upload Files clicked", Toast.LENGTH_SHORT).show();
        });

        // Secondary CTA → Create Folder
        secondaryBtn.setVisibility(View.VISIBLE);
        secondaryBtn.setText("Create Folder");
        secondaryBtn.setOnClickListener(v -> {
            // TODO: launch create-folder flow
            Toast.makeText(this, "Create Folder clicked", Toast.LENGTH_SHORT).show();
        });
    }



    private void applyViewMode(VaultViewMode mode) {
        if (currentViewMode == mode) return;

        currentViewMode = mode;

        boolean isAlbums = (mode == VaultViewMode.ALBUMS);

        segmentAlbums.setSelected(isAlbums);
        segmentFiles.setSelected(!isAlbums);

        albumsContainer.setVisibility(isAlbums ? View.VISIBLE : View.GONE);
        filesContainer.setVisibility(isAlbums ? View.GONE : View.VISIBLE);
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
