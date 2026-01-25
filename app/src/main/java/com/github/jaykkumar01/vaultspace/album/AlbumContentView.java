package com.github.jaykkumar01.vaultspace.album;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.R;

public class AlbumContentView extends FrameLayout {

    private ScrollView scrollView;
    private LinearLayout listContainer;

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

    private void init(Context context){
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));

        scrollView=new ScrollView(context);
        scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));

        listContainer=new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        int pad= 4;
        listContainer.setPadding(pad,pad,pad,pad);

        scrollView.addView(listContainer);
        addView(scrollView);
    }

    private View createItem(Context context, AlbumMedia media){
        TextView tv=new TextView(context);
        tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        tv.setText(media.name+(media.isVideo?" (Video)":" (Photo)"));
        tv.setTextSize(14);
        tv.setTextColor(context.getColor(R.color.vs_text_content));
        tv.setPadding(0,24,0,24);
        return tv;
    }
    public void setMedia(Iterable<AlbumMedia> snapshotList){
        listContainer.removeAllViews();
        Context context=getContext();
        for(AlbumMedia media:snapshotList){
            listContainer.addView(createItem(context,media));
        }
    }

    public void addMedia(AlbumMedia media){
        View item=createItem(getContext(),media);
        listContainer.addView(item,0);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_UP));
    }


}
