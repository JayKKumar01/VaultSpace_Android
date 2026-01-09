package com.github.jaykkumar01.vaultspace.views;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

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

        if (album.coverPath != null) {
            albumPlaceholder.setVisibility(View.GONE);
            albumCover.setVisibility(View.VISIBLE);

            Glide.with(albumCover)
                    .load(album.coverPath)   // file path / uri / url
                    .centerInside()           // matches your fitCenter intent
                    .dontAnimate()            // avoids flicker on rebinding
                    .into(albumCover);

        } else {
            // Clear recycled image
            Glide.with(albumCover).clear(albumCover);

            albumCover.setImageDrawable(null);
            albumCover.setVisibility(View.GONE);
            albumPlaceholder.setVisibility(View.VISIBLE);
        }

        // Overflow menu click
        albumOverflow.setOnClickListener(v ->
                callbacks.onOverflowClicked(album)
        );

        // Long press on entire card
        itemView.setOnLongClickListener(v -> {
            callbacks.onLongPressed(album);
            return true;
        });
    }
}
