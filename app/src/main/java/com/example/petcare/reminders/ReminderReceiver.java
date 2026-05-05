package com.example.petcare.reminders;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "petcare_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        ensureChannel(context);

        String action = intent.getAction();
        long petId = intent.getLongExtra("petId", 0L);
        long medicationId = intent.getLongExtra("medicationId", 0L);
        long vaccinationId = intent.getLongExtra("vaccinationId", 0L);
        String title = intent.getStringExtra("title");
        String text = intent.getStringExtra("text");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title == null ? "Pet reminder" : title)
                .setContentText(text == null ? "" : text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if ("PETCARE_MEDICATION".equals(action) || "PETCARE_VACCINATION".equals(action)) {
            builder.addAction(android.R.drawable.checkbox_on_background, "Mark done",
                    actionIntent(context, action + "_DONE", petId, medicationId, vaccinationId, 31));
            builder.addAction(android.R.drawable.ic_media_pause, "Postpone 1 day",
                    actionIntent(context, action + "_POSTPONE", petId, medicationId, vaccinationId, 32));
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel",
                    actionIntent(context, action + "_CANCEL", petId, medicationId, vaccinationId, 33));
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
    }

    private PendingIntent actionIntent(Context context, String action, long petId, long medicationId, long vaccinationId, int offset) {
        Intent completeIntent = new Intent(context, ReminderActionReceiver.class);
        completeIntent.setAction(action);
        completeIntent.putExtra("petId", petId);
        completeIntent.putExtra("medicationId", medicationId);
        completeIntent.putExtra("vaccinationId", vaccinationId);
        int base = medicationId > 0 ? (int) (20000 + medicationId) : (int) (30000 + vaccinationId);
        return PendingIntent.getBroadcast(context, base + offset, completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
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
