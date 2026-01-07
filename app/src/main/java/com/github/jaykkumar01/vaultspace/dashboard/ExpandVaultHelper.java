package com.github.jaykkumar01.vaultspace.dashboard;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.consent.DriveConsentHelper;
import com.github.jaykkumar01.vaultspace.core.picker.AccountPickerHelper;

public class ExpandVaultHelper {

    public interface Callback {
        void onStart();                 // 🔄 show loader
        void onTrustedAccountAdded();   // ✅ success
        void onError(String message);   // ❌ error
        void onEnd();                   // 🔚 hide loader / cleanup
    }

    private final AppCompatActivity activity;
    private final String primaryEmail;
    private final TrustedAccountsDriveHelper driveHelper;

    private final AccountPickerHelper accountPickerHelper;
    private final DriveConsentHelper driveConsentHelper;

    private Callback callback;

    /* ---------------- Constructor ---------------- */

    public ExpandVaultHelper(AppCompatActivity activity, String primaryEmail) {
        this.activity = activity;
        this.primaryEmail = primaryEmail;

        driveHelper = new TrustedAccountsDriveHelper(activity.getApplicationContext(), primaryEmail);
        accountPickerHelper = new AccountPickerHelper(activity);
        driveConsentHelper = new DriveConsentHelper(activity);
    }

    /* ---------------- Public API ---------------- */

    public void launch(Callback callback) {
        this.callback = callback;
        callback.onStart(); // user initiated action

        accountPickerHelper.launch(new AccountPickerHelper.Callback() {
            @Override
            public void onAccountSelected(String email) {
                onAccountPicked(email);
            }

            @Override
            public void onCancelled() {
                end(); // picker dismissed → stop loader
            }
        });
    }

    /* ---------------- Flow ---------------- */

    private void onAccountPicked(String email) {

        if (email == null) {
            end();
            return;
        }

        if (email.equalsIgnoreCase(primaryEmail)) {
            toast("Primary account cannot be added as trusted");
            end();
            return;
        }

        requestConsent(email);
    }

    private void requestConsent(String trustedEmail) {

        driveConsentHelper.launch(
                trustedEmail,
                new DriveConsentHelper.Callback() {

                    @Override
                    public void onConsentGranted(String email) {
                        addTrustedAccount(email);
                    }

                    @Override
                    public void onConsentDenied(String email) {
                        callback.onError(
                                "Drive permission is required to add trusted account"
                        );
                        end();
                    }

                    @Override
                    public void onFailure(String email, Exception e) {
                        callback.onError(
                                "Failed to verify Drive permissions"
                        );
                        end();
                    }
                }
        );
    }

    private void addTrustedAccount(String trustedEmail) {

        driveHelper.addTrustedAccountAsync(
                trustedEmail,
                new TrustedAccountsDriveHelper.AddResultCallback() {

                    @Override
                    public void onAdded() {
                        activity.runOnUiThread(() -> {
                            toast("Trusted account added");
                            callback.onTrustedAccountAdded();
                            end();
                        });
                    }

                    @Override
                    public void onAlreadyExists() {
                        activity.runOnUiThread(() -> {
                            toast("Account already has access");
                            end();
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        activity.runOnUiThread(() -> {
                            callback.onError("Failed to grant access");
                            end();
                        });
                    }
                }
        );
    }

    /* ---------------- End handling ---------------- */

    private void end() {
        if (callback != null) {
            callback.onEnd();
            callback = null; // 🔒 prevent leaks / double calls
        }
    }

    /* ---------------- UI ---------------- */

    private void toast(String msg) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
    }
}
