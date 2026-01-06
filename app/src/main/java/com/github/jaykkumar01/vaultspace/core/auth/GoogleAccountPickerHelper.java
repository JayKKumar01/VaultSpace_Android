package com.github.jaykkumar01.vaultspace.core.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;

public class GoogleAccountPickerHelper {

    private static final String TAG = "VaultSpace:AccountPicker";

    public interface Callback {
        void onAccountPicked(Account account);
        void onCancelled();
    }

    private final ActivityResultLauncher<Intent> launcher;
    private final Callback callback;

    public GoogleAccountPickerHelper(
            ActivityResultLauncher<Intent> launcher,
            Callback callback
    ) {
        this.launcher = launcher;
        this.callback = callback;
    }

    public void handleResult(int resultCode, Intent data) {

        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Account picker cancelled");
            callback.onCancelled();
            return;
        }

        String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

        if (email == null) {
            Log.e(TAG, "No account returned from picker");
            callback.onCancelled();
            return;
        }

        Log.d(TAG, "Account picked: " + email);
        callback.onAccountPicked(new Account(email, "com.google"));
    }
}
