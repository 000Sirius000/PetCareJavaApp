package com.example.petcare.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DAY_LABEL_FORMAT = new SimpleDateFormat("EEE d", Locale.getDefault());
    private static final SimpleDateFormat MONTH_LABEL_FORMAT = new SimpleDateFormat("MMM", Locale.getDefault());

    public static String date(long time) {
        return DATE_FORMAT.format(new Date(time));
    }

    public static String shortDate(long time) {
        return SHORT_DATE_FORMAT.format(new Date(time));
    }

    public static String dayLabel(long time) {
        return DAY_LABEL_FORMAT.format(new Date(time));
    }

    public static String monthLabel(long time) {
        return MONTH_LABEL_FORMAT.format(new Date(time));
    }

    public static String dateTime(long time) {
        return DATE_TIME_FORMAT.format(new Date(time));
    }

    public static String time(long time) {
        return TIME_FORMAT.format(new Date(time));
    }

    public static String nullable(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value.trim();
    }

    public static String hoursLabel(double hours) {
        if (hours == Math.rint(hours)) {
            return String.format(Locale.getDefault(), "%.0f h", hours);
        }
        return String.format(Locale.getDefault(), "%.1f h", hours);
    }

    public static String number(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.getDefault(), "%.0f", value);
        }
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    public static double parseLeadingNumber(String value) {
        if (value == null) return 0d;
        StringBuilder builder = new StringBuilder();
        for (char c : value.trim().toCharArray()) {
            if (Character.isDigit(c) || c == '.' || c == ',') {
                builder.append(c == ',' ? '.' : c);
            } else if (builder.length() > 0) {
                break;
            }
        }
        if (builder.length() == 0) return 0d;
        try {
            return Double.parseDouble(builder.toString());
        } catch (Exception ignored) {
            return 0d;
        }
    }
}
