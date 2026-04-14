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
        long scheduleId = intent.getLongExtra("scheduleId", 0L);
        long medicationId = intent.getLongExtra("medicationId", 0L);
        String title = intent.getStringExtra("title");
        String text = intent.getStringExtra("text");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title == null ? "Pet reminder" : title)
                .setContentText(text == null ? "" : text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if ("PETCARE_FEEDING".equals(action) || "PETCARE_MEDICATION".equals(action)) {
            Intent completeIntent = new Intent(context, ReminderActionReceiver.class);
            completeIntent.setAction(action + "_DONE");
            completeIntent.putExtra("petId", petId);
            completeIntent.putExtra("scheduleId", scheduleId);
            completeIntent.putExtra("medicationId", medicationId);
            completeIntent.putExtra("title", title);
            completeIntent.putExtra("text", text);

            PendingIntent actionPendingIntent = PendingIntent.getBroadcast(
                    context,
                    (int) System.currentTimeMillis(),
                    completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            builder.addAction(android.R.drawable.checkbox_on_background, "Mark done", actionPendingIntent);
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
        }
    }

    private void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pet reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Feeding, medication and vaccine reminders");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
