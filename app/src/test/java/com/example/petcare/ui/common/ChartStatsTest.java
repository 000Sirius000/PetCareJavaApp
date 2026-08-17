package com.example.petcare.ui.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChartStatsTest {
    @Test
    public void weekAverageUsesOnlyFiveFilledDays() {
        double[] minutes = {60d, 120d, 0d, 180d, 60d, 0d, 80d};
        assertEquals(100d, ChartStats.averagePositive(minutes), 0.0001d);
    }

    @Test
    public void monthAndYearIgnoreEmptyBuckets() {
        assertEquals(15d, ChartStats.averagePositive(new double[]{10d, 0d, 20d, 0d}), 0.0001d);
        assertEquals(0d, ChartStats.averagePositive(new double[12]), 0.0001d);
    }

    @Test
    public void distanceUsesItsOwnFilledBucketFlags() {
        double[] distance = {2d, 0d, 3d, 0d};
        boolean[] supplied = {true, true, false, false};
        assertEquals(1d, ChartStats.averageFilled(distance, supplied), 0.0001d);
    }
}
