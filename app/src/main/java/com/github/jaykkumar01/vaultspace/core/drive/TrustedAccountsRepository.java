package com.github.jaykkumar01.vaultspace.core.drive;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.TrustedAccountsCache;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.TrustedAccountsDriveHelper;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class TrustedAccountsRepository {

    /* ==========================================================
     * Singleton
     * ========================================================== */

    private static volatile TrustedAccountsRepository INSTANCE;

    public static TrustedAccountsRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TrustedAccountsRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TrustedAccountsRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /* ==========================================================
     * Callbacks
     * ========================================================== */

    public interface ErrorCallback { void onError(Exception e); }

    public interface MutationCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface Listener {
        void onAccountsChanged(Iterable<TrustedAccount> accounts);
    }

    /* ==========================================================
     * Core
     * ========================================================== */

    private final TrustedAccountsCache cache;
    private final TrustedAccountsDriveHelper driveHelper;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Object lock = new Object();
    private boolean fetchInProgress = false;
    private final List<Runnable> pendingActions = new ArrayList<>();
    private final Set<Listener> listeners = new HashSet<>();

    /* ==========================================================
     * Constructor (PRIVATE)
     * ========================================================== */

    private TrustedAccountsRepository(Context appContext) {
        UserSession session = new UserSession(appContext);
        this.cache = session.getVaultCache().trustedAccounts;
        this.driveHelper = new TrustedAccountsDriveHelper(appContext);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /* ==========================================================
     * Guard (ALL cache access serialized)
     * ========================================================== */

    private void ensureInitialized(Runnable lockedAction, ErrorCallback errorCb) {
        boolean shouldFetch = false;

        synchronized (lock) {
            if (cache.isInitialized()) {
                lockedAction.run();
                return;
            }

            pendingActions.add(lockedAction);

            if (!fetchInProgress) {
                fetchInProgress = true;
                shouldFetch = true;
            }
        }

        if (!shouldFetch) return;

        driveHelper.fetchTrustedAccounts(
                executor,
                new TrustedAccountsDriveHelper.FetchCallback() {
                    @Override
                    public void onResult(List<TrustedAccount> accounts) {
                        synchronized (lock) {
                            cache.initializeFromDrive(accounts);
                            fetchInProgress = false;

                            for (Runnable r : pendingActions) {
                                r.run(); // UNDER LOCK
                            }
                            pendingActions.clear();
                        }
                        notifyListeners();
                    }

                    @Override
                    public void onError(Exception e) {
                        synchronized (lock) {
                            fetchInProgress = false;
                            pendingActions.clear();
                        }
                        if (errorCb != null) errorCb.onError(e);
                    }
                }
        );
    }

    /* ==========================================================
     * Read APIs
     * ========================================================== */

    public void getAccounts(Consumer<Iterable<TrustedAccount>> cb, ErrorCallback errorCb) {
        ensureInitialized(() -> cb.accept(cache.getAccountsView()), errorCb);
    }

    public List<TrustedAccount> getAccountsSnapshot() {
        synchronized (lock) {
            if (!cache.isInitialized()) return Collections.emptyList();

            List<TrustedAccount> out = new ArrayList<>();
            for (TrustedAccount a : cache.getAccountsView()) out.add(a);
            return out;
        }
    }

    public void getAccount(
            String email,
            Consumer<TrustedAccount> cb,
            ErrorCallback errorCb
    ) {
        ensureInitialized(() -> cb.accept(cache.getAccount(email)), errorCb);
    }

    /* ==========================================================
     * Mutation APIs
     * ========================================================== */

    public void addAccount(TrustedAccount account, MutationCallback cb) {
        ensureInitialized(() -> {
            cache.addAccount(account);
            notifyListeners();
            if (cb != null) cb.onSuccess();
        }, e -> {
            if (cb != null) cb.onError(e);
        });
    }

    public void removeAccount(String email, MutationCallback cb) {
        ensureInitialized(() -> {
            cache.removeAccount(email);
            notifyListeners();
            if (cb != null) cb.onSuccess();
        }, e -> {
            if (cb != null) cb.onError(e);
        });
    }

    public void recordUploadUsage(String email, long bytes, MutationCallback cb) {
        ensureInitialized(() -> {
            cache.recordUploadUsage(email, bytes);
            notifyListeners();
            if (cb != null) cb.onSuccess();
        }, e -> {
            if (cb != null) cb.onError(e);
        });
    }

    public void recordDeleteUsage(String email, long bytes, MutationCallback cb) {
        ensureInitialized(() -> {
            cache.recordDeleteUsage(email, bytes);
            notifyListeners();
            if (cb != null) cb.onSuccess();
        }, e -> {
            if (cb != null) cb.onError(e);
        });
    }

    /* ==========================================================
     * Refresh / Lifecycle
     * ========================================================== */

    public void refresh(ErrorCallback errorCb) {
        synchronized (lock) {
            cache.clear();
        }
        ensureInitialized(this::notifyListeners, errorCb);
    }

    public void addListener(Listener l) {
        if (l != null) listeners.add(l);
    }

    public void removeListener(Listener l) {
        if (l != null) listeners.remove(l);
    }

    private void notifyListeners() {
        Iterable<TrustedAccount> snapshot;
        synchronized (lock) {
            snapshot = cache.getAccountsView();
        }
        mainHandler.post(() -> {
            for (Listener l : listeners) l.onAccountsChanged(snapshot);
        });
    }

    private void releaseInternal() {
        synchronized (lock) {
            pendingActions.clear();
            listeners.clear();
        }
        executor.shutdown();
    }

    public static void destroy() {
        synchronized (TrustedAccountsRepository.class) {
            if (INSTANCE != null) {
                INSTANCE.releaseInternal();
                INSTANCE = null;
            }
        }
    }

}
