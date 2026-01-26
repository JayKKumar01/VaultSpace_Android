package com.github.jaykkumar01.vaultspace.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT_EMAILS = "trusted_account_emails";
    private static final String TAG = "VaultSpace:Setup";

    private List<String> accountEmails;
    private TrustedAccountsRepository trustedRepo;

    /* ==========================================================
     * Setup states (DISPLAY STATES — NO FIXING)
     * ========================================================== */

    private enum SetupState {
        NOT_KNOWN_TO_APP,
        OAUTH_REQUIRED,
        PARTIAL,
        HEALTHY
    }

    /* ==========================================================
     * Lifecycle
     * ========================================================== */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup);
        applyWindowInsets();

        ArrayList<String> extras =
                getIntent().getStringArrayListExtra(EXTRA_ACCOUNT_EMAILS);
        if (extras == null || extras.isEmpty()) { finish(); return; }

        accountEmails = new ArrayList<>(extras);
        trustedRepo = TrustedAccountsRepository.getInstance(this);

        // ---- Initial scan ONLY (no fixes)
        for (String email : accountEmails) {
            SetupState state = evaluateInitialState(email);
            Log.d(TAG, email + " → " + state);
        }
    }

    /* ==========================================================
     * Initial evaluator (PLACEHOLDER ONLY)
     * ORDER IS LOCKED
     * ========================================================== */

    private SetupState evaluateInitialState(String email) {

        // 1️⃣ App does not know this account
        if (!isKnownToApp(email)) {
            return SetupState.NOT_KNOWN_TO_APP;
        }

        // 2️⃣ OAuth unusable (placeholder)
        if (requiresOAuth(email)) {
            return SetupState.OAUTH_REQUIRED;
        }

        // 3️⃣ Partial / degraded (placeholder)
        if (isPartial(email)) {
            return SetupState.PARTIAL;
        }

        // 4️⃣ Fully healthy
        return SetupState.HEALTHY;
    }

    /* ==========================================================
     * Placeholder checks (NO SIDE EFFECTS)
     * ========================================================== */

    private boolean isKnownToApp(String email) {
        return trustedRepo.getAccountSnapshot(email) != null;
    }

    private boolean requiresOAuth(String email) {
        // placeholder
        return false;
    }

    private boolean isPartial(String email) {
        // placeholder
        return false;
    }

    /* ==========================================================
     * UI helpers
     * ========================================================== */

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
