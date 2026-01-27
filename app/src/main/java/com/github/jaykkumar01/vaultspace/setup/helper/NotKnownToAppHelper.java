package com.github.jaykkumar01.vaultspace.setup.helper;

import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.github.jaykkumar01.vaultspace.core.selection.AccountSelectionHelper;

public final class NotKnownToAppHelper {

    private static final String TAG = "VaultSpace:NotKnown";

    private final Context context;
    private final AccountSelectionHelper picker;

    public NotKnownToAppHelper(AppCompatActivity activity) {
        this.context = activity;
        this.picker = new AccountSelectionHelper(activity);
    }

    /* ==========================================================
     * PURE CHECK
     * ========================================================== */

    private boolean canAccess(String email) {
        return GoogleCredentialFactory.canAccessAccount(context, email);
    }

    public boolean isNotKnown(String email) {
        return !canAccess(email);
    }

    /* ==========================================================
     * FIX
     * ========================================================== */

    public void resolve(String email, Runnable onResolved, Runnable onError) {

        if (canAccess(email)) {
            onResolved.run();
            return;
        }

        picker.launch(new AccountSelectionHelper.Callback() {

            @Override
            public void onAccountSelected(String selectedEmail) {
                if (!email.equalsIgnoreCase(selectedEmail)) {
                    Log.w(TAG, "Wrong account selected for " + email);
                    onError.run();
                    return;
                }

                if (canAccess(email)) {
                    onResolved.run();
                    return;
                }
                Log.w(TAG, "Account still not visible after picker: " + email);
                onError.run();
            }

            @Override
            public void onCancelled() {
                onError.run();
            }
        });
    }

}
