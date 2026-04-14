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

    public static void scheduleFeeding(Context context, FeedingSchedule schedule) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, schedule.hourOfDay);
        calendar.set(Calendar.MINUTE, schedule.minute);
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("PETCARE_FEEDING");
        intent.putExtra("petId", schedule.petId);
        intent.putExtra("scheduleId", schedule.id);
        intent.putExtra("title", schedule.mealName);
        intent.putExtra("text", schedule.foodType + " • " + schedule.portion + " " + schedule.portionUnit);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                (int) (10000 + schedule.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        setAlarmSafely(context, alarmManager, calendar.getTimeInMillis(), pi);
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
                    // Fallback: schedule an inexact alarm instead of crashing
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
            // Final safety net: fallback to inexact alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}