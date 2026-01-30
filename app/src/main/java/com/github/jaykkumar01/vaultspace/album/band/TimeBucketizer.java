package com.github.jaykkumar01.vaultspace.album.band;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class TimeBucketizer {

    private TimeBucketizer(){}

    public static List<TimeBucket> buildBuckets(long now){
        List<TimeBucket> out=new ArrayList<>();

        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(now);

        // TODAY
        long todayStart=startOfDay(c);
        long todayEnd=endOfDay(c);
        out.add(new TimeBucket(
                TimeBucketType.TODAY,
                "today",
                todayStart,
                todayEnd
        ));

        // YESTERDAY
        c.add(Calendar.DAY_OF_YEAR,-1);
        out.add(new TimeBucket(
                TimeBucketType.YESTERDAY,
                "yesterday",
                startOfDay(c),
                endOfDay(c)
        ));

        // THIS WEEK
        c.setTimeInMillis(now);
        c.set(Calendar.DAY_OF_WEEK,c.getFirstDayOfWeek());
        long weekStart=startOfDay(c);
        c.add(Calendar.DAY_OF_WEEK,6);
        long weekEnd=endOfDay(c);
        out.add(new TimeBucket(
                TimeBucketType.THIS_WEEK,
                "this_week",
                weekStart,
                weekEnd
        ));

        // THIS MONTH
        c.setTimeInMillis(now);
        c.set(Calendar.DAY_OF_MONTH,1);
        long monthStart=startOfDay(c);
        c.add(Calendar.MONTH,1);
        c.add(Calendar.MILLISECOND,-1);
        long monthEnd=c.getTimeInMillis();
        out.add(new TimeBucket(
                TimeBucketType.THIS_MONTH,
                "this_month",
                monthStart,
                monthEnd
        ));

        return out;
    }

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
