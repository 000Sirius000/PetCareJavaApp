package com.example.petcare.reminders;

import com.example.petcare.data.entities.Medication;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MedicationScheduleCalculator {
    private MedicationScheduleCalculator() {}

    public static long nextOccurrence(Medication medication, long afterExclusiveMillis) {
        return nextOccurrence(medication, afterExclusiveMillis, ZoneId.systemDefault());
    }

    static long nextOccurrence(Medication medication, long afterExclusiveMillis, ZoneId zone) {
        if (medication == null || !medication.reminderEnabled || medication.archived) return 0L;

        LocalDate startDate = dateOf(medication.startDateEpochMillis, zone);
        LocalDate endDate = medication.endDateEpochMillis == null || medication.endDateEpochMillis <= 0L
                ? null
                : dateOf(medication.endDateEpochMillis, zone);
        ZonedDateTime after = Instant.ofEpochMilli(afterExclusiveMillis).atZone(zone);
        LocalDate afterDate = after.toLocalDate();
        LocalDate firstDate = afterDate.isAfter(startDate) ? afterDate : startDate;

        String frequency = medication.frequencyType == null ? "Once daily" : medication.frequencyType.trim();
        if ("Twice daily".equalsIgnoreCase(frequency)) {
            List<Integer> times = new ArrayList<>();
            times.add(normalizeMinute(medication.reminderMinuteOfDay1, 9 * 60));
            int second = normalizeMinute(medication.reminderMinuteOfDay2, 21 * 60);
            if (second != times.get(0)) times.add(second);
            Collections.sort(times);
            return findDaily(firstDate, endDate, times, afterExclusiveMillis, zone);
        }

        int firstMinute = normalizeMinute(medication.reminderMinuteOfDay1, 9 * 60);
        if ("Every N days".equalsIgnoreCase(frequency)) {
            int interval = Math.max(1, medication.frequencyIntervalDays);
            long elapsedDays = Math.max(0L, ChronoUnit.DAYS.between(startDate, firstDate));
            LocalDate candidateDate = startDate.plusDays((elapsedDays / interval) * interval);
            if (candidateDate.isBefore(firstDate)) candidateDate = candidateDate.plusDays(interval);
            while (endDate == null || !candidateDate.isAfter(endDate)) {
                long candidate = atMinute(candidateDate, firstMinute, zone);
                if (candidate > afterExclusiveMillis) return candidate;
                candidateDate = candidateDate.plusDays(interval);
            }
            return 0L;
        }

        if ("Custom".equalsIgnoreCase(frequency)) {
            int mask = medication.reminderWeekdayMask & 0x7F;
            if (mask == 0) return 0L;
            LocalDate candidateDate = firstDate;
            while (endDate == null || !candidateDate.isAfter(endDate)) {
                if (isSelected(candidateDate.getDayOfWeek(), mask)) {
                    long candidate = atMinute(candidateDate, firstMinute, zone);
                    if (candidate > afterExclusiveMillis) return candidate;
                }
                candidateDate = candidateDate.plusDays(1);
            }
            return 0L;
        }

        return findDaily(
                firstDate,
                endDate,
                Collections.singletonList(firstMinute),
                afterExclusiveMillis,
                zone
        );
    }

    public static long plusOneCalendarDay(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .plusDays(1)
                .toInstant()
                .toEpochMilli();
    }

    public static boolean isWithinCourse(Medication medication, long timestamp) {
        if (medication == null) return false;
        ZoneId zone = ZoneId.systemDefault();
        LocalDate date = dateOf(timestamp, zone);
        LocalDate start = dateOf(medication.startDateEpochMillis, zone);
        if (date.isBefore(start)) return false;
        if (medication.endDateEpochMillis == null || medication.endDateEpochMillis <= 0L) return true;
        return !date.isAfter(dateOf(medication.endDateEpochMillis, zone));
    }

    private static long findDaily(LocalDate firstDate, LocalDate endDate, List<Integer> times,
                                  long afterExclusiveMillis, ZoneId zone) {
        LocalDate candidateDate = firstDate;
        while (endDate == null || !candidateDate.isAfter(endDate)) {
            for (int minute : times) {
                long candidate = atMinute(candidateDate, minute, zone);
                if (candidate > afterExclusiveMillis) return candidate;
            }
            candidateDate = candidateDate.plusDays(1);
        }
        return 0L;
    }

    private static long atMinute(LocalDate date, int minuteOfDay, ZoneId zone) {
        LocalTime time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60);
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli();
    }

    private static LocalDate dateOf(long timestamp, ZoneId zone) {
        long safeTimestamp = timestamp > 0L ? timestamp : System.currentTimeMillis();
        return Instant.ofEpochMilli(safeTimestamp).atZone(zone).toLocalDate();
    }

    private static int normalizeMinute(int value, int fallback) {
        return value >= 0 && value < 24 * 60 ? value : fallback;
    }

    private static boolean isSelected(DayOfWeek day, int mask) {
        int bit = 1 << (day.getValue() - 1);
        return (mask & bit) != 0;
    }
}
