package com.github.jaykkumar01.vaultspace.album.band;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class TimeBucketizer {

    public enum Bucket {
        TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, MONTH
    }

    private static final SimpleDateFormat MONTH_KEY =
            new SimpleDateFormat("yyyy-MM", Locale.US);
    private static final SimpleDateFormat MONTH_LABEL =
            new SimpleDateFormat("MMM yyyy", Locale.US);

    private final long todayS,todayE;
    private final long yestS,yestE;
    private final long weekS,weekE;
    private final long monthS,monthE;

    private TimeBucketizer(long now){
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(now);

        todayS=startOfDay(c); todayE=endOfDay(c);

        c.add(Calendar.DAY_OF_YEAR,-1);
        yestS=startOfDay(c); yestE=endOfDay(c);

        c.setTimeInMillis(now);
        c.set(Calendar.DAY_OF_WEEK,c.getFirstDayOfWeek());
        weekS=startOfDay(c);
        c.add(Calendar.DAY_OF_WEEK,6);
        weekE=endOfDay(c);

        c.setTimeInMillis(now);
        c.set(Calendar.DAY_OF_MONTH,1);
        monthS=startOfDay(c);
        c.add(Calendar.MONTH,1);
        c.add(Calendar.MILLISECOND,-1);
        monthE=c.getTimeInMillis();
    }

    /* ===== Factory ===== */

    public static TimeBucketizer create(long now){
        return new TimeBucketizer(now);
    }

    /* ===== Classification ===== */

    public Bucket bucketOf(long ms){
        if(ms>=todayS && ms<=todayE) return Bucket.TODAY;
        if(ms>=yestS  && ms<=yestE)  return Bucket.YESTERDAY;
        if(ms>=weekS  && ms<=weekE)  return Bucket.THIS_WEEK;
        if(ms>=monthS && ms<=monthE) return Bucket.THIS_MONTH;
        return Bucket.MONTH;
    }

    public String keyOf(Bucket b,long ms){
        return b==Bucket.MONTH ? MONTH_KEY.format(ms) : b.name().toLowerCase();
    }

    public String labelOf(Bucket b,long ms){
        return switch(b){
            case TODAY->"Today";
            case YESTERDAY->"Yesterday";
            case THIS_WEEK->"This Week";
            case THIS_MONTH->"This Month";
            default->MONTH_LABEL.format(ms);
        };
    }

    /* ===== Utils ===== */

    private static long startOfDay(Calendar c){
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }

    private static long endOfDay(Calendar c){
        c.set(Calendar.HOUR_OF_DAY,23);
        c.set(Calendar.MINUTE,59);
        c.set(Calendar.SECOND,59);
        c.set(Calendar.MILLISECOND,999);
        return c.getTimeInMillis();
    }
}
