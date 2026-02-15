package com.github.jaykkumar01.vaultspace.dashboard.albums;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.dashboard.interfaces.AlbumItemCallbacks;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.github.jaykkumar01.vaultspace.utils.AlbumUiUtils;

class AlbumsViewHolder extends RecyclerView.ViewHolder {

    private final TextView albumName;
    private final ImageView albumCover;
    private final ImageView albumPlaceholder;
    private final ImageButton albumOverflow;
    private final AlbumItemCallbacks callbacks;

    AlbumsViewHolder(
            @NonNull View itemView,
            @NonNull AlbumItemCallbacks callbacks
    ) {
        super(itemView);
        this.callbacks = callbacks;

        albumName = itemView.findViewById(R.id.album_name);
        albumCover = itemView.findViewById(R.id.album_cover);
        albumPlaceholder = itemView.findViewById(R.id.album_placeholder);
        albumOverflow = itemView.findViewById(R.id.album_overflow);
    }

    void bind(AlbumInfo album) {
        albumName.setText(album.name);

        final boolean isTemp = AlbumUiUtils.isTempAlbum(album);

        /* ---------------- Reset recycled state ---------------- */

        stopLoadingAnimation(albumPlaceholder);

        itemView.setAlpha(1f);
        itemView.setOnLongClickListener(null);
        albumOverflow.setOnClickListener(null);
        albumOverflow.setVisibility(View.VISIBLE);

        albumPlaceholder.setImageResource(R.drawable.ic_album_placeholder);

        /* ---------------- Cover / Placeholder ---------------- */

        if (album.coverPath != null) {
            albumPlaceholder.setVisibility(View.GONE);
            albumCover.setVisibility(View.VISIBLE);

            Glide.with(albumCover)
                    .load(album.coverPath)
                    .centerInside()
                    .dontAnimate()
                    .into(albumCover);

        } else {
            Glide.with(albumCover).clear(albumCover);

            albumCover.setImageDrawable(null);
            albumCover.setVisibility(View.GONE);
            albumPlaceholder.setVisibility(View.VISIBLE);
        }

        /* ---------------- Temp Album UI ---------------- */

        if (isTemp) {
            itemView.setAlpha(0.6f);
            albumOverflow.setVisibility(View.GONE);

            startLoadingAnimation(albumPlaceholder);
            return; // 🚫 no interactions
        }

        /* ---------------- Normal Album Actions ---------------- */

        albumOverflow.setOnClickListener(v ->
                callbacks.onOverflowClicked(album)
        );

        itemView.setOnLongClickListener(v -> {
            v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(80)
                    .withEndAction(() ->
                            v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    )
                    .start();

            callbacks.onLongPressed(album);
            return true;
        });
    }

    /* ---------------- Loading Animation ---------------- */

    private void startLoadingAnimation(View v) {
        v.animate()
                .alpha(0.4f)
                .setDuration(900)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    if (v.getAlpha() < 0.5f) {
                        v.animate()
                                .alpha(0.9f)
                                .setDuration(900)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .withEndAction(() -> startLoadingAnimation(v))
                                .start();
                    }
                })
                .start();
    }

    private void stopLoadingAnimation(View v) {
        v.animate().cancel();
        v.setAlpha(1f);
    }
}
