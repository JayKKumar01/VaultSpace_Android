package com.github.jaykkumar01.vaultspace.core.auth;

import android.accounts.AccountManager;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.AccountPicker.AccountChooserOptions;

import java.util.Collections;

public class AccountPickerHelper {

    private static final String TAG = "VaultSpace:AccountPicker";

    public interface Callback {
        void onAccountSelected(String email);
    }

    private final ActivityResultLauncher<Intent> pickerLauncher;

    public AccountPickerHelper(
            AppCompatActivity activity,
            Callback callback
    ) {
        this.pickerLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() != AppCompatActivity.RESULT_OK
                                    || result.getData() == null) {
                                Log.d(TAG, "Account picker cancelled");
                                return;
                            }

                            String email =
                                    result.getData().getStringExtra(
                                            AccountManager.KEY_ACCOUNT_NAME
                                    );

                            if (email == null) {
                                Log.w(TAG, "Selected account email is null");
                                return;
                            }

                            Log.d(TAG, "Account selected: " + email);
                            callback.onAccountSelected(email);
                        }
                );
    }

    /* ---------------- Public API ---------------- */

    public void launch() {
        pickerLauncher.launch(createPickerIntent());
    }

    /* ---------------- Internals ---------------- */

    private Intent createPickerIntent() {
        AccountChooserOptions options =
                new AccountChooserOptions.Builder()
                        .setAllowableAccountsTypes(
                                Collections.singletonList("com.google")
                        )
                        .build();

        return AccountPicker.newChooseAccountIntent(options);
    }
}
