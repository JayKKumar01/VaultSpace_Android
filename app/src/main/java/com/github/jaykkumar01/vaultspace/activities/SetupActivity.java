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
import com.github.jaykkumar01.vaultspace.setup.SetupAction;
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
    private List<SetupRow> rows;
    private SetupAdapter adapter;

    /* ==========================================================
     * Lifecycle
     * ========================================================== */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup);
        applyWindowInsets();

        ArrayList<String> emails =
                getIntent().getStringArrayListExtra(EXTRA_ACCOUNT_EMAILS);
        if (emails == null || emails.isEmpty()) {
            finish();
            return;
        }

        userSession = new UserSession(this);
        trustedRepo = TrustedAccountsRepository.getInstance(this);
        ignoreStore = userSession.getSetupIgnoreStore();

        setupList = findViewById(R.id.setupList);
        setupList.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.ItemAnimator animator = setupList.getItemAnimator();
        if (animator instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) animator)
                    .setSupportsChangeAnimations(false);
        }


        ignoreStore.load(() -> buildAndBindRows(emails));
    }

    /* ==========================================================
     * Row building (ONE-TIME)
     * ========================================================== */

    private void buildAndBindRows(List<String> emails) {
        rows = new ArrayList<>(emails.size());
        for (String email : emails) {
            SetupState state = evaluateState(email);
            rows.add(new SetupRow(email, state));
            Log.d(TAG, email + " → " + state);
        }
        adapter = new SetupAdapter(rows, this::onSetupAction);
        setupList.setAdapter(adapter);
    }

    /* ==========================================================
     * Action handling (row-scoped)
     * ========================================================== */

    private void onSetupAction(String email, SetupAction action) {
        int index = findRowIndex(email);
        if (index == -1) return;

        SetupRow row = rows.get(index);
        SetupState state = row.state;

        if (action == SetupAction.SECONDARY) {
            if (state != SetupState.IGNORED) {
                ignoreStore.ignore(email);
                rows.set(index, new SetupRow(email, SetupState.IGNORED));
                adapter.notifyItemChanged(index);
            }
            return;
        }

        switch (state) {

            case OAUTH_REQUIRED:
                Log.d(TAG, "Grant OAuth for " + email);
                // startOAuthFlow(email);
                break;

            case NOT_KNOWN_TO_APP:
                Log.d(TAG, "Add account " + email);
                // startAddAccountFlow(email);
                break;

            case IGNORED:
                ignoreStore.unignore(email);
                rows.set(index, new SetupRow(email, evaluateState(email)));
                adapter.notifyItemChanged(index);
                break;

            default:
                Log.w(TAG, "Unhandled primary action: " + state + " for " + email);
        }
    }

    /* ==========================================================
     * State evaluation (PURE, ORDER LOCKED)
     * ========================================================== */

    private SetupState evaluateState(String email) {

        if (ignoreStore.isIgnored(email)) return SetupState.IGNORED;
        if (trustedRepo.getAccountSnapshot(email) == null)
            return SetupState.NOT_KNOWN_TO_APP;
        if (requiresOAuth(email)) return SetupState.OAUTH_REQUIRED;
        if (isPartial(email)) return SetupState.PARTIAL;

        return SetupState.HEALTHY;
    }

    /* ==========================================================
     * Lookup helpers
     * ========================================================== */

    private int findRowIndex(String email) {
        for (int i = 0; i < rows.size(); i++)
            if (rows.get(i).email.equals(email)) return i;
        return -1;
    }

    /* ==========================================================
     * Placeholder checks (NO SIDE EFFECTS)
     * ========================================================== */

    private boolean requiresOAuth(String email) {
        return false;
    }

    private boolean isPartial(String email) {
        return false;
    }

    /* ==========================================================
     * UI helpers
     * ========================================================== */

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),
                (v, insets) -> {
                    Insets bars =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });
    }
}
