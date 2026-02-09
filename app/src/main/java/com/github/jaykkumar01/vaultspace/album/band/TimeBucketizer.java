package com.github.jaykkumar01.vaultspace.album.band;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class TimeBucketizer {

    /* ================= Bucket keys ================= */

    public static final String KEY_TODAY = "today";
    public static final String KEY_YESTERDAY = "yesterday";
    public static final String KEY_THIS_WEEK = "this_week";
    public static final String KEY_THIS_MONTH = "this_month";

    /* ================= Bucket labels ================= */

    public static final String LABEL_TODAY = "Today";
    public static final String LABEL_YESTERDAY = "Yesterday";
    public static final String LABEL_THIS_WEEK = "This Week";
    public static final String LABEL_THIS_MONTH = "This Month";
    public static final String TIME = "Time: ";

    /* ================= Result ================= */

    public static final class Result {
        public final String key, label;
        Result(String key, String label) { this.key = key; this.label = label; }
    }

    /* ================= Month formats ================= */

    private static final SimpleDateFormat MONTH_KEY =
            new SimpleDateFormat("yyyy-MM", Locale.US);
    private static final SimpleDateFormat MONTH_LABEL =
            new SimpleDateFormat("MMM yyyy", Locale.US);

    /* ================= Time windows ================= */

    private final long todayS, todayE;
    private final long yestS, yestE;
    private final long weekS, weekE;
    private final long monthS, monthE;

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

    /* ================= Stable resolve ================= */

    public Result resolve(long ms) {

        if (ms >= todayS && ms <= todayE)
            return new Result(KEY_TODAY, LABEL_TODAY);

        if (ms >= yestS && ms <= yestE)
            return new Result(KEY_YESTERDAY, LABEL_YESTERDAY);

        if (ms >= weekS && ms <= weekE && ms < yestS)
            return new Result(KEY_THIS_WEEK, LABEL_THIS_WEEK);

        if (ms >= monthS && ms <= monthE && ms < weekS)
            return new Result(KEY_THIS_MONTH, LABEL_THIS_MONTH);

        return new Result(
                MONTH_KEY.format(ms),
                MONTH_LABEL.format(ms)
        );
    }

    /* ================= Time helpers ================= */

    private static long startOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static long endOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    /* ================= Formatting helper ================= */

    public static String formatTimeForKey(Context ctx, String timeLabel, long millis) {
        boolean is24h = DateFormat.is24HourFormat(ctx);

        SimpleDateFormat timeFmt = new SimpleDateFormat(
                is24h ? "HH:mm" : "hh:mm a", Locale.getDefault());

        SimpleDateFormat dateTimeFmt = new SimpleDateFormat(
                is24h ? "dd MMM, HH:mm" : "dd MMM, hh:mm a", Locale.getDefault());

        return switch (timeLabel) {
            case LABEL_TODAY, LABEL_YESTERDAY -> TIME + timeFmt.format(millis);
            default -> dateTimeFmt.format(millis);
        };
    }
}
