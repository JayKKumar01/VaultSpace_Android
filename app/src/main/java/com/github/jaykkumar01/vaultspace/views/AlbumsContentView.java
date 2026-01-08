package com.github.jaykkumar01.vaultspace.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.models.AlbumInfo;

import java.util.ArrayList;
import java.util.List;

public class AlbumsContentView extends FrameLayout {

    private final AlbumsMockAdapter adapter;

    public AlbumsContentView(Context context) {
        super(context);

        setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        LayoutInflater.from(context)
                .inflate(R.layout.view_mock_content, this, true);

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        adapter = new AlbumsMockAdapter();
        recyclerView.setAdapter(adapter);

        addView(recyclerView, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
    }

    /* ---------------- TEMP API ---------------- */

    public void submitAlbums(List<AlbumInfo> albums) {
        adapter.submit(albums);
    }

    /* ---------------- Dummy adapter ---------------- */

    private static class AlbumsMockAdapter
            extends RecyclerView.Adapter<AlbumsMockViewHolder> {

        private final List<AlbumInfo> items = new ArrayList<>();

        void submit(List<AlbumInfo> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public AlbumsMockViewHolder onCreateViewHolder(
                android.view.ViewGroup parent, int viewType
        ) {
            android.widget.TextView tv =
                    new android.widget.TextView(parent.getContext());
            tv.setPadding(32, 32, 32, 32);
            tv.setTextSize(14);
            return new AlbumsMockViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(
                AlbumsMockViewHolder holder, int position
        ) {
            AlbumInfo album = items.get(position);
            holder.bind(album);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class AlbumsMockViewHolder
            extends RecyclerView.ViewHolder {

        AlbumsMockViewHolder(android.view.View itemView) {
            super(itemView);
        }

        void bind(AlbumInfo album) {
            ((android.widget.TextView) itemView)
                    .setText("📁 " + album.name);
        }
    }
}
