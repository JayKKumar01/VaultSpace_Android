package com.github.jaykkumar01.vaultspace.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.core.session.UserSession;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            proceedIntoApp();
                        }
                        // else: user denied → app stays here
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Gate everything on notification permission
        if (needsNotificationPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission();
            }
            return;
        }

        proceedIntoApp();
    }

    /* ==========================================================
     * Permission gating
     * ========================================================== */

    private boolean needsNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false; // API < 33 → no permission needed
        }

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission() {
        notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
        );
    }

    /* ==========================================================
     * Normal app flow
     * ========================================================== */

    private void proceedIntoApp() {
        UserSession session = new UserSession(this);

        Intent intent = session.isLoggedIn()
                ? new Intent(this, DashboardActivity.class)
                : new Intent(this, LoginActivity.class);

        startActivity(intent);
        finish();
    }
}
