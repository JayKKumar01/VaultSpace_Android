package com.github.jaykkumar01.vaultspace.core.drive;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.TrustedAccountsCache;
import com.github.jaykkumar01.vaultspace.dashboard.helpers.TrustedAccountsDriveHelper;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class TrustedAccountsRepository {

    /* ================= Singleton ================= */

    private static volatile TrustedAccountsRepository INSTANCE;

    public static TrustedAccountsRepository getInstance(Context c) {
        if (INSTANCE == null) {
            synchronized (TrustedAccountsRepository.class) {
                if (INSTANCE == null)
                    INSTANCE = new TrustedAccountsRepository(c.getApplicationContext());
            }
        }
        return INSTANCE;
    }

    public static void destroy() {
        synchronized (TrustedAccountsRepository.class) {
            if (INSTANCE != null) {
                INSTANCE.releaseInternal();
                INSTANCE = null;
            }
        }
    }

    /* ================= Interfaces ================= */

    public interface Listener {
        void onAccountsChanged(Iterable<TrustedAccount> accounts);
    }

    /* ================= Core fields ================= */

    private final TrustedAccountsCache cache;
    private final TrustedAccountsDriveHelper driveHelper;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Object initLock = new Object();
    private final Set<Listener> listeners = new HashSet<>();

    /* ================= Constructor ================= */

    private TrustedAccountsRepository(Context c) {
        UserSession s = new UserSession(c);
        cache = s.getVaultCache().trustedAccounts;
        driveHelper = new TrustedAccountsDriveHelper(c);
        executor = Executors.newSingleThreadExecutor();
    }

    /* ================= Initialization ================= */

    private void initIfNeeded(Runnable afterInit) {
        driveHelper.fetchTrustedAccounts(executor, new TrustedAccountsDriveHelper.FetchCallback() {
            @Override public void onResult(List<TrustedAccount> accounts) {
                synchronized (initLock) {
                    if (!cache.isInitialized())
                        cache.initializeFromDrive(accounts);
                }
                afterInit.run();
                notifyListeners();
            }

            @Override public void onError(Exception e) {
                afterInit.run();
            }
        });
    }

    private void initBlockingIfNeeded() {
        if (cache.isInitialized())
            return;

        CountDownLatch latch = new CountDownLatch(1);
        initIfNeeded(latch::countDown);

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /* ================= Reads ================= */

    public void getAccounts(Consumer<Iterable<TrustedAccount>> cb) {
        if (cache.isInitialized()) {
            post(cb, cache.getAccountsView());
            return;
        }
        initIfNeeded(() -> post(cb, cache.getAccountsView()));
    }

    public Iterable<TrustedAccount> getAccountsSnapshot() {
        initBlockingIfNeeded();
        return cache.getAccountsView();
    }

    public TrustedAccount getAccountSnapshot(String email) {
        initBlockingIfNeeded();
        return cache.getAccount(email);
    }

    /* ================= Mutations ================= */

    public void addAccount(TrustedAccount a) {
        if (cache.isInitialized()) {
            cache.addAccount(a);
            notifyListeners();
            return;
        }
        initIfNeeded(() -> {
            cache.addAccount(a);
            notifyListeners();
        });
    }

    public void removeAccount(String email) {
        if (cache.isInitialized()) {
            cache.removeAccount(email);
            notifyListeners();
            return;
        }
        initIfNeeded(() -> {
            cache.removeAccount(email);
            notifyListeners();
        });
    }

    public void recordUploadUsage(String email, long bytes) {
        if (cache.isInitialized()) {
            cache.recordUploadUsage(email, bytes);
            notifyListeners();
            return;
        }
        initIfNeeded(() -> {
            cache.recordUploadUsage(email, bytes);
            notifyListeners();
        });
    }

    public void recordDeleteUsage(String email, long bytes) {
        if (cache.isInitialized()) {
            cache.recordDeleteUsage(email, bytes);
            notifyListeners();
            return;
        }
        initIfNeeded(() -> {
            cache.recordDeleteUsage(email, bytes);
            notifyListeners();
        });
    }


    /* ================= Refresh ================= */

    public void refresh() {
        synchronized (initLock) {
            cache.clear();
        }
        initIfNeeded(() -> {});
    }

    /* ================= Listeners ================= */

    public void addListener(Listener l) {
        if (l != null) listeners.add(l);
    }

    public void removeListener(Listener l) {
        if (l != null) listeners.remove(l);
    }

    private void notifyListeners() {
        Iterable<TrustedAccount> snap = cache.getAccountsView();
        mainHandler.post(() -> {
            for (Listener l : listeners)
                l.onAccountsChanged(snap);
        });
    }

    private <T> void post(Consumer<T> cb, T v) {
        mainHandler.post(() -> cb.accept(v));
    }

    /* ================= Lifecycle ================= */

    private void releaseInternal() {
        listeners.clear();
        executor.shutdown();
    }
}
