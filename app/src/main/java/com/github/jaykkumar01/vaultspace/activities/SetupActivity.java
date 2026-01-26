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
import com.github.jaykkumar01.vaultspace.core.session.SetupIgnoreStore;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.setup.SetupAction;
import com.github.jaykkumar01.vaultspace.setup.SetupAdapter;
import com.github.jaykkumar01.vaultspace.setup.SetupRow;
import com.github.jaykkumar01.vaultspace.setup.SetupState;
import com.github.jaykkumar01.vaultspace.setup.helper.NotKnownToAppHelper;
import com.github.jaykkumar01.vaultspace.setup.helper.OAuthRequiredHelper;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {
    public static final String EXTRA_ACCOUNT_EMAILS = "trusted_account_emails";
    private static final String TAG = "VaultSpace:Setup";
    private SetupIgnoreStore ignoreStore;
    private RecyclerView setupList;
    private List<SetupRow> rows;
    private SetupAdapter adapter;
    private NotKnownToAppHelper notKnownHelper;
    private OAuthRequiredHelper oauthRequiredHelper;

    /* ==========================================================
     * Lifecycle
     * ========================================================== */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup);
        applyWindowInsets();

        ArrayList<String> emails = getIntent().getStringArrayListExtra(EXTRA_ACCOUNT_EMAILS);
        if (emails == null || emails.isEmpty()) {
            finish();
            return;
        }

        ignoreStore = new UserSession(this).getSetupIgnoreStore();
        notKnownHelper = new NotKnownToAppHelper(this);
        oauthRequiredHelper = new OAuthRequiredHelper(this);

        setupList = findViewById(R.id.setupList);
        setupList.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.ItemAnimator animator = setupList.getItemAnimator();
        if (animator instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
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

        SetupState state = rows.get(index).state;

        if (action == SetupAction.SECONDARY) {
            if (state != SetupState.IGNORED) {
                ignoreStore.ignore(email);
                updateRow(index, email);
            }
            return;
        }

        switch (state) {
            case NOT_KNOWN_TO_APP -> notKnownHelper.resolve(email, () -> updateRow(index, email));

            case OAUTH_REQUIRED ->
                    oauthRequiredHelper.resolve(email, () -> updateRow(index, email));

            case IGNORED -> handleUnignore(email, index);

            default -> Log.w(TAG, "Unhandled primary action: " + state + " for " + email);
        }

    }

    /* ==========================================================
     * State handlers
     * ========================================================== */

    private void handleUnignore(String email, int index) {
        ignoreStore.unignore(email);
        updateRow(index, email);
    }

    private void updateRow(int index, String email) {
        rows.set(index, new SetupRow(email, evaluateState(email)));
        adapter.notifyItemChanged(index);
    }

    /* ==========================================================
     * State evaluation (PURE, ORDER LOCKED)
     * ========================================================== */

    private SetupState evaluateState(String email) {

        if (ignoreStore.isIgnored(email))
            return SetupState.IGNORED;

        if (notKnownHelper.isNotKnown(email))
            return SetupState.NOT_KNOWN_TO_APP;

        if (oauthRequiredHelper.isRequired(email))
            return SetupState.OAUTH_REQUIRED;

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
