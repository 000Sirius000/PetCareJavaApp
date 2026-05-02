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
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("PETCARE_MEDICATION");
        intent.putExtra("petId", medication.petId);
        intent.putExtra("medicationId", medication.id);
        intent.putExtra("title", medication.medicationName);
        intent.putExtra("text", medication.dosage + " " + medication.dosageUnit);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                (int) (20000 + medication.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        setAlarmSafely(context, alarmManager, medication.nextReminderAt, pi);
    }

    public static void scheduleVaccinationDue(Context context, Vaccination vaccination, int leadDays) {
        if (vaccination.nextDueAt == null) {
            return;
        }

        long reminderAt = vaccination.nextDueAt - (leadDays * 24L * 60 * 60 * 1000);
        if (reminderAt < System.currentTimeMillis()) {
            reminderAt = System.currentTimeMillis() + 15_000L;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("PETCARE_VACCINATION");
        intent.putExtra("petId", vaccination.petId);
        intent.putExtra("vaccinationId", vaccination.id);
        intent.putExtra("title", "Vaccination due: " + vaccination.vaccineName);
        intent.putExtra("text", "Due date: " + FormatUtils.date(vaccination.nextDueAt));

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                (int) (30000 + vaccination.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        setAlarmSafely(context, alarmManager, reminderAt, pi);
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
