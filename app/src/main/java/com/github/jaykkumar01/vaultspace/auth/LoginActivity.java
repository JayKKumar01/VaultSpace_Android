package com.github.jaykkumar01.vaultspace.auth;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jaykkumar01.vaultspace.R;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpaceLogin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        applySystemInsets();
        bindActions();
    }

    /* ---------------------------------------------------
     * UI wiring
     * --------------------------------------------------- */

    private void bindActions() {

        // Primary action (future official flow)
        findViewById(R.id.btnSelectPrimaryDrive)
                .setOnClickListener(v -> {
                    Log.d(TAG, "Primary Drive button clicked");
                    onPrimaryDriveSelected();
                });

        // Debug / exploration button
        findViewById(R.id.btnTestDriveOAuth)
                .setOnClickListener(v -> {
                    Log.d(TAG, "Debug OAuth button clicked");
                    onDebugOAuthClicked();
                });
    }

    /* ---------------------------------------------------
     * Intent handlers (empty by design)
     * --------------------------------------------------- */

    /**
     * This will become the ONLY official entry point
     * for Drive ownership selection.
     *
     * For now, it does nothing except log.
     */
    private void onPrimaryDriveSelected() {

        Toast.makeText(
                this,
                "Primary Drive flow not decided yet",
                Toast.LENGTH_SHORT
        ).show();

        // Future options (official only):
        // - Google Sign-In (Android SDK)
        // - Account picker + Drive API
        // - Backend-assisted OAuth
    }

    /**
     * Temporary exploration hook.
     * Safe place to test ideas without polluting main flow.
     */
    private void onDebugOAuthClicked() {

        Toast.makeText(
                this,
                "Debug mode – no active OAuth flow",
                Toast.LENGTH_SHORT
        ).show();

        // Intentionally empty
    }

    /* ---------------------------------------------------
     * Insets
     * --------------------------------------------------- */

    private void applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                }
        );
    }
}
