package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.drive.DriveStorageRepository;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class TrustedAccountsFetchWorker {

    private static final String TAG = "VaultSpace:TrustedAccountsDrive";
    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 250;

    private TrustedAccountsFetchWorker() {
    }

    /* ==========================================================
     * Callback
     * ========================================================== */

    public interface Callback {
        void onSuccess(List<TrustedAccount> accounts);
        void onError(Exception e);
    }

    /* ==========================================================
     * Entry point
     * ========================================================== */

    public static void fetch(ExecutorService executor,
                             Context appContext,
                             Drive primaryDrive,
                             String primaryEmail,
                             Callback callback) {

        executor.execute(() -> {
            long startMs = SystemClock.elapsedRealtime();

            try {
                String rootFolderId =
                        DriveFolderRepository.getRootFolderId(primaryDrive);

                if (rootFolderId == null) {
                    callback.onSuccess(List.of());
                    return;
                }

                List<String> emails =
                        fetchWriterEmails(primaryDrive, rootFolderId, primaryEmail);

                if (emails.isEmpty()) {
                    callback.onSuccess(List.of());
                    return;
                }

                int cpu = Runtime.getRuntime().availableProcessors();
                int workers = Math.max(1, Math.min(emails.size(), cpu - 1));

                Log.d(TAG, "fetch start emails=" + emails.size()
                        + " cpu=" + cpu
                        + " workers=" + workers);

                List<TrustedAccount> accounts =
                        fetchAccountsParallel(appContext, emails, workers);

                Log.d(TAG, "fetch done accounts=" + accounts.size()
                        + " took=" + (SystemClock.elapsedRealtime() - startMs) + "ms");

                callback.onSuccess(accounts);

            } catch (Exception e) {
                Log.e(TAG, "fetchTrustedAccounts failed", e);
                callback.onError(e);
            }
        });
    }

    /* ==========================================================
     * Permission helpers
     * ========================================================== */

    private static List<String> fetchWriterEmails(Drive drive,
                                                  String rootFolderId,
                                                  String primaryEmail) throws Exception {

        PermissionList permissions =
                drive.permissions()
                        .list(rootFolderId)
                        .setFields("permissions(emailAddress,role,type)")
                        .execute();

        List<String> emails = new ArrayList<>();

        for (Permission p : permissions.getPermissions()) {
            if (!"user".equals(p.getType())) continue;
            if (!"writer".equals(p.getRole())) continue;

            String email = p.getEmailAddress();
            if (email == null || email.equalsIgnoreCase(primaryEmail)) continue;

            emails.add(email);
        }

        return emails;
    }

    /* ==========================================================
     * Parallel fetch
     * ========================================================== */

    private static List<TrustedAccount> fetchAccountsParallel(Context appContext,
                                                              List<String> emails,
                                                              int workers) {

        List<Future<TrustedAccount>> futures = new ArrayList<>(emails.size());
        List<TrustedAccount> result = new ArrayList<>(emails.size());

        try (ExecutorService pool = Executors.newFixedThreadPool(workers)) {
            for (String email : emails) {
                futures.add(pool.submit(() -> fetchWithRetry(appContext, email)));
            }

            for (Future<TrustedAccount> f : futures) {
                try {
                    TrustedAccount account = f.get();
                    if (account != null) {
                        result.add(account);
                    }
                } catch (Exception ignore) {
                    // individual account failures are tolerated
                }
            }
        }

        return result;
    }

    /* ==========================================================
     * Retry helper
     * ========================================================== */

    private static TrustedAccount fetchWithRetry(Context appContext,
                                                 String email) {

        int attempts = 0;

        while (attempts++ < MAX_ATTEMPTS) {
            try {
                Drive drive =
                        DriveClientProvider.forAccount(appContext, email);

                return DriveStorageRepository
                        .fetchStorageInfo(drive, email);

            } catch (Exception e) {
                Log.w(TAG, "fetch failed email=" + email + " attempt=" + attempts, e);
                SystemClock.sleep(RETRY_DELAY_MS);
            }
        }

        return null;
    }
}
