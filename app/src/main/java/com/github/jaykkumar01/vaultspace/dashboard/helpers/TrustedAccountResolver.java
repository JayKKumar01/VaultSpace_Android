package com.github.jaykkumar01.vaultspace.dashboard.helpers;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveStorageRepository;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.google.api.services.drive.Drive;

public final class TrustedAccountResolver {

    private static final String TAG = "VaultSpace:TrustedAccountResolver";
    private static final long RETRY_DELAY_MS = 250;

    private final Context appContext;

    public TrustedAccountResolver(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public TrustedAccount resolve(String email) {
        long start = SystemClock.elapsedRealtime();

        for (int attempt = 1; attempt <= 2; attempt++) {
            long attemptStart = SystemClock.elapsedRealtime();
            try {
                Drive drive = DriveClientProvider.forAccount(appContext, email);
                TrustedAccount account =
                        DriveStorageRepository.fetchStorageInfo(drive, email);

                Log.d(TAG, "Resolved " + email + " attempt=" + attempt + " in "
                        + (SystemClock.elapsedRealtime() - attemptStart) + " ms");
                return account;

            } catch (Exception e) {
                long failTime = SystemClock.elapsedRealtime() - attemptStart;
                Log.w(TAG, "Attempt " + attempt + " failed for " + email + " after "
                        + failTime + " ms | "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());

                if (attempt == 1) SystemClock.sleep(RETRY_DELAY_MS);
            }
        }

        Log.w(TAG, "Final skip for " + email + " after "
                + (SystemClock.elapsedRealtime() - start) + " ms");
        return null;
    }
}
