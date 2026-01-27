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
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class OAuthRequiredHelper {

    private static final String TAG = "VaultSpace:OAuthRequired";

    private final AppCompatActivity activity;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<Intent> launcher;

    private volatile String pendingEmail;
    private volatile Consumer<TrustedAccount> onResolved;
    private volatile Runnable onError;

    public OAuthRequiredHelper(AppCompatActivity activity) {
        this.activity = activity;

        this.launcher = activity.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (pendingEmail == null) {
                                if (onError != null){
                                    onError.run();
                                }
                                return;
                            }

                            if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                                Log.d(TAG, "OAuth granted, promoting account");
                                createAccount(pendingEmail);
                            } else {
                                Log.w(TAG, "OAuth denied by user");
                                clearPending();
                                onError.run();
                            }
                        }
                );
    }

    /* ================= PURE CHECK ================= */



    /* ================= FIX ================= */

    public void resolve(String email, Consumer<TrustedAccount> onResolved, Runnable onError) {
        this.pendingEmail = email;
        this.onResolved = onResolved;
        this.onError = onError;

        executor.execute(() -> {
            try {
                ensureDriveAccess(email);

            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, "OAuth required for " + email);
                mainHandler.post(() -> launcher.launch(e.getIntent()));
            } catch (Exception e) {
                Log.e(TAG, "OAuth resolve failed for " + email, e);
                clearPending();
                onError.run();
            }
        });
    }

    /* ================= INTERNAL ================= */

    private void ensureDriveAccess(String email) throws Exception {
        Drive drive = DriveClientProvider.forAccount(activity, email);
        drive.about().get().setFields("user").execute();
    }

    private void createAccount(String email) {
        executor.execute(() -> {
            try {
                Drive drive = DriveClientProvider.forAccount(activity, email);
                TrustedAccount account = DriveStorageRepository.fetchStorageInfo(drive, email);
                onResolved.accept(account);
            } catch (Exception e) {
                Log.e(TAG, "Failed to promote account " + email, e);
                onError.run();
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
