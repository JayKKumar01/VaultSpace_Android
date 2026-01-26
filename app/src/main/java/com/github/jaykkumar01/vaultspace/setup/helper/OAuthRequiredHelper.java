package com.github.jaykkumar01.vaultspace.setup.helper;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveStorageRepository;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OAuthRequiredHelper {

    private static final String TAG = "VaultSpace:OAuthRequired";

    private final AppCompatActivity activity;
    private final TrustedAccountsRepository repo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<Intent> launcher;

    private volatile String pendingEmail;
    private volatile Runnable onResolved;

    public OAuthRequiredHelper(AppCompatActivity activity) {
        this.activity = activity;
        this.repo = TrustedAccountsRepository.getInstance(activity);
        this.launcher = activity.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (pendingEmail == null) return;

                            if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                                Log.d(TAG, "OAuth granted, promoting account");
                                promoteAccount(pendingEmail);
                            } else {
                                Log.w(TAG, "OAuth denied by user");
                                clearPending();
                            }
                        }
                );
    }

    /* ================= PURE CHECK ================= */

    public boolean isRequired(String email) {
        return repo.getAccountSnapshot(email) == null;
    }

    /* ================= FIX ================= */

    public void resolve(String email, Runnable onResolved) {
        this.pendingEmail = email;
        this.onResolved = onResolved;

        executor.execute(() -> {
            try {
                ensureDriveAccess(email);
                promoteAccount(email);

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "OAuth required for " + email);
                mainHandler.post(() -> launcher.launch(e.getIntent()));

            } catch (Exception e) {
                Log.e(TAG, "OAuth resolve failed for " + email, e);
                clearPending();
            }
        });
    }

    /* ================= INTERNAL ================= */

    private void ensureDriveAccess(String email) throws Exception {
        Drive drive = DriveClientProvider.forAccount(activity, email);
        drive.about().get().setFields("user").execute();
    }

    private void promoteAccount(String email) {
        executor.execute(() -> {
            try {
                Drive drive = DriveClientProvider.forAccount(activity, email);
                TrustedAccount account =
                        DriveStorageRepository.fetchStorageInfo(drive, email);

                repo.addAccount(account);

                Log.d(TAG, "Account promoted to HEALTHY: " + email);
                mainHandler.post(onResolved);
            } catch (Exception e) {
                Log.e(TAG, "Failed to promote account " + email, e);
            } finally {
                clearPending();
            }
        });
    }

    private void clearPending() {
        pendingEmail = null;
        onResolved = null;
    }
}
