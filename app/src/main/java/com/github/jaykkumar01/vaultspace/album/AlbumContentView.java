package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jaykkumar01.vaultspace.R;

import java.util.ArrayList;
import java.util.List;

public class AlbumContentView extends FrameLayout {

    private RecyclerView recyclerView;
    private AlbumAdapter adapter;

    public AlbumContentView(Context context) {
        super(context);
        init(context);
    }

    public AlbumContentView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AlbumContentView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setBackgroundColor(context.getColor(R.color.vs_content_bg));

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setItemAnimator(null); // 🔥 reduces jank
        recyclerView.setHasFixedSize(true);

        adapter = new AlbumAdapter(context);
        recyclerView.setAdapter(adapter);

        addView(recyclerView, new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
    }

    /* ================= Public API (UNCHANGED) ================= */

    public void setMedia(Iterable<AlbumMedia> snapshotList) {
        List<AlbumMedia> list = new ArrayList<>();
        for (AlbumMedia m : snapshotList) list.add(m);
        adapter.setItems(list);
    }

    public void addMedia(AlbumMedia media) {
        adapter.addItem(media);
        recyclerView.scrollToPosition(0);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        adapter.release();
    }
}
