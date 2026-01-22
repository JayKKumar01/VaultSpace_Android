package com.github.jaykkumar01.vaultspace.core.session.cache;

import com.github.jaykkumar01.vaultspace.models.TrustedAccount;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TrustedAccountsCache
 *
 * Session-scoped, in-memory cache for trusted accounts.
 *
 * Guarantees:
 * - O(1) lookup by email
 * - O(1) add / remove / update
 * - O(n) ONLY during (re)initialization
 * - Empty result is a VALID initialized state
 *
 * Supports:
 * - One-time initialization
 * - Explicit refresh via clear() + initializeFromDrive()
 *
 * Responsibilities:
 * - Hold trusted account data
 *
 * Non-responsibilities:
 * - Drive access
 * - Listeners
 * - Threading
 * - UI concerns
 */
public final class TrustedAccountsCache extends VaultCache {

    /**
     * email -> TrustedAccount
     * LinkedHashMap preserves insertion order for UI.
     */
    private final Map<String, TrustedAccount> accountsByEmail =
            new LinkedHashMap<>();

    /* ==========================================================
     * Initialization / Refresh (O(n))
     * ========================================================== */

    /**
     * Initializes cache from Drive data.
     * Must be called ONLY when cache is not initialized.
     * Empty iterable is a VALID initialized state.
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
     * Read-only iterable view.
     * Caller may snapshot if needed.
     */
    public Iterable<TrustedAccount> getAccountsView() {
        return Collections.unmodifiableCollection(accountsByEmail.values());
    }

    /* ==========================================================
     * Mutation APIs (O(1))
     * ========================================================== */

    public void addAccount(TrustedAccount account) {
        if (!isInitialized() || account == null || account.email == null) return;
        accountsByEmail.put(account.email, account);
    }

    public void removeAccount(String email) {
        if (!isInitialized() || email == null) return;
        accountsByEmail.remove(email);
    }

    public void recordUploadUsage(String email, long uploadedBytes) {
        if (uploadedBytes <= 0) return;
        updateUsage(email, uploadedBytes);
    }

    public void recordDeleteUsage(String email, long freedBytes) {
        if (freedBytes <= 0) return;
        updateUsage(email, -freedBytes);
    }

    private void updateUsage(String email, long deltaUsed) {
        if (!isInitialized() || email == null || deltaUsed == 0) return;

        TrustedAccount old = accountsByEmail.get(email);
        if (old == null) return;

        long newUsed = Math.max(0, Math.min(
                old.usedQuota + deltaUsed,
                old.totalQuota
        ));

        accountsByEmail.put(
                email,
                new TrustedAccount(
                        old.email,
                        old.totalQuota,
                        newUsed,
                        old.totalQuota - newUsed
                )
        );
    }

    /* ==========================================================
     * VaultCache hook
     * ========================================================== */

    /**
     * Clears all data AND resets initialization state.
     * Repository must call this before refresh.
     */
    @Override
    protected void onClear() {
        accountsByEmail.clear();
    }
}
