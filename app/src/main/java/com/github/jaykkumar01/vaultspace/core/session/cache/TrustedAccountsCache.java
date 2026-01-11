package com.github.jaykkumar01.vaultspace.core.session.cache;

import com.github.jaykkumar01.vaultspace.models.TrustedAccount;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
    }

    public void removeAccount(String email) {
        if (!isInitialized() || email == null) return;

        accountsByEmail.remove(email);
    }

    /* ==========================================================
     * VaultCache hook
     * ========================================================== */

    @Override
    protected void onClear() {
        accountsByEmail.clear();
    }
}
