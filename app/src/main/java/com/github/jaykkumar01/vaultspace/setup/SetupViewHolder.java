package com.github.jaykkumar01.vaultspace.setup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;

final class SetupViewHolder extends RecyclerView.ViewHolder {

    private final TextView txtEmail;
    private final TextView txtStatus;
    private final TextView btnPrimary;
    private final TextView btnSecondary;

    private SetupViewHolder(View itemView) {
        super(itemView);
        txtEmail = itemView.findViewById(R.id.txtEmail);
        txtStatus = itemView.findViewById(R.id.txtStatus);
        btnPrimary = itemView.findViewById(R.id.btnPrimaryAction);
        btnSecondary = itemView.findViewById(R.id.btnSecondaryAction);
    }

    static SetupViewHolder create(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_setup_account, parent, false);
        return new SetupViewHolder(v);
    }

    void bind(SetupRow row) {
        txtEmail.setText(row.email);

        // Reset recycled state
        btnPrimary.setVisibility(View.GONE);
        btnSecondary.setVisibility(View.GONE);

        switch (row.state) {

            case HEALTHY:
                txtStatus.setText("Healthy");
                txtStatus.setTextColor(
                        txtStatus.getContext().getColor(R.color.vs_accent_primary)
                );
                break;

            case PARTIAL:
                txtStatus.setText("Limited access");
                txtStatus.setTextColor(
                        txtStatus.getContext().getColor(R.color.vs_warning)
                );
                btnSecondary.setVisibility(View.VISIBLE);
                btnSecondary.setText("Ignore");
                break;

            case OAUTH_REQUIRED:
                txtStatus.setText("Permission required");
                txtStatus.setTextColor(
                        txtStatus.getContext().getColor(R.color.vs_warning)
                );
                btnPrimary.setVisibility(View.VISIBLE);
                btnPrimary.setText("Grant access");
                btnSecondary.setVisibility(View.VISIBLE);
                btnSecondary.setText("Ignore");
                break;

            case NOT_KNOWN_TO_APP:
                txtStatus.setText("Account not trusted");
                txtStatus.setTextColor(
                        txtStatus.getContext().getColor(R.color.vs_error)
                );
                btnPrimary.setVisibility(View.VISIBLE);
                btnPrimary.setText("Add account");
                btnSecondary.setVisibility(View.VISIBLE);
                btnSecondary.setText("Ignore");
                break;

            case IGNORED:
                txtStatus.setText("Ignored");
                txtStatus.setTextColor(
                        txtStatus.getContext().getColor(R.color.vs_text_content)
                );
                btnPrimary.setVisibility(View.VISIBLE);
                btnPrimary.setText("Restore");
                btnPrimary.setTextColor(
                        btnPrimary.getContext().getColor(R.color.vs_accent_soft)
                );
                break;
        }
    }

}
