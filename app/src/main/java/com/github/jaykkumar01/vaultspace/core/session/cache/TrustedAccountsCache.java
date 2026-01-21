package com.github.jaykkumar01.vaultspace.core.session.cache;

import com.github.jaykkumar01.vaultspace.models.TrustedAccount;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * TrustedAccountsCache
 *
 * Session-scoped cache for trusted accounts.
 *
 * Guarantees:
 * - O(1) lookup by email
 * - O(1) add / remove
 * - O(n) ONLY during initialization
 * - Empty result is a VALID initialized state
 *
 * Responsibilities:
 * - Hold trusted account knowledge for the session
 *
 * Non-responsibilities:
 * - Drive access
 * - Permission checks
 * - UI logic
 */
public final class TrustedAccountsCache extends VaultCache {

    public interface Listener {
        void onAccountsChanged(Iterable<TrustedAccount> accounts);
    }

    private final Set<Listener> listeners = new HashSet<>();

    public void addListener(Listener l) {
        if (l != null) listeners.add(l);
    }

    public void removeListener(Listener l) {
        if (l != null) listeners.remove(l);
    }

    private void notifyListeners() {
        Iterable<TrustedAccount> snapshot = getAccountsView();
        for (Listener l : listeners) {
            l.onAccountsChanged(snapshot);
        }
    }



    /* ==========================================================
     * Storage
     * ========================================================== */

    /**
     * email -> TrustedAccount
     *
     * LinkedHashMap preserves insertion order for UI.
     */
    private final Map<String, TrustedAccount> accountsByEmail =
            new LinkedHashMap<>();

    /* ==========================================================
     * Initialization (O(n) — ONLY allowed place)
     * ========================================================== */

    /**
     * Initializes cache from Drive result.
     * Empty result is considered INITIALIZED.
     */
    public void initializeFromDrive(Iterable<TrustedAccount> accounts) {
        if (isInitialized()) return;

        if (accounts != null) {
            for (TrustedAccount account : accounts) {
                if (account != null && account.email != null) {
                    accountsByEmail.put(account.email, account);
                }
            }
        }

        markInitialized();
    }

    /* ==========================================================
     * Read APIs (O(1))
     * ========================================================== */

    public boolean hasAccount(String email) {
        return isInitialized()
                && email != null
                && accountsByEmail.containsKey(email);
    }

    public TrustedAccount getAccount(String email) {
        if (!isInitialized() || email == null) return null;
        return accountsByEmail.get(email);
    }

    /**
     * Read-only iterable view for UI.
     * UI should snapshot to List if needed.
     */
    public Iterable<TrustedAccount> getAccountsView() {
        return Collections.unmodifiableCollection(accountsByEmail.values());
    }

    /* ==========================================================
     * Mutation APIs (ALL O(1))
     * ========================================================== */

    public void addAccount(TrustedAccount account) {
        if (!isInitialized() || account == null || account.email == null) return;

        accountsByEmail.put(account.email, account);
        notifyListeners();
    }

    public void recordUploadUsage(String email, long uploadedBytes) {
        if (!isInitialized() || email == null || uploadedBytes <= 0) return;

        TrustedAccount old = accountsByEmail.get(email);
        if (old == null) return;

        long newUsed = old.usedQuota + uploadedBytes;
        long newFree = Math.max(0, old.totalQuota - newUsed);

        accountsByEmail.put(
                email,
                new TrustedAccount(
                        old.email,
                        old.totalQuota,
                        newUsed,
                        newFree
                )
        );
        notifyListeners();
    }


    public void removeAccount(String email) {
        if (!isInitialized() || email == null) return;

        accountsByEmail.remove(email);
        notifyListeners();
    }

    /* ==========================================================
     * VaultCache hook
     * ========================================================== */

    @Override
    protected void onClear() {
        accountsByEmail.clear();
    }
}
