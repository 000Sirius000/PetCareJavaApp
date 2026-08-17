package com.example.petcare.ui.common;

public final class ChartStats {
    private ChartStats() {}

    public static double averagePositive(double[] bucketTotals) {
        if (bucketTotals == null || bucketTotals.length == 0) return 0d;
        double total = 0d;
        int filled = 0;
        for (double value : bucketTotals) {
            total += Math.max(0d, value);
            if (value > 0d) filled++;
        }
        return filled == 0 ? 0d : total / filled;
    }

    public static double averageFilled(double[] bucketTotals, boolean[] filledBuckets) {
        if (bucketTotals == null || filledBuckets == null) return 0d;
        int count = Math.min(bucketTotals.length, filledBuckets.length);
        double total = 0d;
        int filled = 0;
        for (int index = 0; index < count; index++) {
            if (!filledBuckets[index]) continue;
            total += Math.max(0d, bucketTotals[index]);
            filled++;
        }
        return filled == 0 ? 0d : total / filled;
    }
}
