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
    private static final int MAX_PARALLEL_WORKERS = 2;
    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 250;

    private TrustedAccountsFetchWorker() {}

    public interface Callback {
        void onSuccess(List<TrustedAccount> accounts);
        void onError(Exception e);
    }

    public static void fetch(
            ExecutorService executor,
            Context appContext,
            Drive primaryDrive,
            String primaryEmail,
            Callback callback
    ) {
        executor.execute(() -> {
            long start = SystemClock.elapsedRealtime();
            try {
                String rootFolderId =
                        DriveFolderRepository.getRootFolderId(primaryDrive);
                if (rootFolderId == null) {
                    callback.onSuccess(List.of());
                    return;
                }

                // ---------- PERMISSIONS FETCH TIMING ----------
                long permStart = SystemClock.elapsedRealtime();

                PermissionList permissions =
                        primaryDrive.permissions()
                                .list(rootFolderId)
                                .setFields("permissions(emailAddress,role,type)")
                                .execute();

                long permEnd = SystemClock.elapsedRealtime();
                Log.d(TAG, "Permissions list API took "
                        + (permEnd - permStart) + " ms");

                List<String> emails = new ArrayList<>();
                for (Permission p : permissions.getPermissions()) {
                    if (!"user".equals(p.getType())) continue;
                    if (!"writer".equals(p.getRole())) continue;
                    String email = p.getEmailAddress();
                    if (email == null || email.equalsIgnoreCase(primaryEmail)) continue;
                    emails.add(email);
                }

                if (emails.isEmpty()) {
                    callback.onSuccess(List.of());
                    return;
                }

                int workers = Math.min(MAX_PARALLEL_WORKERS, emails.size());

                try (ExecutorService workerPool =
                             Executors.newFixedThreadPool(workers)) {

                    List<Future<TrustedAccount>> futures =
                            new ArrayList<>(emails.size());

                    for (String email : emails) {
                        futures.add(workerPool.submit(() -> {
                            int attempts = MAX_ATTEMPTS;
                            while (attempts-- > 0) {
                                try {
                                    Drive drive =
                                            DriveClientProvider.forAccount(appContext, email);
                                    return DriveStorageRepository
                                            .fetchStorageInfo(drive, email);
                                } catch (Exception e) {
                                    Log.w(TAG, "Failed once: " + email);
                                    SystemClock.sleep(RETRY_DELAY_MS);
                                }
                            }
                            Log.w(TAG, "Failed twice: " + email);
                            return null;
                        }));
                    }

                    List<TrustedAccount> result =
                            new ArrayList<>(emails.size());

                    for (Future<TrustedAccount> f : futures) {
                        try {
                            TrustedAccount account = f.get();
                            if (account != null) result.add(account);
                        } catch (Exception ignore) {}
                    }

                    Log.d(TAG, "accounts took "
                            + (SystemClock.elapsedRealtime() - permEnd)
                            + " ms, accounts=" + result.size());
                    Log.d(TAG, "fetchTrustedAccounts TOTAL took "
                            + (SystemClock.elapsedRealtime() - start)
                            + " ms, accounts=" + result.size());

                    callback.onSuccess(result);
                }

            } catch (Exception e) {
                Log.e(TAG, "fetchTrustedAccounts failed", e);
                callback.onError(e);
            }
        });
    }

}
