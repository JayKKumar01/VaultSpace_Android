package com.github.jaykkumar01.vaultspace.dashboard.files;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.models.FileNode;

class FileViewHolder extends RecyclerView.ViewHolder {

    private final ImageView icon;
    private final TextView name;
    private final TextView size;

    private ObjectAnimator pulseAnimator;

    FileViewHolder(@NonNull View itemView) {
        super(itemView);
        icon = itemView.findViewById(R.id.icon);
        name = itemView.findViewById(R.id.name);
        size = itemView.findViewById(R.id.size);
    }

    void bind(FileNode node, FilesContentView.OnItemInteractionListener listener) {

        stopPulse(); // Important for recycling safety

        name.setText(node.name);
        icon.setImageResource(node.isFolder ? R.drawable.ic_folder : R.drawable.ic_file);

        if (node.isFolder) {
            size.setVisibility(View.GONE);
        } else {
            String formatted = formatSize(node.sizeBytes);
            if (formatted.isEmpty()) size.setVisibility(View.GONE);
            else {
                size.setVisibility(View.VISIBLE);
                size.setText(formatted);
            }
        }

        boolean isTemp = node.id != null && node.id.startsWith("temp_");

        if (isTemp) {
            startPulse();
            disableInteractions();
            return;
        }

        enableInteractions(node, listener);
    }

    /* ================= Pending Animation ================= */

    private void startPulse() {
        pulseAnimator = ObjectAnimator.ofFloat(itemView, View.ALPHA, 1f, 0.4f);
        pulseAnimator.setDuration(800);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.start();
    }

    private void stopPulse() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        itemView.setAlpha(1f);
    }

    /* ================= Interaction Handling ================= */

    private void disableInteractions() {
        itemView.setOnClickListener(null);
        itemView.setOnLongClickListener(null);
    }

    private void enableInteractions(FileNode node,
                                    FilesContentView.OnItemInteractionListener listener) {

        itemView.setOnClickListener(v -> {
            if (listener == null) return;
            if (node.isFolder) listener.onFolderClick(node);
            else listener.onFileClick(node);
        });

        itemView.setOnLongClickListener(v -> {
            if (listener == null) return false;
            if (node.isFolder) listener.onFolderLongClick(node);
            else listener.onFileLongClick(node);
            return true;
        });
    }

    /* ================= Size Formatter ================= */

    @SuppressLint("DefaultLocale")
    private String formatSize(long bytes) {
        if (bytes <= 0) return "";
        float kb = bytes / 1024f;
        if (kb < 1024) return String.format("%.1f KB", kb);
        float mb = kb / 1024f;
        if (mb < 1024) return String.format("%.1f MB", mb);
        float gb = mb / 1024f;
        return String.format("%.1f GB", gb);
    }
}