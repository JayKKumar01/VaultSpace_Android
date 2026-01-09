package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.content.Context;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.github.jaykkumar01.vaultspace.views.creative.StorageBarView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fetches trusted account storage and binds it to StorageBarView
 * with proper decimal precision.
 */
public class DashboardStorageBarHelper {

    private static final String TAG = "VaultSpace:StorageBarHelper";
    private static final String UNIT_GB = "GB";
    private static final double BYTES_IN_GB = 1024d * 1024d * 1024d;

    private final Context appContext;
    private final StorageBarView storageBarView;
    private final String primaryEmail;

    // Single background executor (I/O bound work)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DashboardStorageBarHelper(
            Context context,
            StorageBarView storageBarView,
            String primaryEmail
    ) {
        this.appContext = context.getApplicationContext();
        this.storageBarView = storageBarView;
        this.primaryEmail = primaryEmail;
    }

    /**
     * Safe to call multiple times.
     */
    public void loadAndBindStorage() {

        Log.d(TAG, "Loading storage info");

        // Enter loading state immediately (UI thread)
        storageBarView.post(() -> storageBarView.setLoading(true));

        executor.execute(() -> {
            try {
                TrustedAccountsDriveHelper helper =
                        new TrustedAccountsDriveHelper(appContext, primaryEmail);

                List<TrustedAccount> accounts = helper.getTrustedAccounts();

                long totalBytes = 0L;
                long usedBytes = 0L;

                for (TrustedAccount account : accounts) {
                    totalBytes += account.totalQuota;
                    usedBytes += account.usedQuota;
                }

                float totalGb = (float) (totalBytes / BYTES_IN_GB);
                float usedGb = (float) (usedBytes / BYTES_IN_GB);

                Log.d(TAG,
                        "Storage summary: " +
                                usedGb + " / " + totalGb + " " + UNIT_GB
                );

                storageBarView.post(() ->
                        storageBarView.setUsage(
                                usedGb,
                                totalGb,
                                UNIT_GB
                        )
                );

            } catch (Exception e) {
                Log.e(TAG, "Failed to load storage info", e);

                storageBarView.post(() ->
                        storageBarView.setUsage(0f, 0f, UNIT_GB)
                );
            }
        });
    }

    /**
     * Call if you ever want to clean up explicitly (optional).
     */
    public void shutdown() {
        executor.shutdown();
    }
}
