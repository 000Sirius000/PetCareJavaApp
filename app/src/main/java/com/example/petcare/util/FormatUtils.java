package com.example.petcare.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public static String date(long time) {
        return DATE_FORMAT.format(new Date(time));
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
}
