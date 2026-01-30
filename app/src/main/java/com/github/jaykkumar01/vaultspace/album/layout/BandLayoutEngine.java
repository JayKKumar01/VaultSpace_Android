package com.github.jaykkumar01.vaultspace.album.layout;

import com.github.jaykkumar01.vaultspace.album.model.AlbumItem;
import com.github.jaykkumar01.vaultspace.album.model.Band;

import java.util.ArrayList;
import java.util.List;

public final class BandLayoutEngine {

    private static final float BASE_HEIGHT = 60f;
    private static final float PAIR_FACTOR = 1.05f;

    private static final float MAX_X = 5f;
    private static final float MAX_Y = 3f;
    private static final float MAX_ROT = 2f;

    private static final float STEP = 0.21f;

    private BandLayoutEngine(){}

    public static List<BandLayout> layout(String albumId,List<Band> bands){
        List<BandLayout> out=new ArrayList<>(bands.size());
        float seed=seed(albumId);

        for(Band b:bands){
            float h=computeHeight(b);
            float t=seed+b.bandIndex*STEP;

            float ox=noise(t)*MAX_X;
            float oy=noise(t+100f)*MAX_Y;
            float r=noise(t+200f)*MAX_ROT;

            out.add(new BandLayout(b.bandIndex,h,ox,oy,r));
        }
        return out;
    }

    /* ================= Geometry ================= */

    private static float computeHeight(Band b){
        float dominant=0f;
        for(AlbumItem i:b.items){
            dominant=Math.max(dominant,aspectWeight(i.media.aspectRatio));
        }
        float h=BASE_HEIGHT*dominant;
        if(!b.isSolo()) h*=PAIR_FACTOR;
        return h;
    }

    private static float aspectWeight(float ar){
        if(ar<0.7f) return 1.25f;   // very tall → needs air
        if(ar<=1.2f) return 1.0f;   // neutral
        if(ar<=1.6f) return 0.9f;   // wide
        return 0.8f;                // extreme wide
    }

    /* ================= Deterministic noise ================= */

    private static float seed(String s){
        int h=0;
        for(int i=0;i<s.length();i++) h=31*h+s.charAt(i);
        return (h&0xFFFF)/1000f;
    }

    // lightweight 1D Perlin-like noise
    private static float noise(float x){
        int xi=(int)Math.floor(x)&255;
        float xf=x-(int)Math.floor(x);
        float u=xf*xf*xf*(xf*(xf*6-15)+10);
        int a=PERM[xi],b=PERM[xi+1];
        return lerp(grad(a,xf),grad(b,xf-1),u);
    }

    private static float lerp(float a,float b,float t){
        return a+t*(b-a);
    }

    private static float grad(int h,float x){
        return ((h&1)==0)?x:-x;
    }

    /* ================= Permutation table ================= */

    private static final int[] PERM=new int[512];

    static{
        int[] p={
                151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,
                140,36,103,30,69,142,8,99,37,240,21,10,23,
                190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,
                35,11,32,57,177,33,88,237,149,56,87,174,20,125,136,171,
                168,68,175,74,165,71,134,139,48,27,166,77,146,158,231,83,
                111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
                102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,
                89,18,169,200,196,135,130,116,188,159,86,164,100,109,198,173,
                186,3,64,52,217,226,250,124,123,5,202,38,147,118,126,255,
                82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,223,
                183,170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,
                43,172,9,129,22,39,253,19,98,108,110,79,113,224,232,178,
                185,112,104,218,246,97,228,251,34,242,193,238,210,144,12,191,
                179,162,241,81,51,145,235,249,14,239,107,49,192,214,31,181,
                199,106,157,184,84,204,176,115,121,50,45,127,4,150,254,138,
                236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215
        };
        for(int i=0;i<512;i++) PERM[i]=p[i%p.length];
    }
}
