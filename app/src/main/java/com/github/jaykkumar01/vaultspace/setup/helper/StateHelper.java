package com.github.jaykkumar01.vaultspace.setup.helper;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.github.jaykkumar01.vaultspace.setup.SetupState;

import java.util.function.Consumer;

public final class StateHelper {

    private static final String TAG = "VaultSpace:NotKnown";

    private final NotKnownToAppHelper notKnownHelper;
    private final OAuthRequiredHelper oauthRequiredHelper;
    private final TrustedAccountsRepository repo;

    public StateHelper(AppCompatActivity activity) {
        notKnownHelper = new NotKnownToAppHelper(activity);
        oauthRequiredHelper = new OAuthRequiredHelper(activity);
        this.repo = TrustedAccountsRepository.getInstance(activity);
    }

    /* ==========================================================
     * PURE CHECK
     * ========================================================== */

    public boolean isNotKnown(String email) {
        return notKnownHelper.isNotKnown(email);
    }

    public boolean isLimited(String email) {
        return repo.getAccountSnapshot(email) == null;
    }

    /* ==========================================================
     * FIX
     * ========================================================== */

    public void resolve(String email, Consumer<SetupState> nextState, Runnable onError) {
        notKnownHelper.resolve(email, ()-> onNotKnownSuccess(email, nextState), onError);
    }

    private void onNotKnownSuccess(String email, Consumer<SetupState> nextState) {
        oauthRequiredHelper.resolve(email, (account) -> onOauthResolved(account, nextState), ()-> nextState.accept(SetupState.OAUTH_REQUIRED));
    }

    private void onOauthResolved(TrustedAccount account, Consumer<SetupState> nextState) {
        repo.addAccount(account);
        nextState.accept(SetupState.HEALTHY);
    }
}
