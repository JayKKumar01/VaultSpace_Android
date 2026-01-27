package com.github.jaykkumar01.vaultspace.activities;

import android.os.Bundle;

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
import com.github.jaykkumar01.vaultspace.setup.helper.StateHelper;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalEnums;
import com.github.jaykkumar01.vaultspace.views.popups.core.ModalHost;
import com.github.jaykkumar01.vaultspace.views.popups.loading.LoadingSpec;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {
    public static final String EXTRA_ACCOUNT_EMAILS = "trusted_account_emails";
    private static final String TAG = "VaultSpace:Setup";
    private ModalHost modalHost;
    private final LoadingSpec loading = new LoadingSpec();
    private SetupIgnoreStore ignoreStore;
    private List<SetupRow> rows;
    private SetupAdapter adapter;
    private StateHelper stateHelper;


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

        modalHost = ModalHost.attach(this);
        ignoreStore = new UserSession(this).getSetupIgnoreStore();
        stateHelper = new StateHelper(this);

        RecyclerView list = findViewById(R.id.setupList);
        list.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.ItemAnimator a = list.getItemAnimator();
        if (a instanceof androidx.recyclerview.widget.SimpleItemAnimator)
            ((androidx.recyclerview.widget.SimpleItemAnimator) a).setSupportsChangeAnimations(false);

        findViewById(R.id.btnFinishSetup).setOnClickListener(v -> finish());

        ignoreStore.load(() -> buildRows(emails, list));
    }

    private void buildRows(List<String> emails, RecyclerView list) {
        rows = new ArrayList<>(emails.size());
        for (String e : emails) rows.add(new SetupRow(e, evaluateInitialState(e)));
        adapter = new SetupAdapter(rows, this::onAction);
        list.setAdapter(adapter);
    }

    private void onAction(String email, SetupAction action) {
        int i = findIndex(email);
        if (i == -1) return;

        SetupState state = rows.get(i).state;

        if (action == SetupAction.SECONDARY) {
            if (state != SetupState.IGNORED) {
                ignoreStore.ignore(email);
                rows.set(i, new SetupRow(email, SetupState.IGNORED));
            }
            adapter.notifyItemChanged(i);
            return;
        }

        if (state == SetupState.IGNORED) {
            ignoreStore.unignore(email);
            rows.set(i, new SetupRow(email, evaluateInitialState(email)));
            adapter.notifyItemChanged(i);
            return;
        }

        modalHost.request(loading);
        stateHelper.resolve(email, nextState -> {
            modalHost.dismiss(loading, ModalEnums.DismissResult.SYSTEM);
            rows.set(i, new SetupRow(email, nextState));
            adapter.notifyItemChanged(i);
        });
    }

    private SetupState evaluateInitialState(String email) {
        if (ignoreStore.isIgnored(email)) return SetupState.IGNORED;
        if (stateHelper.isNotKnown(email)) return SetupState.NOT_KNOWN_TO_APP;
        if (stateHelper.isLimited(email)) return SetupState.LIMITED;
        return SetupState.HEALTHY;
    }

    private int findIndex(String email) {
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