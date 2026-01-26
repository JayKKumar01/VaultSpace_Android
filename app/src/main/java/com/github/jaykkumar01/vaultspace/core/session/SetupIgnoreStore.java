package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;


import com.github.jaykkumar01.vaultspace.core.session.db.SessionStore;
import com.github.jaykkumar01.vaultspace.core.session.db.VaultSessionDatabase;
import com.github.jaykkumar01.vaultspace.core.session.db.setup.SetupIgnoreDao;
import com.github.jaykkumar01.vaultspace.core.session.db.setup.SetupIgnoreEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SetupIgnoreStore implements SessionStore{

    private final SetupIgnoreDao dao;
    private Set<String> ignoredCache;

    public SetupIgnoreStore(Context c) {
        dao = VaultSessionDatabase.get(c).setupIgnoreDao();
    }

    /* ================= Reads ================= */

    public synchronized boolean isIgnored(String email) {
        ensureLoaded();
        return ignoredCache.contains(email);
    }

    public synchronized Set<String> getAllIgnored() {
        ensureLoaded();
        return new HashSet<>(ignoredCache);
    }

    /* ================= Writes ================= */

    public synchronized void ignore(String email) {
        ensureLoaded();
        if (ignoredCache.add(email)) {
            dao.insert(new SetupIgnoreEntity(
                    email,
                    System.currentTimeMillis()
            ));
        }
    }

    public synchronized void unignore(String email) {
        ensureLoaded();
        if (ignoredCache.remove(email)) {
            dao.delete(email);
        }
    }

    public synchronized void clear() {
        ignoredCache = null;
        dao.clear();
    }

    /* ================= Internals ================= */

    private void ensureLoaded() {
        if (ignoredCache != null) return;

        List<String> emails = dao.getAllIgnoredEmails();
        ignoredCache = new HashSet<>(emails.size());
        ignoredCache.addAll(emails);
    }

    @Override
    public void onSessionCleared() {
        clear();
    }
}
