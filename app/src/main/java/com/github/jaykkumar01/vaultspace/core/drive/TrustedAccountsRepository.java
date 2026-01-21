package com.github.jaykkumar01.vaultspace.core.drive;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.TrustedAccountsCache;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.TrustedAccountsDriveHelper;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TrustedAccountsRepository {

    private static final String TAG = "VaultSpace:TrustedRepo";

    public interface Callback {
        void onResult(@NonNull List<TrustedAccount> accounts);
        void onError(@NonNull Exception e);
    }

    private final TrustedAccountsCache cache;
    private final TrustedAccountsDriveHelper driveHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Object lock = new Object();
    private final List<Callback> waiters = new ArrayList<>();

    public TrustedAccountsRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        UserSession session = new UserSession(appContext);
        this.cache = session.getVaultCache().trustedAccounts;
        this.driveHelper = new TrustedAccountsDriveHelper(appContext);
    }

    /**
     * Cache-first, Drive-backed trusted account fetch.
     * Callback is invoked on repository executor thread.
     */
    public void getAccounts(@NonNull Callback callback) {
        if (cache.isInitialized()) {
            callback.onResult(copyFromCache());
            return;
        }

        boolean shouldStartFetch;

        synchronized (lock) {
            shouldStartFetch = waiters.isEmpty();
            waiters.add(callback);
        }

        if (!shouldStartFetch) return;

        executor.execute(() -> {
            driveHelper.fetchTrustedAccounts(
                    executor,
                    new TrustedAccountsDriveHelper.FetchCallback() {
                        @Override
                        public void onResult(List<TrustedAccount> accounts) {
                            cache.initializeFromDrive(accounts);
                            notifySuccess(copyFromCache());
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Drive fetch failed", e);
                            notifyError(e);
                        }
                    }
            );
        });
    }

    private List<TrustedAccount> copyFromCache() {
        List<TrustedAccount> out = new ArrayList<>();
        for (TrustedAccount account : cache.getAccountsView()) {
            out.add(account);
        }
        return out;
    }

    private void notifySuccess(List<TrustedAccount> accounts) {
        List<Callback> callbacks;

        synchronized (lock) {
            callbacks = new ArrayList<>(waiters);
            waiters.clear();
        }

        for (Callback cb : callbacks) {
            cb.onResult(accounts);
        }
    }

    private void notifyError(Exception e) {
        List<Callback> callbacks;

        synchronized (lock) {
            callbacks = new ArrayList<>(waiters);
            waiters.clear();
        }

        for (Callback cb : callbacks) {
            cb.onError(e);
        }
    }

    public void release() {
        executor.shutdown();
    }
}
