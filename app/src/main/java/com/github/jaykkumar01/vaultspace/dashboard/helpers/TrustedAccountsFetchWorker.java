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

    private TrustedAccountsFetchWorker() {
    }

    /* ================= Listener ================= */

    public interface Callback {
        void onSuccess(List<TrustedAccount> accounts, List<String> linkedEmails);

        void onError(Exception e);
    }

    /* ================= Entry ================= */

    public static void fetch(ExecutorService executor, Context appContext, Drive primaryDrive, String primaryEmail, Callback callback) {

        executor.execute(() -> {
            long startMs = SystemClock.elapsedRealtime();
            try {
                String rootId = DriveFolderRepository.getRootFolderId(appContext);
                if (rootId == null) {
                    callback.onSuccess(List.of(), List.of());
                    return;
                }

                List<String> emails = fetchWriterEmails(primaryDrive, rootId, primaryEmail);
                if (emails.isEmpty()) {
                    callback.onSuccess(List.of(), emails);
                    return;
                }

                int cpu = Runtime.getRuntime().availableProcessors();
                int workers = Math.max(1, Math.min(emails.size(), cpu - 1));

                Log.d(TAG, "fetch start emails=" + emails.size() +
                        " cpu=" + cpu + " workers=" + workers);

                List<TrustedAccount> accounts = fetchAccountsParallel(appContext, emails, workers);

                Log.d(TAG, "fetch done accounts=" + accounts.size() +
                        " took=" + (SystemClock.elapsedRealtime() - startMs) + "ms");

                callback.onSuccess(accounts, emails);

            } catch (Exception e) {
                Log.e(TAG, "fetchTrustedAccounts failed", e);
                callback.onError(e);
            }
        });
    }

    /* ================= Permissions ================= */

    private static List<String> fetchWriterEmails(Drive drive, String folderId, String primaryEmail) throws Exception {

        PermissionList list = drive.permissions()
                .list(folderId)
                .setFields("permissions(emailAddress,role,type)")
                .execute();

        List<String> emails = new ArrayList<>();
        for (Permission p : list.getPermissions()) {
            if (!"user".equals(p.getType())) continue;
            if (!"writer".equals(p.getRole())) continue;

            String email = p.getEmailAddress();
            if (email == null || email.equalsIgnoreCase(primaryEmail)) continue;
            emails.add(email);
        }
        return emails;
    }

    /* ================= Parallel ================= */

    private static List<TrustedAccount> fetchAccountsParallel(Context ctx, List<String> emails, int workers) {

        List<Future<TrustedAccount>> futures = new ArrayList<>(emails.size());
        List<TrustedAccount> result = new ArrayList<>(emails.size());

        try (ExecutorService pool = Executors.newFixedThreadPool(workers)) {
            for (String e : emails) {
                if (e == null || e.isBlank()) continue;
                final String email = e.trim();
                futures.add(pool.submit(() -> fetch(ctx, email)));
            }

            for (Future<TrustedAccount> f : futures) {
                try {
                    TrustedAccount a = f.get();
                    if (a != null) result.add(a);
                } catch (Exception ignore) {
                }
            }
        }
        return result;
    }

    /* ================= Fetch ================= */

    private static TrustedAccount fetch(Context ctx, String email) {
        try {
            Drive drive = DriveClientProvider.forAccount(ctx, email);
            return DriveStorageRepository.fetchStorageInfo(drive, email);
        } catch (Exception e) {
            return null;
        }
    }
}
