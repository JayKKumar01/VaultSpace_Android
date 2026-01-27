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
import java.util.concurrent.atomic.AtomicLong;
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
        void onAccountsChanged(Iterable<TrustedAccount> accounts, Set<String> linkedEmails);
    }

    public interface UsageListener {
        void onUsageChanged(long usedBytes, long totalBytes);
    }

    /* ================= Core fields ================= */

    private final TrustedAccountsCache cache;
    private final TrustedAccountsDriveHelper driveHelper;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Object initLock = new Object();
    private final Set<Listener> listeners = new HashSet<>();
    private final Set<UsageListener> usageListeners = new HashSet<>();
    private volatile Set<String> linkedEmails;

    private final AtomicLong totalStorageBytes = new AtomicLong();
    private final AtomicLong usedStorageBytes = new AtomicLong();


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
            @Override
            public void onResult(List<TrustedAccount> accounts, List<String> linkedEmailsFromDrive) {
                synchronized (initLock) {
                    if (!cache.isInitialized())
                        cache.initializeFromDrive(accounts);

                    linkedEmails = new HashSet<>(linkedEmailsFromDrive); // always overwrite
                    recomputeTotalsLocked();
                }
                afterInit.run();
                notifyListeners();
                notifyUsageListeners();
            }


            @Override
            public void onError(Exception e) {
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


    public record AccountsAndLinks(Iterable<TrustedAccount> accounts, Set<String> linkedEmails) {
    }

    public void getAccountsAndLinkedEmails(Consumer<AccountsAndLinks> cb) {
        if (cache.isInitialized() && linkedEmails != null) {
            post(cb, new AccountsAndLinks(cache.getAccountsView(), new HashSet<>(linkedEmails)));
            return;
        }

        initIfNeeded(() -> post(cb, new AccountsAndLinks(
                cache.getAccountsView(), linkedEmails != null ?
                new HashSet<>(linkedEmails) : Set.of())
        ));
    }

    public Set<String> getLinkedEmailsSnapshot() {
        initBlockingIfNeeded();
        synchronized (initLock) {
            return linkedEmails != null ? new HashSet<>(linkedEmails) : Set.of();
        }
    }



    /* ================= Mutations ================= */

    private void addAccountInternal(TrustedAccount a) {
        cache.addAccount(a);
        if (linkedEmails == null) linkedEmails = new HashSet<>();
        linkedEmails.add(a.email);
        totalStorageBytes.addAndGet(a.totalQuota);
        usedStorageBytes.addAndGet(a.usedQuota);
        notifyListeners();
        notifyUsageListeners();
    }

    private void removeAccountInternal(String email) {
        TrustedAccount a = cache.getAccount(email);
        cache.removeAccount(email);
        if (a != null) {
            totalStorageBytes.addAndGet(-a.totalQuota);
            usedStorageBytes.addAndGet(-a.usedQuota);
        }
        if (linkedEmails != null) linkedEmails.remove(email);
        notifyListeners();
        notifyUsageListeners();
    }

    public void addAccount(TrustedAccount a) {
        if (cache.isInitialized()) {
            addAccountInternal(a);
            return;
        }
        initIfNeeded(() -> addAccountInternal(a));
    }

    public void removeAccount(String email) {
        if (cache.isInitialized()) {
            removeAccountInternal(email);
            return;
        }
        initIfNeeded(() -> removeAccountInternal(email));
    }


    private void recordUploadUsageInternal(String email, long bytes) {
        cache.recordUploadUsage(email, bytes);
        usedStorageBytes.addAndGet(bytes);
        if (cache.isInitialized()) notifyUsageListeners();
    }

    private void recordDeleteUsageInternal(String email, long bytes) {
        cache.recordDeleteUsage(email, bytes);
        usedStorageBytes.addAndGet(-bytes);
        if (cache.isInitialized()) notifyUsageListeners();
    }

    public void recordUploadUsage(String email, long bytes) {
        if (cache.isInitialized()) {
            recordUploadUsageInternal(email, bytes);
            return;
        }
        initIfNeeded(() -> recordUploadUsageInternal(email, bytes));
    }

    public void recordDeleteUsage(String email, long bytes) {
        if (cache.isInitialized()) {
            recordDeleteUsageInternal(email, bytes);
            return;
        }
        initIfNeeded(() -> recordDeleteUsageInternal(email, bytes));
    }



    /* ================= Refresh ================= */

    public void refresh() {
        synchronized (initLock) {
            cache.clear();
            linkedEmails = null;
            totalStorageBytes.set(0L);
            usedStorageBytes.set(0L);
        }
        initIfNeeded(() -> {
        });
    }

    /* ================= Listeners ================= */

    public void addListener(Listener l) {
        if (l != null) listeners.add(l);
    }

    public void removeListener(Listener l) {
        if (l != null) listeners.remove(l);
    }

    public void addUsageListener(UsageListener l) { if (l != null) usageListeners.add(l); }
    public void removeUsageListener(UsageListener l) { if (l != null) usageListeners.remove(l); }

    private void notifyListeners() {
        Iterable<TrustedAccount> snap = cache.getAccountsView();
        Set<Listener> snapshot = new HashSet<>(listeners);
        mainHandler.post(() -> {
            for (Listener l : snapshot)
                l.onAccountsChanged(snap, linkedEmails);
        });
    }


    private void notifyUsageListeners() {
        long used = usedStorageBytes.get();
        long total = totalStorageBytes.get();
        Set<UsageListener> snapshot = new HashSet<>(usageListeners);
        mainHandler.post(() -> {
            for (UsageListener l : snapshot)
                l.onUsageChanged(used, total);
        });
    }


    private void recomputeTotalsLocked() {
        long total = 0L, used = 0L;
        for (TrustedAccount a : cache.getAccountsView()) {
            if (a == null) continue;
            total += a.totalQuota;
            used += a.usedQuota;
        }
        totalStorageBytes.set(total);
        usedStorageBytes.set(used);
    }

    private <T> void post(Consumer<T> cb, T v) {
        mainHandler.post(() -> cb.accept(v));
    }

    /* ================= Lifecycle ================= */

    private void releaseInternal() {
        listeners.clear();
        usageListeners.clear();
        executor.shutdown();
    }
}
