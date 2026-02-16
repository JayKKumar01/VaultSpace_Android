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
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.dashboard.albums.AlbumsUi;
import com.github.jaykkumar01.vaultspace.dashboard.files.FilesUi;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardModalCoordinator;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.DashboardProfileHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.ExpandVaultHelper;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.ExpandVaultHelper.*;
import com.github.jaykkumar01.vaultspace.dashboard.interfaces.SectionUi;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.github.jaykkumar01.vaultspace.views.creative.StorageBarView;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressLint("SetTextI18n")
public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpace:Dashboard";
    private static final String EXTRA_FROM_LOGIN = "FROM_LOGIN";
    private boolean isFromLogin;

    private static final String UNIT_GB = "GB";
    private static final double BYTES_IN_GB = 1024d * 1024d * 1024d;

    /* Core */
    private UserSession userSession;

    /* Modals */
    private ModalHost modalHost;
    private DashboardModalCoordinator modalCoordinator;

    /* Helpers */
    private DashboardProfileHelper profileHelper;
    private ExpandVaultHelper expandVaultHelper;
    private TrustedAccountsRepository trustedAccountsRepo;
    private List<String> lastAccountEmails;


    /* UI */
    private StorageBarView storageBar;
    private AppCompatButton setUpAccounts;
    private TextView segmentAlbums, segmentFiles;
    private FrameLayout albumsContainer, filesContainer;
    private View btnExpandVault, btnLogout;
    private SectionUi albumsUi;
    private SectionUi filesUi;
    private ViewMode currentViewMode;
    private SetupState setupState = SetupState.UNKNOWN;

    public enum ViewMode {ALBUMS, FILES}

    private enum SetupState {UNKNOWN, REQUIRED, COMPLETE}


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
        activate();
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
        modalCoordinator = new DashboardModalCoordinator(modalHost);
        userSession = new UserSession(this);
        profileHelper = new DashboardProfileHelper(this);
        trustedAccountsRepo = TrustedAccountsRepository.getInstance(this);
        trustedAccountsRepo.addListener(this::onVaultStorageState);
        trustedAccountsRepo.addUsageListener(this::onUsageChanged);
        expandVaultHelper = new ExpandVaultHelper(this);
    }

    private void initViews() {
        storageBar = findViewById(R.id.storageBar);
        setUpAccounts = findViewById(R.id.setUpAccounts);
        segmentAlbums = findViewById(R.id.segmentAlbums);
        segmentFiles = findViewById(R.id.segmentFiles);
        albumsContainer = findViewById(R.id.albumsContainer);
        filesContainer = findViewById(R.id.filesContainer);
        btnExpandVault = findViewById(R.id.btnExpandVault);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void initVaultSections() {
        albumsUi = new AlbumsUi(this, albumsContainer, modalHost);
        filesUi = new FilesUi(this, filesContainer, modalHost);
    }

    private void initListeners() {
        btnLogout.setOnClickListener(v -> modalCoordinator.confirmLogout(this::exitToLogin));
    }

    private void initBackHandling() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {

                        if (modalHost.onBackPressed()) return;

                        modalCoordinator.confirmExit(DashboardActivity.this::finish);


                    }
                });
    }

    private void activate() {
        profileHelper.attach(isFromLogin);
        trustedAccountsRepo.refresh();

        setUpAccounts.setOnClickListener(v -> navigateToSetup());

        applyViewMode(ViewMode.ALBUMS);
        segmentAlbums.setOnClickListener(v -> applyViewMode(ViewMode.ALBUMS));
        segmentFiles.setOnClickListener(v -> applyViewMode(ViewMode.FILES));

        btnExpandVault.setOnClickListener(v -> {
            modalCoordinator.showLoading();
            expandVaultHelper.launch(expandAccountListener);
        });
    }

    private void navigateToSetup() {
        if (lastAccountEmails == null) return;

        startActivity(new Intent(this, SetupActivity.class).putStringArrayListExtra(
                SetupActivity.EXTRA_ACCOUNT_EMAILS,
                new ArrayList<>(lastAccountEmails)
        ));
    }

    private void onUsageChanged(long usedBytes, long totalBytes) {
        boolean hasStorage = totalBytes > 0L;

        if (setupState == SetupState.REQUIRED && !hasStorage) {
            storageBar.showGuidance();
            return;
        }

        float used = (float) (usedBytes / BYTES_IN_GB);
        float total = (float) (totalBytes / BYTES_IN_GB);

        storageBar.setUsage(used, total, UNIT_GB);

        if (setupState == SetupState.REQUIRED) {
            storageBar.showGuidanceWithUsage();
        }
    }

    private void onVaultStorageState(Iterable<TrustedAccount> accounts, Set<String> linkedEmails) {
        if (accounts == null || linkedEmails == null) return;

        // ---------- Setup-state computation ----------
        Set<String> trustedSet = new HashSet<>();

        for (TrustedAccount a : accounts) {
            if (a == null) continue;
            if (a.email != null) trustedSet.add(a.email);
        }

        boolean setupRequired = false;
        for (String email : linkedEmails) {
            if (!trustedSet.contains(email)) {
                setupRequired = true;
                break;
            }
        }

        setupState = setupRequired ? SetupState.REQUIRED : SetupState.COMPLETE;


        lastAccountEmails = new ArrayList<>(linkedEmails);

        // ---------- Structural UI ----------
        setUpAccounts.setVisibility(setupRequired ? View.VISIBLE : View.GONE);
    }

    /* ==========================================================
     * UI helpers
     * ========================================================== */

    private void applyViewMode(ViewMode mode) {
        if (currentViewMode == mode) return;

        currentViewMode = mode;
        boolean showAlbums = mode == ViewMode.ALBUMS;

        segmentAlbums.setSelected(showAlbums);
        segmentFiles.setSelected(!showAlbums);

        albumsContainer.setVisibility(showAlbums ? View.VISIBLE : View.GONE);
        filesContainer.setVisibility(showAlbums ? View.GONE : View.VISIBLE);

        if (showAlbums) albumsUi.show();
        else filesUi.show();
    }


    private final ExpandAccountListener expandAccountListener = new ExpandAccountListener() {
        @Override
        public void onSuccess(@NonNull TrustedAccount account, @NonNull ExpandResult result) {
            trustedAccountsRepo.addAccount(account);
            showToast(result.getMessage());
            modalCoordinator.clearLoading();
        }

        @Override
        public void onError(@NonNull ExpandError error) {
            showToast(error.getMessage());
            modalCoordinator.clearLoading();
        }
    };

    /* ==========================================================
     * Exit
     * ========================================================== */

    private void exitToLogin() {

        modalCoordinator.reset();
        Toast.makeText(this, "You have been logged out", Toast.LENGTH_LONG).show();

        userSession.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        trustedAccountsRepo.removeListener(this::onVaultStorageState);
        trustedAccountsRepo.removeUsageListener(this::onUsageChanged);
        albumsUi.onRelease();
        filesUi.onRelease();
        expandVaultHelper.release();
    }

}