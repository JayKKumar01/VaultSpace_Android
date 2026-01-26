package com.github.jaykkumar01.vaultspace.setup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.google.android.material.button.MaterialButton;

public final class SetupViewHolder extends RecyclerView.ViewHolder {

    private final View root, actionsRow;
    private final TextView txtEmail, txtStatus;
    private final MaterialButton btnPrimary, btnSecondary;

    private SetupViewHolder(View v) {
        super(v);
        root = v;
        txtEmail = v.findViewById(R.id.txtEmail);
        txtStatus = v.findViewById(R.id.txtStatus);
        actionsRow = v.findViewById(R.id.actionsRow);
        btnPrimary = v.findViewById(R.id.btnPrimaryAction);
        btnSecondary = v.findViewById(R.id.btnSecondaryAction);
    }

    public static SetupViewHolder create(ViewGroup parent) {
        return new SetupViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_setup_account, parent, false)
        );
    }

    public void bind(SetupRow row, SetupActionListener listener) {
        txtEmail.setText(row.email);

        root.setAlpha(1f);
        actionsRow.setVisibility(View.GONE);
        btnPrimary.setVisibility(View.GONE);
        btnSecondary.setVisibility(View.GONE);
        btnPrimary.setTextColor(color(R.color.vs_accent_primary));
        btnPrimary.setOnClickListener(null);
        btnSecondary.setOnClickListener(null);
        btnPrimary.setTextColor(color(R.color.vs_accent_primary));


        switch (row.state) {

            case HEALTHY:
                setStatus("Healthy", R.color.vs_accent_primary);
                break;

            case PARTIAL:
                setStatus("Limited access", R.color.vs_warning);
                showSecondary(row.email, listener);
                break;

            case OAUTH_REQUIRED:
                setStatus("Permission required", R.color.vs_warning);
                showPrimary("Grant access", row.email, listener);
                showSecondary(row.email, listener);
                break;

            case NOT_KNOWN_TO_APP:
                setStatus("Account not trusted", R.color.vs_error);
                showPrimary("Pick account", row.email, listener);
                showSecondary(row.email, listener);
                break;

            case IGNORED:
                root.setAlpha(0.55f);
                setStatus("Ignored", R.color.vs_text_content);
                btnPrimary.setTextColor(color(R.color.vs_accent_soft));
                showPrimary("Restore", row.email, listener);
                break;
        }
    }

    private void setStatus(String text, int colorRes) {
        txtStatus.setText(text);
        txtStatus.setTextColor(color(colorRes));
    }

    private void showPrimary(String text, String email, SetupActionListener l) {
        actionsRow.setVisibility(View.VISIBLE);
        btnPrimary.setVisibility(View.VISIBLE);
        btnPrimary.setText(text);
        btnPrimary.setOnClickListener(v ->
                l.onAction(email, SetupAction.PRIMARY));
    }

    private void showSecondary(String email, SetupActionListener l) {
        actionsRow.setVisibility(View.VISIBLE);
        btnSecondary.setVisibility(View.VISIBLE);
        btnSecondary.setText(R.string.ignore);
        btnSecondary.setOnClickListener(v ->
                l.onAction(email, SetupAction.SECONDARY));
    }

    private int color(int res) {
        return itemView.getContext().getColor(res);
    }
}
