package com.github.jaykkumar01.vaultspace.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.core.session.SetupIgnoreStore;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.setup.SetupAdapter;
import com.github.jaykkumar01.vaultspace.setup.SetupRow;
import com.github.jaykkumar01.vaultspace.setup.SetupState;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT_EMAILS = "trusted_account_emails";
    private static final String TAG = "VaultSpace:Setup";

    private UserSession userSession;
    private TrustedAccountsRepository trustedRepo;
    private SetupIgnoreStore ignoreStore;

    private RecyclerView setupList;

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
        if (extras == null || extras.isEmpty()) {
            finish();
            return;
        }

        userSession = new UserSession(this);
        trustedRepo = TrustedAccountsRepository.getInstance(this);
        ignoreStore = userSession.getSetupIgnoreStore();

        setupList = findViewById(R.id.setupList);
        setupList.setLayoutManager(new LinearLayoutManager(this));

        // ---- Load ignore store FIRST, then build rows
        ignoreStore.load(() -> buildAndBindRows(extras));
    }

    /* ==========================================================
     * Row building (PURE after load)
     * ========================================================== */

    private void buildAndBindRows(List<String> emails) {
        List<SetupRow> rows = new ArrayList<>(emails.size());

        for (String email : emails) {
            SetupState state = evaluateInitialState(email);
            rows.add(new SetupRow(email, state));
            Log.d(TAG, email + " → " + state);
        }

        setupList.setAdapter(new SetupAdapter(rows));
    }

    /* ==========================================================
     * Initial evaluator (PURE, ORDER IS LOCKED)
     * ========================================================== */

    private SetupState evaluateInitialState(String email) {

        // 0️⃣ Explicitly ignored by user
        if (ignoreStore.isIgnored(email)) {
            return SetupState.IGNORED;
        }

        // 1️⃣ App does not know / trust this account
        if (trustedRepo.getAccountSnapshot(email) == null) {
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

    private boolean requiresOAuth(String email) {
        // TODO: Drive OAuth / UserRecoverableAuthIOException
        return false;
    }

    private boolean isPartial(String email) {
        // TODO: quota fetch fail-soft / permission downgrade
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
