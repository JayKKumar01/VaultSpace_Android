package com.github.jaykkumar01.vaultspace.views.creative.delete;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;

public final class DeleteStatusRenderer {

    public DeleteStatusRenderModel render(
            String albumName,
            String fileName,
            int deleted,
            int total,
            View.OnClickListener cancel
    ) {
        float p = total > 0 ? Math.min(1f, deleted / (float) total) : 0f;
        return new DeleteStatusRenderModel(
                buildTitle(albumName, fileName),
                p,
                cancel
        );
    }

    private CharSequence buildTitle(String album, String file) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append("Deleting “")
          .append(album)
          .append("” · ")
          .append(file != null ? file : "…");

        sb.setSpan(new StyleSpan(Typeface.BOLD),
                0, sb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }
}
