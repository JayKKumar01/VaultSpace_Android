package com.github.jaykkumar01.vaultspace.dashboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
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
import com.github.jaykkumar01.vaultspace.views.LoadingStateView;
import com.github.jaykkumar01.vaultspace.views.ProfileInfoView;
import com.github.jaykkumar01.vaultspace.views.StorageBarView;

@SuppressLint("SetTextI18n")
public class DashboardActivity extends AppCompatActivity {

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

    /* ---------------- Container Views ---------------- */
    private LoadingStateView albumsLoadingView;
    private LoadingStateView filesLoadingView;
    private View albumsEmptyView;
    private View albumsContentView;
    private View filesEmptyView;
    private View filesContentView;

    /* ---------------- Helpers ---------------- */

    private DashboardProfileInfoHelper profileInfoHelper;
    private DashboardStorageBarHelper storageBarHelper;
    private PrimaryAccountConsentHelper primaryAccountConsentHelper;
    private ExpandVaultHelper expandVaultHelper;

    /* ---------------- State ---------------- */

    private VaultViewMode currentViewMode = null;

    enum ContainerState {
        LOADING,
        EMPTY,
        CONTENT
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
        initClickListeners();
        initSegments();

        boolean fromLogin = getIntent().getBooleanExtra(EXTRA_FROM_LOGIN, false);
        handleConsent(fromLogin);
    }

    /* ---------------- Init: Session ---------------- */

    private void initSession() {
        userSession = new UserSession(this);

        primaryEmail = userSession.getPrimaryAccountEmail();
        profileName = userSession.getProfileName();

        Log.d(TAG, "Primary email = " + primaryEmail);
        Log.d(TAG, "Profile name = " + profileName);

        if (primaryEmail == null) {
            forceLogout("Session expired. Please login again.");
        }
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
        expandVaultHelper = new ExpandVaultHelper(this, primaryEmail);
    }

    /* ---------------- Init: Click Listeners ---------------- */

    private void initClickListeners() {
        findViewById(R.id.btnExpandVault)
                .setOnClickListener(v -> onExpandVaultClicked());

        findViewById(R.id.btnLogout)
                .setOnClickListener(v -> logout());

        segmentAlbums.setOnClickListener(v ->
                applyViewMode(VaultViewMode.ALBUMS));

        segmentFiles.setOnClickListener(v ->
                applyViewMode(VaultViewMode.FILES));
    }

    /* ---------------- Init: Segments & Containers ---------------- */

    private void initSegments() {
        initContainerViews();
        initEmptyStates();
        initLoadingTexts();

        setAlbumsState(ContainerState.LOADING);
        setFilesState(ContainerState.LOADING);

        applyViewMode(VaultViewMode.ALBUMS);
    }

    private void initContainerViews() {

        // Albums
        albumsLoadingView = new LoadingStateView(this);
        albumsContainer.addView(albumsLoadingView);

        albumsEmptyView = inflate(R.layout.view_empty_state, albumsContainer);
        albumsContentView = inflate(R.layout.view_mock_content, albumsContainer);

        // Files
        filesLoadingView = new LoadingStateView(this);
        filesContainer.addView(filesLoadingView);

        filesEmptyView = inflate(R.layout.view_empty_state, filesContainer);
        filesContentView = inflate(R.layout.view_mock_content, filesContainer);
    }



    private void initEmptyStates() {
        bindAlbumsEmptyState();
        bindFilesEmptyState();
    }

    private void initLoadingTexts() {
        setLoadingText(albumsLoadingView, "Loading albums…");
        setLoadingText(filesLoadingView, "Loading files…");
    }

    private void setLoadingText(LoadingStateView view, String text) {
        if (view != null) {
            view.setText(text);
        }
    }



    private View inflate(int layout, FrameLayout parent) {
        View view = getLayoutInflater().inflate(layout, parent, false);
        parent.addView(view);
        return view;
    }

    /* ---------------- View Mode ---------------- */

    private void applyViewMode(VaultViewMode mode) {
        if (currentViewMode == mode) return;

        currentViewMode = mode;
        boolean isAlbums = mode == VaultViewMode.ALBUMS;

        segmentAlbums.setSelected(isAlbums);
        segmentFiles.setSelected(!isAlbums);

        albumsContainer.setVisibility(isAlbums ? View.VISIBLE : View.GONE);
        filesContainer.setVisibility(isAlbums ? View.GONE : View.VISIBLE);

        if (isAlbums) {
            loadAlbumsMock();
        } else {
            loadFilesMock();
        }
    }

    /* ---------------- Container State ---------------- */

    private void setAlbumsState(ContainerState state) {
        albumsLoadingView.setVisibility(state == ContainerState.LOADING ? View.VISIBLE : View.GONE);
        albumsEmptyView.setVisibility(state == ContainerState.EMPTY ? View.VISIBLE : View.GONE);
        albumsContentView.setVisibility(state == ContainerState.CONTENT ? View.VISIBLE : View.GONE);
    }

    private void setFilesState(ContainerState state) {
        filesLoadingView.setVisibility(state == ContainerState.LOADING ? View.VISIBLE : View.GONE);
        filesEmptyView.setVisibility(state == ContainerState.EMPTY ? View.VISIBLE : View.GONE);
        filesContentView.setVisibility(state == ContainerState.CONTENT ? View.VISIBLE : View.GONE);
    }

    /* ---------------- Mock Loaders ---------------- */

    private void loadAlbumsMock() {
        setAlbumsState(ContainerState.LOADING);

        albumsContainer.postDelayed(() -> {
            boolean hasAlbums = false;
            setAlbumsState(hasAlbums ? ContainerState.CONTENT : ContainerState.EMPTY);
        }, 12000);
    }

    private void loadFilesMock() {
        setFilesState(ContainerState.LOADING);

        filesContainer.postDelayed(() -> {
            boolean hasFiles = false;
            setFilesState(hasFiles ? ContainerState.CONTENT : ContainerState.EMPTY);
        }, 1200);
    }

    /* ---------------- Empty States ---------------- */

    private void bindAlbumsEmptyState() {
        ImageView icon = albumsEmptyView.findViewById(R.id.ivEmptyIcon);
        TextView title = albumsEmptyView.findViewById(R.id.tvEmptyTitle);
        TextView subtitle = albumsEmptyView.findViewById(R.id.tvEmptySubtitle);
        Button primaryBtn = albumsEmptyView.findViewById(R.id.btnPrimaryAction);

        icon.setImageResource(R.drawable.ic_album_empty);
        title.setText("No albums yet");
        subtitle.setText("Albums help you organize memories your way.");

        primaryBtn.setText("Create Album");
        primaryBtn.setOnClickListener(v ->
                Toast.makeText(this, "Create Album clicked", Toast.LENGTH_SHORT).show());
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

        primaryBtn.setText("Upload Files");
        primaryBtn.setOnClickListener(v ->
                Toast.makeText(this, "Upload Files clicked", Toast.LENGTH_SHORT).show());

        secondaryBtn.setVisibility(View.VISIBLE);
        secondaryBtn.setText("Create Folder");
        secondaryBtn.setOnClickListener(v ->
                Toast.makeText(this, "Create Folder clicked", Toast.LENGTH_SHORT).show());
    }

    /* ---------------- Expand Vault ---------------- */

    private void onExpandVaultClicked() {
        expandVaultHelper.launch(new ExpandVaultHelper.Callback() {
            @Override public void onStart() { loading.show(); }
            @Override public void onTrustedAccountAdded() {
                loading.hide();
                storageBarHelper.loadAndBindStorage();
            }
            @Override public void onError(String message) {
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
                    profileInfoHelper.bindProfileLatest(updatedName -> {
                        profileName = updatedName;
                    });
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
}
