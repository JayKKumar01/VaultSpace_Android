package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.helper.DriveThumbnailResolver;

import java.util.ArrayList;
import java.util.List;

final class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.VH> {

    private final List<AlbumMedia> items = new ArrayList<>();
    private final DriveThumbnailResolver resolver;
    private final Context context;

    AlbumAdapter(Context context) {
        this.context = context;
        this.resolver = new DriveThumbnailResolver(context);
        setHasStableIds(true);
    }

    /* ================= Public API ================= */

    void setItems(List<AlbumMedia> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged(); // simple & fine for now
    }

    void addItem(AlbumMedia media) {
        items.add(0, media);
        notifyItemInserted(0);
    }

    /* ================= Adapter ================= */

    @Override
    public long getItemId(int position) {
        return items.get(position).fileId.hashCode();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(createItemView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AlbumMedia m = items.get(position);

        h.title.setText(m.name);
        h.subtitle.setText((m.isVideo ? "Video" : "Photo") + " • " + formatSize(m.sizeBytes));
        h.meta.setText(DateUtils.getRelativeTimeSpanString(
                m.modifiedTime,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        ));

        h.thumb.setImageResource(R.drawable.ic_file);

        resolver.resolveAsync(m, resolved -> {
            if (resolved == null) return;
            h.thumb.post(() -> Glide.with(h.thumb)
                    .load(resolved)
                    .placeholder(R.drawable.ic_file)
                    .error(R.drawable.ic_file)
                    .into(h.thumb));
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void release() {
        resolver.release();
    }

    /* ================= ViewHolder ================= */

    static final class VH extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView title, subtitle, meta;

        VH(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewWithTag("thumb");
            title = itemView.findViewWithTag("title");
            subtitle = itemView.findViewWithTag("subtitle");
            meta = itemView.findViewWithTag("meta");
        }
    }

    /* ================= Row UI ================= */

    private View createItemView(Context c) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(dp(c, 8), dp(c, 8), dp(c, 8), dp(c, 8));
        root.setBackgroundColor(c.getColor(R.color.vs_surface_soft));

        ImageView thumb = new ImageView(c);
        thumb.setTag("thumb");
        thumb.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 80), dp(c, 80)));
        thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
        thumb.setAdjustViewBounds(true);
        thumb.setBackgroundColor(c.getColor(R.color.vs_media_back));
        thumb.setPadding(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4));
        root.addView(thumb);

        LinearLayout textCol = new LinearLayout(c);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(dp(c, 12), 0, 0, 0);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(c);
        title.setTag("title");
        title.setTextSize(14);
        title.setMaxLines(1);
        title.setTextColor(c.getColor(R.color.vs_text_header));
        textCol.addView(title);

        TextView subtitle = new TextView(c);
        subtitle.setTag("subtitle");
        subtitle.setTextSize(12);
        subtitle.setTextColor(c.getColor(R.color.vs_text_content));
        textCol.addView(subtitle);

        TextView meta = new TextView(c);
        meta.setTag("meta");
        meta.setTextSize(11);
        meta.setTextColor(c.getColor(R.color.vs_media_front));
        textCol.addView(meta);

        root.addView(textCol);
        return root;
    }

    /* ================= Utils ================= */

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format(
                "%.1f %s",
                bytes / Math.pow(1024, exp),
                "KMGTPE".charAt(exp - 1) + "B"
        );
    }
}
