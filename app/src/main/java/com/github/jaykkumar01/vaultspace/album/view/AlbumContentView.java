package com.github.jaykkumar01.vaultspace.album.view;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
import com.github.jaykkumar01.vaultspace.album.band.BandFormer;
import com.github.jaykkumar01.vaultspace.album.layout.AlbumLayoutEngine;
import com.github.jaykkumar01.vaultspace.album.layout.ResolvedBandLayout;
import com.github.jaykkumar01.vaultspace.album.layout.ResolvedItemFrame;
import com.github.jaykkumar01.vaultspace.album.model.AlbumItem;
import com.github.jaykkumar01.vaultspace.album.model.Band;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class AlbumContentView extends FrameLayout {

    private static final String TAG="VaultSpace:AlbumContent";

    private static final SimpleDateFormat DF =
            new SimpleDateFormat("dd MMM yyyy\nHH:mm:ss",Locale.getDefault());

    private static final SimpleDateFormat MONTH_FMT =
            new SimpleDateFormat("MMM yyyy",Locale.getDefault());

    private FrameLayout canvas;
    private String albumId;

    public AlbumContentView(Context c){super(c);init(c);}
    public AlbumContentView(Context c,@Nullable AttributeSet a){super(c,a);init(c);}
    public AlbumContentView(Context c,@Nullable AttributeSet a,int s){super(c,a,s);init(c);}

    private void init(Context c){
        setBackgroundColor(c.getColor(R.color.vs_content_bg));
        ScrollView scroll=new ScrollView(c);
        canvas=new FrameLayout(c);
        scroll.addView(canvas,new LayoutParams(MATCH_PARENT,WRAP_CONTENT));
        addView(scroll,new LayoutParams(MATCH_PARENT,MATCH_PARENT));
    }

    /* ================= Public API (UNCHANGED) ================= */

    public void setAlbum(String albumId,String albumName){
        this.albumId=albumId;
        Log.d(TAG,"album="+albumId);
    }

    public void setMedia(Iterable<AlbumMedia> snapshot){
        List<AlbumMedia> media=new ArrayList<>();
        for(AlbumMedia m:snapshot) media.add(m);
        Log.d(TAG,"mediaCount="+media.size());
        rebuild(media);
    }

    /* ================= Pipeline ================= */

    private void rebuild(List<AlbumMedia> media){
        if(albumId==null) return;

        media.sort(Comparator.comparingLong((AlbumMedia m)->m.momentMillis).reversed());

        List<AlbumItem> items=new ArrayList<>(media.size());
        for(int i=0;i<media.size();i++) items.add(new AlbumItem(media.get(i),i));

        List<Band> bands=BandFormer.form(items);

        int screenW=getResources().getDisplayMetrics().widthPixels;
        List<ResolvedBandLayout> snapshot=
                AlbumLayoutEngine.resolve(albumId,screenW,bands);

        renderResolved(snapshot,bands);
    }

    /* ================= Rendering ================= */

    private void renderResolved(
            List<ResolvedBandLayout> layouts,
            List<Band> bands
    ){
        canvas.removeAllViews();
        int yCursor=0;

        for(int i=0;i<layouts.size();i++){
            ResolvedBandLayout bandLayout=layouts.get(i);
            Band band=bands.get(i);

            FrameLayout bandBox=new FrameLayout(getContext());
            LayoutParams bandLp=
                    new LayoutParams(MATCH_PARENT,bandLayout.bandHeightPx);
            bandLp.topMargin=yCursor;
            bandBox.setLayoutParams(bandLp);
            bandBox.setBackground(bandBorder());

            // label (unchanged behavior)
            TextView tvLabel=new TextView(getContext());
            tvLabel.setText(resolveLabel(band.anchorMoment));
            tvLabel.setTextSize(12f);
            tvLabel.setTextColor(0x99FFFFFF);
            tvLabel.setPadding(dp(16),dp(4),dp(16),dp(4));
            bandBox.addView(tvLabel);

            for(int j=0;j<bandLayout.items.size();j++){
                ResolvedItemFrame f=bandLayout.items.get(j);

                LayoutParams lp=new LayoutParams(f.widthPx,f.heightPx);
                lp.leftMargin=f.xPx;
                lp.topMargin=f.yPx;

                View frame=makeFrame(f.debugMomentMillis);
                frame.setLayoutParams(lp);
                frame.setTranslationX(f.offsetXPx);
                frame.setTranslationY(f.offsetYPx);
                frame.setRotation(f.rotationDeg);

                logFrame(i,j,f);
                bandBox.addView(frame);
            }

            canvas.addView(bandBox);
            yCursor+=bandLayout.bandHeightPx;
        }
    }

    /* ================= Logging ================= */

    private void logFrame(int band,int index,ResolvedItemFrame f){
        Log.d(TAG,
                "F["+band+":"+index+"]"+
                        " x="+f.xPx+" y="+f.yPx+
                        " w="+f.widthPx+" h="+f.heightPx+
                        " ox="+String.format("%.2f",f.offsetXPx)+
                        " oy="+String.format("%.2f",f.offsetYPx)+
                        " rot="+String.format("%.2f",f.rotationDeg)
        );
    }

    /* ================= Labels ================= */

    private String resolveLabel(long t){
        Calendar now=Calendar.getInstance();
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(t);

        if(isSameDay(c,now)) return "Today";

        Calendar y=(Calendar)now.clone();
        y.add(Calendar.DAY_OF_YEAR,-1);
        if(isSameDay(c,y)) return "Yesterday";

        Calendar w=(Calendar)now.clone();
        w.set(Calendar.DAY_OF_WEEK,w.getFirstDayOfWeek());
        if(t>=w.getTimeInMillis()) return "This week";

        Calendar m=(Calendar)now.clone();
        m.set(Calendar.DAY_OF_MONTH,1);
        if(t>=m.getTimeInMillis()) return "This month";

        return MONTH_FMT.format(new Date(t));
    }

    private boolean isSameDay(Calendar a,Calendar b){
        return a.get(Calendar.YEAR)==b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR)==b.get(Calendar.DAY_OF_YEAR);
    }

    /* ================= Helpers ================= */

    private View makeFrame(long momentMillis){
        FrameLayout v=new FrameLayout(getContext());
        v.setBackground(border(R.color.vs_media_highlight));

        TextView tv=new TextView(getContext());
        tv.setText(DF.format(new Date(momentMillis)));
        tv.setTextSize(11f);
        tv.setTextColor(0x88FFFFFF);
        tv.setGravity(Gravity.CENTER);

        v.addView(tv,new LayoutParams(MATCH_PARENT,MATCH_PARENT));
        return v;
    }

    private GradientDrawable border(int color){
        GradientDrawable d=new GradientDrawable();
        d.setColor(0x00000000);
        d.setCornerRadius(dp(10));
        d.setStroke(dp(1),getContext().getColor(color));
        return d;
    }

    private GradientDrawable bandBorder(){
        GradientDrawable d=new GradientDrawable();
        d.setColor(0x00000000);
        d.setCornerRadius(dp(12));
        d.setStroke(dp(1),0x22FFFFFF);
        return d;
    }

    private int dp(int v){
        return Math.round(v*getResources().getDisplayMetrics().density);
    }
}
