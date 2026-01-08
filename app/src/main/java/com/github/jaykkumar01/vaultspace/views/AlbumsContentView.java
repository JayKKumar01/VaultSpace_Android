package com.github.jaykkumar01.vaultspace.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;
import com.github.jaykkumar01.vaultspace.views.util.VaultFabUtil;

import java.util.ArrayList;
import java.util.List;

public class AlbumsContentView extends FrameLayout {

    public interface OnAlbumClickListener {
        void onAlbumClick(AlbumInfo album);
    }

    private final AlbumsAdapter adapter;
    private final ImageButton addAlbumFab;

    public AlbumsContentView(Context context) {
        super(context);

        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));

        adapter = new AlbumsAdapter();
        recyclerView.setAdapter(adapter);
        addView(recyclerView);

        addAlbumFab = VaultFabUtil.createAddAlbumFab(context);
        addView(addAlbumFab);
    }

    /* ---------------- Public API ---------------- */

    public void submitAlbums(List<AlbumInfo> albums) {
        adapter.submit(albums);
    }

    public void setOnAddAlbumClickListener(OnClickListener listener) {
        addAlbumFab.setOnClickListener(listener);
    }

    public void setFabVisible(boolean visible) {
        addAlbumFab.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setOnAlbumClickListener(OnAlbumClickListener listener) {
        adapter.setOnAlbumClickListener(listener);
    }

    /* ---------------- Adapter ---------------- */

    private static class AlbumsAdapter
            extends RecyclerView.Adapter<AlbumsViewHolder> {

        private final List<AlbumInfo> items = new ArrayList<>();
        private OnAlbumClickListener clickListener;

        void submit(List<AlbumInfo> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        void setOnAlbumClickListener(OnAlbumClickListener listener) {
            this.clickListener = listener;
        }

        @NonNull
        @Override
        public AlbumsViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType
        ) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_album, parent, false);
            return new AlbumsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull AlbumsViewHolder holder, int position
        ) {
            AlbumInfo album = items.get(position);
            holder.bind(album);

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAlbumClick(album);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class AlbumsViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView albumName;
        private final View albumCover;
        private final View albumPlaceholder;

        AlbumsViewHolder(@NonNull View itemView) {
            super(itemView);
            albumName = itemView.findViewById(R.id.album_name);
            albumCover = itemView.findViewById(R.id.album_cover);
            albumPlaceholder = itemView.findViewById(R.id.album_placeholder);
        }

        void bind(AlbumInfo album) {
            albumName.setText(album.name);


            if (album.cover != null) {
                // later use Glide to show the image
                albumPlaceholder.setVisibility(View.GONE);
                albumCover.setVisibility(View.VISIBLE);
            } else {
                albumPlaceholder.setVisibility(View.VISIBLE);
                albumCover.setVisibility(View.GONE);
            }
        }
    }
}
