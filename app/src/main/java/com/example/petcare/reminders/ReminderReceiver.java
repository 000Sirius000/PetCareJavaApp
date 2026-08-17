package com.example.petcare.reminders;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Medication;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "petcare_reminders";
    private static final String PREFS = "petcare_prefs";
    private static final String KEY_SHOWN_PREFIX = "shown_reminder_";

    @Override
    public void onReceive(Context context, Intent intent) {
        ensureChannel(context);

        String action = intent.getAction();
        long petId = intent.getLongExtra("petId", 0L);
        long medicationId = intent.getLongExtra("medicationId", 0L);
        long vaccinationId = intent.getLongExtra("vaccinationId", 0L);
        String title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE);
        String text = intent.getStringExtra(ReminderScheduler.EXTRA_TEXT);
        long sourceReminderAt = intent.getLongExtra(
                ReminderScheduler.EXTRA_SOURCE_REMINDER_AT,
                intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_AT, 0L)
        );
        int notificationId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, defaultNotificationId(action, medicationId, vaccinationId));

        if (action != null && action.startsWith("PETCARE_MEDICATION")) {
            PetRepository repository = new PetRepository(context);
            Medication medication = repository.getDb().medicationDao().getById(medicationId);
            if (medication == null || !medication.reminderEnabled || medication.archived) return;

            boolean advanceCadence = intent.getBooleanExtra(ReminderScheduler.EXTRA_ADVANCE_CADENCE, true);
            if (advanceCadence) {
                if (medication.nextReminderAt != sourceReminderAt) return;
                medication.nextReminderAt = MedicationScheduleCalculator.nextOccurrence(
                        medication,
                        Math.max(sourceReminderAt, System.currentTimeMillis())
                );
                repository.getDb().medicationDao().update(medication);
                if (medication.nextReminderAt > 0L) ReminderScheduler.scheduleMedication(context, medication);
                else ReminderScheduler.cancelMedication(context, medication.id);
            }

            if (sourceReminderAt > 0L && repository.getDb().medicationLogDao()
                    .getByMedicationAndSourceReminder(medicationId, sourceReminderAt) != null) {
                return;
            }
        }

        if (alreadyShown(context, action, medicationId, vaccinationId, sourceReminderAt)) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title == null ? "Pet reminder" : title)
                .setContentText(text == null ? "" : text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if ((action != null && action.startsWith("PETCARE_MEDICATION")) || "PETCARE_VACCINATION".equals(action)) {
            builder.addAction(android.R.drawable.checkbox_on_background, "Mark done",
                    actionIntent(context, action + "_DONE", petId, medicationId, vaccinationId, notificationId, sourceReminderAt, title, text, 31));
            builder.addAction(android.R.drawable.ic_media_pause, "Postpone 1 day",
                    actionIntent(context, action + "_POSTPONE", petId, medicationId, vaccinationId, notificationId, sourceReminderAt, title, text, 32));
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel",
                    actionIntent(context, action + "_CANCEL", petId, medicationId, vaccinationId, notificationId, sourceReminderAt, title, text, 33));
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            markShown(context, action, medicationId, vaccinationId, sourceReminderAt);
            nm.notify(notificationId, builder.build());
        }
    }

    private PendingIntent actionIntent(
            Context context,
            String action,
            long petId,
            long medicationId,
            long vaccinationId,
            int notificationId,
            long sourceReminderAt,
            String title,
            String text,
            int offset
    ) {
        Intent completeIntent = new Intent(context, ReminderActionReceiver.class);
        completeIntent.setAction(action);
        completeIntent.putExtra("petId", petId);
        completeIntent.putExtra("medicationId", medicationId);
        completeIntent.putExtra("vaccinationId", vaccinationId);
        completeIntent.putExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, notificationId);
        completeIntent.putExtra(ReminderScheduler.EXTRA_SOURCE_REMINDER_AT, sourceReminderAt);
        completeIntent.putExtra(ReminderScheduler.EXTRA_TITLE, title);
        completeIntent.putExtra(ReminderScheduler.EXTRA_TEXT, text);
        int base = medicationId > 0 ? (int) (20000 + medicationId) : (int) (30000 + vaccinationId);
        return PendingIntent.getBroadcast(context, base + offset, completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private int defaultNotificationId(String action, long medicationId, long vaccinationId) {
        if (action != null && action.startsWith("PETCARE_MEDICATION")) return ReminderScheduler.medicationNotificationId(medicationId);
        if ("PETCARE_VACCINATION".equals(action)) return ReminderScheduler.vaccinationNotificationId(vaccinationId);
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    private boolean alreadyShown(Context context, String action, long medicationId, long vaccinationId, long sourceReminderAt) {
        if (sourceReminderAt <= 0L) return false;
        return prefs(context).getLong(shownKey(action, medicationId, vaccinationId), Long.MIN_VALUE) == sourceReminderAt;
    }

    private void markShown(Context context, String action, long medicationId, long vaccinationId, long sourceReminderAt) {
        if (sourceReminderAt <= 0L) return;
        prefs(context).edit().putLong(shownKey(action, medicationId, vaccinationId), sourceReminderAt).apply();
    }

    private String shownKey(String action, long medicationId, long vaccinationId) {
        long itemId = medicationId > 0L ? medicationId : vaccinationId;
        return KEY_SHOWN_PREFIX + action + "_" + itemId;
    }

    private SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Pet reminders", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Medication and vaccine reminders");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
