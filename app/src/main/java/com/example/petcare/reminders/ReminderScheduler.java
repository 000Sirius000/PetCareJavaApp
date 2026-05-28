package com.example.petcare.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.util.FormatUtils;

import java.util.Calendar;

public class ReminderScheduler {
    static final String EXTRA_NOTIFICATION_ID = "notificationId";
    static final String EXTRA_REMINDER_AT = "reminderAt";
    static final String EXTRA_SOURCE_REMINDER_AT = "sourceReminderAt";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_TEXT = "text";

    /**
     * Feeding reminders were removed from the product.
     * Keep this method as a safe no-op so old callers do not schedule new alarms.
     */
    public static void scheduleFeeding(Context context, FeedingSchedule schedule) {
        cancelFeeding(context, schedule == null ? 0L : schedule.id);
    }

    public static void cancelFeeding(Context context, long scheduleId) {
        if (scheduleId <= 0L) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("PETCARE_FEEDING");
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                (int) (10000 + scheduleId),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pi != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pi);
            }
            pi.cancel();
        }
    }

    public static void scheduleMedication(Context context, Medication medication) {
        if (medication == null || medication.id <= 0L || medication.archived || medication.nextReminderAt <= 0L) {
            cancelMedication(context, medication == null ? 0L : medication.id);
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("PETCARE_MEDICATION");
        intent.putExtra("petId", medication.petId);
        intent.putExtra("medicationId", medication.id);
        intent.putExtra(EXTRA_TITLE, medication.medicationName);
        intent.putExtra(EXTRA_TEXT, medication.dosage + " " + medication.dosageUnit);
        intent.putExtra(EXTRA_REMINDER_AT, medication.nextReminderAt);
        intent.putExtra(EXTRA_SOURCE_REMINDER_AT, medication.nextReminderAt);
        intent.putExtra(EXTRA_NOTIFICATION_ID, medicationNotificationId(medication.id));

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                medicationNotificationId(medication.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        setAlarmSafely(context, alarmManager, medication.nextReminderAt, pi);
    }

    public static void cancelMedication(Context context, long medicationId) {
        cancelReminder(context, "PETCARE_MEDICATION", medicationNotificationId(medicationId));
    }

    public static void scheduleVaccinationDue(Context context, Vaccination vaccination, int leadDays) {
        if (vaccination.nextDueAt == null) {
            cancelVaccination(context, vaccination.id);
            return;
        }

        long sourceReminderAt = vaccination.nextDueAt - (leadDays * 24L * 60 * 60 * 1000);
        long reminderAt = sourceReminderAt;
        if (reminderAt < System.currentTimeMillis()) {
            reminderAt = System.currentTimeMillis() + 15_000L;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("PETCARE_VACCINATION");
        intent.putExtra("petId", vaccination.petId);
        intent.putExtra("vaccinationId", vaccination.id);
        intent.putExtra("vaccinationDueAt", vaccination.nextDueAt);
        intent.putExtra(EXTRA_TITLE, "Vaccination due: " + vaccination.vaccineName);
        intent.putExtra(EXTRA_TEXT, "Due date: " + FormatUtils.date(vaccination.nextDueAt));
        intent.putExtra(EXTRA_REMINDER_AT, reminderAt);
        intent.putExtra(EXTRA_SOURCE_REMINDER_AT, sourceReminderAt);
        intent.putExtra(EXTRA_NOTIFICATION_ID, vaccinationNotificationId(vaccination.id));

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                vaccinationNotificationId(vaccination.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        setAlarmSafely(context, alarmManager, reminderAt, pi);
    }

    public static void cancelVaccination(Context context, long vaccinationId) {
        cancelReminder(context, "PETCARE_VACCINATION", vaccinationNotificationId(vaccinationId));
    }

    static int medicationNotificationId(long medicationId) {
        return (int) (20000 + medicationId);
    }

    static int vaccinationNotificationId(long vaccinationId) {
        return (int) (30000 + vaccinationId);
    }

    private static void cancelReminder(Context context, String action, int requestCode) {
        if (requestCode <= 0) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pi != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pi);
            }
            pi.cancel();
        }
    }

    private static void setAlarmSafely(Context context, AlarmManager alarmManager, long triggerAt, PendingIntent pi) {
        if (alarmManager == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                    return;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException e) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}
