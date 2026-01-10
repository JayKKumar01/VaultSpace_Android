package com.github.jaykkumar01.vaultspace.core.session.cache;

import android.util.Log;

import com.github.jaykkumar01.vaultspace.models.TrustedAccount;

import java.util.*;

public final class TrustedAccountsCache
        extends VaultCache<List<TrustedAccount>> {

    private static final String TAG = "VaultSpace:TrustedAccountsCache";

    private final List<TrustedAccount> accounts = new ArrayList<>();
    private final Map<String, TrustedAccount> byEmail = new HashMap<>();

    /* ================= VaultCache hooks ================= */

    @Override
    protected List<TrustedAccount> getInternal() {
        return accounts;
    }

    @Override
    protected List<TrustedAccount> getEmpty() {
        return Collections.emptyList();
    }

    @Override
    protected void setInternal(List<TrustedAccount> data) {
        accounts.clear();
        byEmail.clear();

        if (data != null) {
            for (TrustedAccount a : data) {
                accounts.add(a);
                byEmail.put(a.email, a);
            }
        }

        Log.d(TAG, "Trusted accounts cached: " + accounts.size());
    }

    @Override
    protected void clearInternal() {
        accounts.clear();
        byEmail.clear();
        Log.d(TAG, "Trusted accounts cache cleared");
    }

    /* ================= Domain API ================= */

    public boolean hasAccount(String email) {
        return isCached() && email != null && byEmail.containsKey(email);
    }

    public TrustedAccount getAccount(String email) {
        return email != null ? byEmail.get(email) : null;
    }

    public void addAccount(TrustedAccount account) {
        if (!isCached() || account == null || account.email == null) return;

        if (byEmail.containsKey(account.email)) return;

        accounts.add(account);
        byEmail.put(account.email, account);

        Log.d(TAG, "Trusted account added: " + account.email);
    }
}
