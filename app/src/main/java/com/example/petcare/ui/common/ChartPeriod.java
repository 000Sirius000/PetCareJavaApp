package com.example.petcare.ui.common;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Calendar bounds and buckets shared by all statistic charts. */
public final class ChartPeriod {
    public static final class Bucket {
        public final long startMillis;
        public final long endMillis;
        public final String label;

        private Bucket(long startMillis, long endMillis, String label) {
            this.startMillis = startMillis;
            this.endMillis = endMillis;
            this.label = label;
        }

        public boolean contains(long timestamp) {
            return timestamp >= startMillis && timestamp <= endMillis;
        }
    }

    public final FilterRange range;
    public final long startMillis;
    public final long endMillis;
    public final String label;
    public final List<Bucket> buckets;

    private ChartPeriod(FilterRange range, long startMillis, long endMillis,
                        String label, List<Bucket> buckets) {
        this.range = range;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.label = label;
        this.buckets = Collections.unmodifiableList(buckets);
    }

    public static ChartPeriod of(FilterRange range, long anchorMillis) {
        FilterRange safeRange = range == null ? FilterRange.WEEK : range;
        Calendar start = startCalendar(safeRange, anchorMillis);
        Calendar next = (Calendar) start.clone();
        addPeriod(next, safeRange, 1);
        long endMillis = next.getTimeInMillis() - 1L;
        return new ChartPeriod(
                safeRange,
                start.getTimeInMillis(),
                endMillis,
                formatLabel(safeRange, start, endMillis),
                buildBuckets(safeRange, start)
        );
    }

    public ChartPeriod shift(int amount) {
        Calendar shifted = Calendar.getInstance();
        shifted.setTimeInMillis(startMillis);
        addPeriod(shifted, range, amount);
        return of(range, shifted.getTimeInMillis());
    }

    public boolean contains(long timestamp) {
        return timestamp >= startMillis && timestamp <= endMillis;
    }

    public int bucketIndex(long timestamp) {
        for (int index = 0; index < buckets.size(); index++) {
            if (buckets.get(index).contains(timestamp)) return index;
        }
        return -1;
    }

    public static long periodStart(FilterRange range, long timestamp) {
        return startCalendar(range == null ? FilterRange.WEEK : range, timestamp).getTimeInMillis();
    }

    private static Calendar startCalendar(FilterRange range, long anchorMillis) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(anchorMillis > 0L ? anchorMillis : System.currentTimeMillis());
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        if (range == FilterRange.YEAR) {
            start.set(Calendar.MONTH, Calendar.JANUARY);
            start.set(Calendar.DAY_OF_MONTH, 1);
        } else if (range == FilterRange.MONTH) {
            start.set(Calendar.DAY_OF_MONTH, 1);
        } else {
            int daysSinceMonday = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            start.add(Calendar.DAY_OF_YEAR, -daysSinceMonday);
        }
        return start;
    }

    private static void addPeriod(Calendar calendar, FilterRange range, int amount) {
        if (range == FilterRange.YEAR) calendar.add(Calendar.YEAR, amount);
        else if (range == FilterRange.MONTH) calendar.add(Calendar.MONTH, amount);
        else calendar.add(Calendar.WEEK_OF_YEAR, amount);
    }

    private static List<Bucket> buildBuckets(FilterRange range, Calendar periodStart) {
        List<Bucket> result = new ArrayList<>();
        if (range == FilterRange.YEAR) {
            String[] months = new DateFormatSymbols(Locale.getDefault()).getShortMonths();
            for (int month = 0; month < 12; month++) {
                Calendar start = (Calendar) periodStart.clone();
                start.set(Calendar.MONTH, month);
                Calendar next = (Calendar) start.clone();
                next.add(Calendar.MONTH, 1);
                result.add(new Bucket(start.getTimeInMillis(), next.getTimeInMillis() - 1L, months[month]));
            }
            return result;
        }

        int count = range == FilterRange.MONTH
                ? periodStart.getActualMaximum(Calendar.DAY_OF_MONTH)
                : 7;
        SimpleDateFormat weekLabel = new SimpleDateFormat("EEE d", Locale.getDefault());
        for (int index = 0; index < count; index++) {
            Calendar start = (Calendar) periodStart.clone();
            start.add(Calendar.DAY_OF_YEAR, index);
            Calendar next = (Calendar) start.clone();
            next.add(Calendar.DAY_OF_YEAR, 1);
            String label = range == FilterRange.MONTH
                    ? String.valueOf(index + 1)
                    : weekLabel.format(start.getTime());
            result.add(new Bucket(start.getTimeInMillis(), next.getTimeInMillis() - 1L, label));
        }
        return result;
    }

    private static String formatLabel(FilterRange range, Calendar start, long endMillis) {
        if (range == FilterRange.YEAR) {
            return new SimpleDateFormat("yyyy", Locale.getDefault()).format(start.getTime());
        }
        if (range == FilterRange.MONTH) {
            return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(start.getTime());
        }
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(endMillis);
        SimpleDateFormat first = new SimpleDateFormat("d MMM", Locale.getDefault());
        SimpleDateFormat last = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        return first.format(start.getTime()) + " - " + last.format(end.getTime());
    }
}
