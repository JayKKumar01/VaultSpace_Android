////package com.github.jaykkumar01.vaultspace.album.view;
////
////public class backup {
////}
//
//view should not decide the dimensions package com.github.jaykkumar01.vaultspace.album.view;
//
//import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
//import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
//
//import android.content.Context;
//import android.graphics.drawable.GradientDrawable;
//import android.util.AttributeSet;
//import android.util.Log;
//import android.view.Gravity;
//import android.view.View;
//import android.widget.FrameLayout;
//import android.widget.ScrollView;
//import android.widget.TextView;
//
//import androidx.annotation.Nullable;
//
//import com.github.jaykkumar01.vaultspace.R;
//import com.github.jaykkumar01.vaultspace.album.AlbumMedia;
//import com.github.jaykkumar01.vaultspace.album.band.BandFormer;
//import com.github.jaykkumar01.vaultspace.album.layout.BandLayout;
//import com.github.jaykkumar01.vaultspace.album.layout.BandLayoutEngine;
//import com.github.jaykkumar01.vaultspace.album.model.AlbumItem;
//import com.github.jaykkumar01.vaultspace.album.model.Band;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Comparator;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//
//public final class AlbumContentView extends FrameLayout {
//
//    private static final String TAG="VaultSpace:AlbumContent";
//
//    private static final float SOLO_WIDTH_FACTOR = 1.45f;
//
//    private static final SimpleDateFormat DF =
//            new SimpleDateFormat("dd MMM yyyy\nHH:mm:ss",Locale.getDefault());
//
//    private static final SimpleDateFormat MONTH_FMT =
//            new SimpleDateFormat("MMM yyyy",Locale.getDefault());
//
//    private FrameLayout canvas;
//    private String albumId;
//
//    public AlbumContentView(Context c){super(c);init(c);}
//    public AlbumContentView(Context c,@Nullable AttributeSet a){super(c,a);init(c);}
//    public AlbumContentView(Context c,@Nullable AttributeSet a,int s){super(c,a,s);init(c);}
//
//    private void init(Context c){
//        setBackgroundColor(c.getColor(R.color.vs_content_bg));
//        ScrollView scroll=new ScrollView(c);
//        canvas=new FrameLayout(c);
//        scroll.addView(canvas,new LayoutParams(MATCH_PARENT,WRAP_CONTENT));
//        addView(scroll,new LayoutParams(MATCH_PARENT,MATCH_PARENT));
//    }
//
//    /* ================= Public API ================= */
//
//    public void setAlbum(String albumId,String albumName){
//        this.albumId=albumId;
//        Log.d(TAG,"album="+albumId);
//    }
//
//    public void setMedia(Iterable<AlbumMedia> snapshot){
//        List<AlbumMedia> media=new ArrayList<>();
//        for(AlbumMedia m:snapshot) media.add(m);
//        Log.d(TAG,"mediaCount="+media.size());
//        rebuild(media);
//    }
//
//    /* ================= Pipeline ================= */
//
//    private void rebuild(List<AlbumMedia> media){
//        if(albumId==null) return;
//
//        media.sort(Comparator.comparingLong((AlbumMedia m)->m.momentMillis).reversed());
//
//        List<AlbumItem> items=new ArrayList<>(media.size());
//        for(int i=0;i<media.size();i++) items.add(new AlbumItem(media.get(i),i));
//
//        List<Band> bands=BandFormer.form(items);
//        List<BandLayout> layouts=BandLayoutEngine.layout(albumId,bands);
//
//        renderDebugFrames(bands,layouts);
//    }
//
//    /* ================= Rendering ================= */
//
//    private void renderDebugFrames(List<Band> bands,List<BandLayout> layouts){
//        canvas.removeAllViews();
//
//        final int screenW=getResources().getDisplayMetrics().widthPixels;
//        final int baseW=dp(140),hPad=dp(16),vPad=dp(16),labelH=dp(24);
//
//        int yCursor=0;
//
//        for(int bi=0;bi<bands.size();bi++){
//            Band band=bands.get(bi);
//            BandLayout layout=layouts.get(bi);
//            String label=resolveLabel(band.anchorMoment);
//
//            int count=band.items.size();
//            int frameW = baseW;
//
//            if(band.isSolo()){
//                float ar = band.items.get(0).media.aspectRatio;
//
//                if(ar > 1.3f){                 // wide
//                    frameW = Math.round(baseW * 1.5f);
//                }else if(ar < 0.7f){           // very tall
//                    frameW = Math.round(baseW * 0.85f);
//                }else if(ar < 0.9f){           // tall
//                    frameW = Math.round(baseW * 0.95f);
//                }
//            }
//
//
//            int[] h=new int[count];
//            int maxH=0;
//
//            for(int i=0;i<count;i++){
//                AlbumMedia m=band.items.get(i).media;
//                h[i]=Math.round(frameW/m.aspectRatio);
//                if(h[i]>maxH) maxH=h[i];
//            }
//
//            int bandH=maxH+vPad*2+labelH;
//
//            FrameLayout bandBox=new FrameLayout(getContext());
//            LayoutParams bandLp=new LayoutParams(MATCH_PARENT,bandH);
//            bandLp.topMargin=yCursor;
//            bandBox.setLayoutParams(bandLp);
//            bandBox.setBackground(bandBorder());
//
//            TextView tvLabel=new TextView(getContext());
//            tvLabel.setText(label);
//            tvLabel.setTextSize(12f);
//            tvLabel.setTextColor(0x99FFFFFF);
//            tvLabel.setPadding(dp(16),dp(4),dp(16),dp(4));
//            bandBox.addView(tvLabel);
//
//            if(count==1){
//                AlbumMedia m=band.items.get(0).media;
//                LayoutParams lp=new LayoutParams(frameW,h[0]);
//                lp.leftMargin=(screenW-frameW)/2;
//                lp.topMargin=labelH+vPad+(maxH-h[0])/2;
//
//                logFrame(bi,0,label,m,layout,
//                        lp.leftMargin,yCursor+lp.topMargin,frameW,h[0]);
//
//                View frame=makeFrame(m);
//                frame.setLayoutParams(lp);
//                applyLayout(frame,layout);
//                bandBox.addView(frame);
//
//            }else{
//                int available=screenW-hPad*3;
//                int colW=available/2;
//
//                for(int i=0;i<2;i++){
//                    AlbumMedia m=band.items.get(i).media;
//                    LayoutParams lp=new LayoutParams(baseW,h[i]);
//                    lp.leftMargin=hPad+i*(colW+hPad)+(colW-baseW)/2;
//                    lp.topMargin=labelH+vPad+(maxH-h[i])/2;
//
//                    logFrame(bi,i,label,m,layout,
//                            lp.leftMargin,yCursor+lp.topMargin,baseW,h[i]);
//
//                    View frame=makeFrame(m);
//                    frame.setLayoutParams(lp);
//                    applyLayout(frame,layout);
//                    bandBox.addView(frame);
//                }
//            }
//
//            canvas.addView(bandBox);
//            yCursor+=bandH;
//        }
//    }
//
//    /* ================= Logging ================= */
//
//    private void logFrame(
//            int band,int index,String label,
//            AlbumMedia m,BandLayout l,
//            int x,int y,int w,int h
//    ){
//        Log.d(TAG,
//                "F["+band+":"+index+"]"+
//                        " label="+label+
//                        " id="+shortId(m.fileId)+
//                        " x="+x+" y="+y+
//                        " w="+w+" h="+h+
//                        " ar="+String.format("%.2f",m.aspectRatio)+
//                        " ox="+String.format("%.2f",l.offsetX)+
//                        " oy="+String.format("%.2f",l.offsetY)+
//                        " rot="+String.format("%.2f",l.rotation)
//        );
//    }
//
//    private String shortId(String id){
//        return id.length()<=6?id:id.substring(0,6);
//    }
//
//    /* ================= Labels ================= */
//
//    private String resolveLabel(long t){
//        Calendar now=Calendar.getInstance();
//        Calendar c=Calendar.getInstance();
//        c.setTimeInMillis(t);
//
//        if(isSameDay(c,now)) return "Today";
//
//        Calendar y=(Calendar)now.clone();
//        y.add(Calendar.DAY_OF_YEAR,-1);
//        if(isSameDay(c,y)) return "Yesterday";
//
//        Calendar w=(Calendar)now.clone();
//        w.set(Calendar.DAY_OF_WEEK,w.getFirstDayOfWeek());
//        if(t>=w.getTimeInMillis()) return "This week";
//
//        Calendar m=(Calendar)now.clone();
//        m.set(Calendar.DAY_OF_MONTH,1);
//        if(t>=m.getTimeInMillis()) return "This month";
//
//        return MONTH_FMT.format(new Date(t));
//    }
//
//    private boolean isSameDay(Calendar a,Calendar b){
//        return a.get(Calendar.YEAR)==b.get(Calendar.YEAR) &&
//                a.get(Calendar.DAY_OF_YEAR)==b.get(Calendar.DAY_OF_YEAR);
//    }
//
//    /* ================= Helpers ================= */
//
//    private void applyLayout(View v,BandLayout l){
//        v.setTranslationX(l.offsetX);
//        v.setTranslationY(l.offsetY);
//        v.setRotation(l.rotation);
//    }
//
//    private View makeFrame(AlbumMedia m){
//        FrameLayout v=new FrameLayout(getContext());
//        v.setBackground(border(R.color.vs_media_highlight));
//
//        TextView tv=new TextView(getContext());
//        tv.setText(DF.format(new Date(m.momentMillis)));
//        tv.setTextSize(11f);
//        tv.setTextColor(0x88FFFFFF);
//        tv.setGravity(Gravity.CENTER);
//
//        v.addView(tv,new LayoutParams(MATCH_PARENT,MATCH_PARENT));
//        return v;
//    }
//
//    private GradientDrawable border(int color){
//        GradientDrawable d=new GradientDrawable();
//        d.setColor(0x00000000);
//        d.setCornerRadius(dp(10));
//        d.setStroke(dp(1),getContext().getColor(color));
//        return d;
//    }
//
//    private GradientDrawable bandBorder(){
//        GradientDrawable d=new GradientDrawable();
//        d.setColor(0x00000000);
//        d.setCornerRadius(dp(12));
//        d.setStroke(dp(1),0x22FFFFFF);
//        return d;
//    }
//
//    private int dp(int v){
//        return Math.round(v*getResources().getDisplayMetrics().density);
//    }
//}  package com.github.jaykkumar01.vaultspace.album.band;
//
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.List;
//import java.util.Locale;
//
//public final class TimeBucketizer {
//
//    private TimeBucketizer(){}
//
//    public static List<TimeBucket> buildBuckets(long now){
//        List<TimeBucket> out=new ArrayList<>();
//
//        Calendar c=Calendar.getInstance();
//        c.setTimeInMillis(now);
//
//        // TODAY
//        long todayStart=startOfDay(c);
//        long todayEnd=endOfDay(c);
//        out.add(new TimeBucket(
//                TimeBucketType.TODAY,
//                "today",
//                todayStart,
//                todayEnd
//        ));
//
//        // YESTERDAY
//        c.add(Calendar.DAY_OF_YEAR,-1);
//        out.add(new TimeBucket(
//                TimeBucketType.YESTERDAY,
//                "yesterday",
//                startOfDay(c),
//                endOfDay(c)
//        ));
//
//        // THIS WEEK
//        c.setTimeInMillis(now);
//        c.set(Calendar.DAY_OF_WEEK,c.getFirstDayOfWeek());
//        long weekStart=startOfDay(c);
//        c.add(Calendar.DAY_OF_WEEK,6);
//        long weekEnd=endOfDay(c);
//        out.add(new TimeBucket(
//                TimeBucketType.THIS_WEEK,
//                "this_week",
//                weekStart,
//                weekEnd
//        ));
//
//        // THIS MONTH
//        c.setTimeInMillis(now);
//        c.set(Calendar.DAY_OF_MONTH,1);
//        long monthStart=startOfDay(c);
//        c.add(Calendar.MONTH,1);
//        c.add(Calendar.MILLISECOND,-1);
//        long monthEnd=c.getTimeInMillis();
//        out.add(new TimeBucket(
//                TimeBucketType.THIS_MONTH,
//                "this_month",
//                monthStart,
//                monthEnd
//        ));
//
//        return out;
//    }
//
//    private static long startOfDay(Calendar c){
//        c.set(Calendar.HOUR_OF_DAY,0);
//        c.set(Calendar.MINUTE,0);
//        c.set(Calendar.SECOND,0);
//        c.set(Calendar.MILLISECOND,0);
//        return c.getTimeInMillis();
//    }
//
//    private static long endOfDay(Calendar c){
//        c.set(Calendar.HOUR_OF_DAY,23);
//        c.set(Calendar.MINUTE,59);
//        c.set(Calendar.SECOND,59);
//        c.set(Calendar.MILLISECOND,999);
//        return c.getTimeInMillis();
//    }
//}  package com.github.jaykkumar01.vaultspace.album.band;
//
//public final class TimeBucket {
//
//    public final TimeBucketType type;
//    public final String key;      // e.g. "2026-01" for MONTH
//    public final long startMillis;
//    public final long endMillis;
//
//    public TimeBucket(
//            TimeBucketType type,
//            String key,
//            long startMillis,
//            long endMillis
//    ){
//        this.type=type;
//        this.key=key;
//        this.startMillis=startMillis;
//        this.endMillis=endMillis;
//    }
//
//    public boolean contains(long t){
//        return t>=startMillis && t<=endMillis;
//    }
//}  package com.github.jaykkumar01.vaultspace.album.band;
//
//public final class BandRules {
//
//    private BandRules(){}
//
//    // Hard blockers
//    public static final float EXTREME_WIDE = 1.6f;
//    public static final float ASPECT_MISMATCH = 2.2f;
//
//    // Similarity
//    public static final float SIMILAR_AR_DELTA = 0.35f;
//
//    // Shape classes
//    public static final float VERY_TALL = 0.7f; // vertical dominant
//    public static final float WIDE = 1.3f;      // horizontal dominant
//}
//  package com.github.jaykkumar01.vaultspace.album.band;
//
//import com.github.jaykkumar01.vaultspace.album.model.AlbumItem;
//import com.github.jaykkumar01.vaultspace.album.model.Band;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public final class BandFormer {
//
//    private BandFormer(){}
//
//    public static List<Band> form(List<AlbumItem> items){
//        List<Band> out=new ArrayList<>();
//        if(items.isEmpty()) return out;
//
//        long now=System.currentTimeMillis();
//        List<TimeBucket> buckets=TimeBucketizer.buildBuckets(now);
//
//        int bandIndex=0,cursor=0;
//
//        // semantic buckets
//        for(TimeBucket b:buckets){
//            List<AlbumItem> slice=new ArrayList<>();
//            while(cursor<items.size()){
//                AlbumItem it=items.get(cursor);
//                if(!b.contains(it.media.momentMillis)) break;
//                slice.add(it);
//                cursor++;
//            }
//            bandIndex=formInside(slice,out,bandIndex);
//        }
//
//        // remaining → month-wise
//        bandIndex=formMonthWise(items,cursor,out,bandIndex);
//        return out;
//    }
//
//    /* ================= Internal ================= */
//
//    private static int formInside(List<AlbumItem> items,List<Band> out,int bandIndex){
//        int i=0;
//        while(i<items.size()){
//            AlbumItem a=items.get(i);
//            if(i+1<items.size()){
//                AlbumItem b=items.get(i+1);
//                if(shouldPair(a,b)){
//                    out.add(new Band(bandIndex++,List.of(a,b)));
//                    i+=2; continue;
//                }
//            }
//            out.add(new Band(bandIndex++,List.of(a)));
//            i++;
//        }
//        return bandIndex;
//    }
//
//    private static int formMonthWise(List<AlbumItem> items,int start,List<Band> out,int bandIndex){
//        int i=start;
//        while(i<items.size()){
//            long key=monthKey(items.get(i).media.momentMillis);
//            List<AlbumItem> bucket=new ArrayList<>();
//            while(i<items.size()&&monthKey(items.get(i).media.momentMillis)==key)
//                bucket.add(items.get(i++));
//            bandIndex=formInside(bucket,out,bandIndex);
//        }
//        return bandIndex;
//    }
//
//    private static long monthKey(long t){
//        java.util.Calendar c=java.util.Calendar.getInstance();
//        c.setTimeInMillis(t);
//        return c.get(java.util.Calendar.YEAR)*100L+
//                c.get(java.util.Calendar.MONTH);
//    }
//
//    /* ================= Pairing (VISUAL ONLY) ================= */
//
//    private static boolean shouldPair(AlbumItem a,AlbumItem b){
//        if(hasHardBlocker(a,b)) return false;
//        return isVisuallyCompatible(a,b);
//    }
//
//    private static boolean hasHardBlocker(AlbumItem a,AlbumItem b){
//        float arA=a.media.aspectRatio,arB=b.media.aspectRatio;
//        if(arA>=BandRules.EXTREME_WIDE||arB>=BandRules.EXTREME_WIDE) return true;
//        float ratio=Math.max(arA,arB)/Math.min(arA,arB);
//        return ratio>=BandRules.ASPECT_MISMATCH;
//    }
//
//    private static boolean isVisuallyCompatible(AlbumItem a,AlbumItem b){
//        float arA=a.media.aspectRatio,arB=b.media.aspectRatio;
//        Shape sa=shapeOf(arA),sb=shapeOf(arB);
//        if(sa==sb) return true;
//        if(sa==Shape.NEUTRAL||sb==Shape.NEUTRAL)
//            return Math.abs(arA-arB)<=BandRules.SIMILAR_AR_DELTA;
//        return false; // tall + wide
//    }
//
//    private enum Shape{TALL,NEUTRAL,WIDE}
//
//    private static Shape shapeOf(float ar){
//        if(ar<BandRules.VERY_TALL) return Shape.TALL;
//        if(ar>BandRules.WIDE) return Shape.WIDE;
//        return Shape.NEUTRAL;
//    }
//}  package com.github.jaykkumar01.vaultspace.album.layout;
//
//public final class BandLayout {
//
//    public final int bandIndex;
//    public final float height;
//    public final float offsetX;
//    public final float offsetY;
//    public final float rotation;
//
//    public BandLayout(
//            int bandIndex,
//            float height,
//            float offsetX,
//            float offsetY,
//            float rotation
//    ){
//        this.bandIndex=bandIndex;
//        this.height=height;
//        this.offsetX=offsetX;
//        this.offsetY=offsetY;
//        this.rotation=rotation;
//    }
//}  package com.github.jaykkumar01.vaultspace.album.layout;
//
//import com.github.jaykkumar01.vaultspace.album.model.AlbumItem;
//import com.github.jaykkumar01.vaultspace.album.model.Band;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public final class BandLayoutEngine {
//
//    private static final float BASE_HEIGHT = 60f;
//    private static final float PAIR_FACTOR = 1.05f;
//
//    private static final float MAX_X = 5f;
//    private static final float MAX_Y = 3f;
//    private static final float MAX_ROT = 2f;
//
//    private static final float STEP = 0.21f;
//
//    private BandLayoutEngine(){}
//
//    public static List<BandLayout> layout(String albumId,List<Band> bands){
//        List<BandLayout> out=new ArrayList<>(bands.size());
//        float seed=seed(albumId);
//
//        for(Band b:bands){
//            float h=computeHeight(b);
//            float t=seed+b.bandIndex*STEP;
//
//            float ox=noise(t)*MAX_X;
//            float oy=noise(t+100f)*MAX_Y;
//            float r=noise(t+200f)*MAX_ROT;
//
//            out.add(new BandLayout(b.bandIndex,h,ox,oy,r));
//        }
//        return out;
//    }
//
//    /* ================= Geometry ================= */
//
//    private static float computeHeight(Band b){
//        float dominant=0f;
//        for(AlbumItem i:b.items){
//            dominant=Math.max(dominant,aspectWeight(i.media.aspectRatio));
//        }
//        float h=BASE_HEIGHT*dominant;
//        if(!b.isSolo()) h*=PAIR_FACTOR;
//        return h;
//    }
//
//    private static float aspectWeight(float ar){
//        if(ar<0.7f) return 1.25f;   // very tall → needs air
//        if(ar<=1.2f) return 1.0f;   // neutral
//        if(ar<=1.6f) return 0.9f;   // wide
//        return 0.8f;                // extreme wide
//    }
//
//    /* ================= Deterministic noise ================= */
//
//    private static float seed(String s){
//        int h=0;
//        for(int i=0;i<s.length();i++) h=31*h+s.charAt(i);
//        return (h&0xFFFF)/1000f;
//    }
//
//    // lightweight 1D Perlin-like noise
//    private static float noise(float x){
//        int xi=(int)Math.floor(x)&255;
//        float xf=x-(int)Math.floor(x);
//        float u=xf*xf*xf*(xf*(xf*6-15)+10);
//        int a=PERM[xi],b=PERM[xi+1];
//        return lerp(grad(a,xf),grad(b,xf-1),u);
//    }
//
//    private static float lerp(float a,float b,float t){
//        return a+t*(b-a);
//    }
//
//    private static float grad(int h,float x){
//        return ((h&1)==0)?x:-x;
//    }
//
//    /* ================= Permutation table ================= */
//
//    private static final int[] PERM=new int[512];
//
//    static{
//        int[] p={
//                151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,
//                140,36,103,30,69,142,8,99,37,240,21,10,23,
//                190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,
//                35,11,32,57,177,33,88,237,149,56,87,174,20,125,136,171,
//                168,68,175,74,165,71,134,139,48,27,166,77,146,158,231,83,
//                111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
//                102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,
//                89,18,169,200,196,135,130,116,188,159,86,164,100,109,198,173,
//                186,3,64,52,217,226,250,124,123,5,202,38,147,118,126,255,
//                82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,223,
//                183,170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,
//                43,172,9,129,22,39,253,19,98,108,110,79,113,224,232,178,
//                185,112,104,218,246,97,228,251,34,242,193,238,210,144,12,191,
//                179,162,241,81,51,145,235,249,14,239,107,49,192,214,31,181,
//                199,106,157,184,84,204,176,115,121,50,45,127,4,150,254,138,
//                236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215
//        };
//        for(int i=0;i<512;i++) PERM[i]=p[i%p.length];
//    }
//}  okay now before touching code first let's discuss currently what we are getting and what we are setting in view