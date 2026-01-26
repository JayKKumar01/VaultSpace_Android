package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.github.jaykkumar01.vaultspace.core.session.db.SessionStore;
import com.github.jaykkumar01.vaultspace.core.session.db.VaultSessionDatabase;
import com.github.jaykkumar01.vaultspace.core.session.db.setup.SetupIgnoreDao;
import com.github.jaykkumar01.vaultspace.core.session.db.setup.SetupIgnoreEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class SetupIgnoreStore implements SessionStore {

    private final SetupIgnoreDao dao;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile Set<String> ignoredCache;
    private volatile boolean loaded;

    public SetupIgnoreStore(Context c) {
        dao = VaultSessionDatabase.get(c).setupIgnoreDao();
    }

    /* ================= Lifecycle ================= */

    /**
     * Must be called once before reads.
     * Safe to call multiple times.
     */
    public void load(Runnable onReady) {
        if (loaded) {
            onReady.run();
            return;
        }

        executor.execute(() -> {
            List<String> emails = dao.getAllIgnoredEmails();
            Set<String> set = new HashSet<>(emails.size());
            set.addAll(emails);

            ignoredCache = set;
            loaded = true;

            mainHandler.post(onReady);
        });
    }

    /* ================= Reads (PURE) ================= */

    public boolean isIgnored(String email) {
        if (!loaded || ignoredCache == null) return false;
        return ignoredCache.contains(email);
    }

    public Set<String> getAllIgnoredSnapshot() {
        if (!loaded || ignoredCache == null) return new HashSet<>();
        return new HashSet<>(ignoredCache);
    }

    /* ================= Writes ================= */

    public void ignore(String email) {
        if (!loaded) return;

        if (ignoredCache.add(email)) {
            executor.execute(() ->
                    dao.insert(new SetupIgnoreEntity(
                            email,
                            System.currentTimeMillis()
                    ))
            );
        }
    }

    public void unignore(String email) {
        if (!loaded) return;

        if (ignoredCache.remove(email)) {
            executor.execute(() -> dao.delete(email));
        }
    }

    public void clear() {
        ignoredCache = null;
        loaded = false;
        executor.execute(dao::clear);
    }

    /* ================= Session ================= */

    @Override
    public void onSessionCleared() {
        clear();
    }
}
