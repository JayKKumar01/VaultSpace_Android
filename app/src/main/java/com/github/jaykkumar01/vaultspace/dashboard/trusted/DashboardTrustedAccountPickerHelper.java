package com.github.jaykkumar01.vaultspace.dashboard.trusted;

import android.accounts.AccountManager;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.auth.DriveConsentFlowHelper;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

public class DashboardTrustedAccountPickerHelper {

    private static final String TAG = "VaultSpace:TrustedPicker";

    public interface Callback {
        void onAccountSelected(String email);
    }

    private final GoogleAccountCredential credential;
    private final ActivityResultLauncher<Intent> pickerLauncher;

    public DashboardTrustedAccountPickerHelper(
            AppCompatActivity activity,
            String primaryEmail,
            Callback callback
    ) {
        // Credential owned by helper (future Drive use)
        this.credential =
                DriveConsentFlowHelper.createCredential(activity, false);

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

                            if (email.equalsIgnoreCase(primaryEmail)) {
                                Log.w(TAG, "Primary account selected as trusted — ignored");
                                return;
                            }

                            Log.d(TAG, "Trusted account selected: " + email);
                            callback.onAccountSelected(email);
                        }
                );
    }

    /* ---------------- Public API ---------------- */

    public void launch() {
        pickerLauncher.launch(credential.newChooseAccountIntent());
    }

    /* ---------------- Future-ready ---------------- */

    public GoogleAccountCredential getCredential() {
        return credential;
    }
}
