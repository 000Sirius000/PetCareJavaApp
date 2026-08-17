package com.example.petcare.ui.common;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class ChartPeriodTest {
    private TimeZone previousTimeZone;

    @Before
    public void useUtc() {
        previousTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void restoreTimeZone() {
        TimeZone.setDefault(previousTimeZone);
    }

    @Test
    public void weekRunsMondayThroughSunday() {
        ChartPeriod period = ChartPeriod.of(FilterRange.WEEK, timestamp(2024, Calendar.MAY, 15, 12, 0));
        assertEquals(timestamp(2024, Calendar.MAY, 13, 0, 0), period.startMillis);
        assertEquals(7, period.buckets.size());
        assertEquals(timestamp(2024, Calendar.MAY, 20, 0, 0) - 1L, period.endMillis);
    }

    @Test
    public void shiftingMonthKeepsCalendarBounds() {
        ChartPeriod february = ChartPeriod.of(FilterRange.MONTH, timestamp(2024, Calendar.FEBRUARY, 20, 12, 0));
        assertEquals(29, february.buckets.size());
        ChartPeriod march = february.shift(1);
        assertEquals(31, march.buckets.size());
        assertEquals(timestamp(2024, Calendar.MARCH, 1, 0, 0), march.startMillis);
    }

    private long timestamp(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }
}
