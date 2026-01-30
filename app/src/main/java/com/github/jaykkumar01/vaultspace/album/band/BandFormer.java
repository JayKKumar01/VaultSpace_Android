package com.github.jaykkumar01.vaultspace.album.band;

import com.github.jaykkumar01.vaultspace.album.model.AlbumItem;
import com.github.jaykkumar01.vaultspace.album.model.Band;

import java.util.ArrayList;
import java.util.List;

public final class BandFormer {

    private BandFormer(){}

    public static List<Band> form(List<AlbumItem> items){
        List<Band> out=new ArrayList<>();
        if(items.isEmpty()) return out;

        long now=System.currentTimeMillis();
        List<TimeBucket> buckets=TimeBucketizer.buildBuckets(now);

        int bandIndex=0,cursor=0;

        // semantic buckets
        for(TimeBucket b:buckets){
            List<AlbumItem> slice=new ArrayList<>();
            while(cursor<items.size()){
                AlbumItem it=items.get(cursor);
                if(!b.contains(it.media.momentMillis)) break;
                slice.add(it);
                cursor++;
            }
            bandIndex=formInside(slice,out,bandIndex);
        }

        // remaining → month-wise
        bandIndex=formMonthWise(items,cursor,out,bandIndex);
        return out;
    }

    /* ================= Internal ================= */

    private static int formInside(List<AlbumItem> items,List<Band> out,int bandIndex){
        int i=0;
        while(i<items.size()){
            AlbumItem a=items.get(i);
            if(i+1<items.size()){
                AlbumItem b=items.get(i+1);
                if(shouldPair(a,b)){
                    out.add(new Band(bandIndex++,List.of(a,b)));
                    i+=2; continue;
                }
            }
            out.add(new Band(bandIndex++,List.of(a)));
            i++;
        }
        return bandIndex;
    }

    private static int formMonthWise(List<AlbumItem> items,int start,List<Band> out,int bandIndex){
        int i=start;
        while(i<items.size()){
            long key=monthKey(items.get(i).media.momentMillis);
            List<AlbumItem> bucket=new ArrayList<>();
            while(i<items.size()&&monthKey(items.get(i).media.momentMillis)==key)
                bucket.add(items.get(i++));
            bandIndex=formInside(bucket,out,bandIndex);
        }
        return bandIndex;
    }

    private static long monthKey(long t){
        java.util.Calendar c=java.util.Calendar.getInstance();
        c.setTimeInMillis(t);
        return c.get(java.util.Calendar.YEAR)*100L+
                c.get(java.util.Calendar.MONTH);
    }

    /* ================= Pairing (VISUAL ONLY) ================= */

    private static boolean shouldPair(AlbumItem a,AlbumItem b){
        if(hasHardBlocker(a,b)) return false;
        return isVisuallyCompatible(a,b);
    }

    private static boolean hasHardBlocker(AlbumItem a,AlbumItem b){
        float arA=a.media.aspectRatio,arB=b.media.aspectRatio;
        if(arA>=BandRules.EXTREME_WIDE||arB>=BandRules.EXTREME_WIDE) return true;
        float ratio=Math.max(arA,arB)/Math.min(arA,arB);
        return ratio>=BandRules.ASPECT_MISMATCH;
    }

    private static boolean isVisuallyCompatible(AlbumItem a,AlbumItem b){
        float arA=a.media.aspectRatio,arB=b.media.aspectRatio;
        Shape sa=shapeOf(arA),sb=shapeOf(arB);
        if(sa==sb) return true;
        if(sa==Shape.NEUTRAL||sb==Shape.NEUTRAL)
            return Math.abs(arA-arB)<=BandRules.SIMILAR_AR_DELTA;
        return false; // tall + wide
    }

    private enum Shape{TALL,NEUTRAL,WIDE}

    private static Shape shapeOf(float ar){
        if(ar<BandRules.VERY_TALL) return Shape.TALL;
        if(ar>BandRules.WIDE) return Shape.WIDE;
        return Shape.NEUTRAL;
    }
}
