package com.github.jaykkumar01.vaultspace.album.band;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class TimeBucketizer {

    public static final class Result {
        public final String key;
        public final String label;
        Result(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }

    private static final SimpleDateFormat MONTH_KEY =
            new SimpleDateFormat("yyyy-MM", Locale.US);
    private static final SimpleDateFormat MONTH_LABEL =
            new SimpleDateFormat("MMM yyyy", Locale.US);

    private final long todayS,todayE;
    private final long yestS,yestE;
    private final long weekS,weekE;
    private final long monthS,monthE;

    private TimeBucketizer(long now) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);

        todayS = startOfDay(c); todayE = endOfDay(c);

        c.add(Calendar.DAY_OF_YEAR, -1);
        yestS = startOfDay(c); yestE = endOfDay(c);

        c.setTimeInMillis(now);
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        weekS = startOfDay(c);
        c.add(Calendar.DAY_OF_WEEK, 6);
        weekE = endOfDay(c);

        c.setTimeInMillis(now);
        c.set(Calendar.DAY_OF_MONTH, 1);
        monthS = startOfDay(c);
        c.add(Calendar.MONTH, 1);
        c.add(Calendar.MILLISECOND, -1);
        monthE = c.getTimeInMillis();
    }

    public static TimeBucketizer create(long now) {
        return new TimeBucketizer(now);
    }

    /* ===== SINGLE ENTRY POINT ===== */

    public Result resolve(long ms) {
        if (ms >= todayS && ms <= todayE)
            return new Result("today", "Today");

        if (ms >= yestS && ms <= yestE)
            return new Result("yesterday", "Yesterday");

        if (ms >= weekS && ms <= weekE)
            return new Result("this_week", "This Week");

        if (ms >= monthS && ms <= monthE)
            return new Result("this_month", "This Month");

        return new Result(
                MONTH_KEY.format(ms),
                MONTH_LABEL.format(ms)
        );
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
