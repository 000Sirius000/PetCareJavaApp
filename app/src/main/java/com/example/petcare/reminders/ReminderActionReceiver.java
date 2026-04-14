package com.example.petcare.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.data.entities.Medication;

public class ReminderActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PetRepository repository = new PetRepository(context);
        String action = intent.getAction();
        long petId = intent.getLongExtra("petId", 0L);

        if ("PETCARE_FEEDING_DONE".equals(action)) {
            long scheduleId = intent.getLongExtra("scheduleId", 0L);
            FeedingSchedule schedule = null;
            for (FeedingSchedule item : repository.getFeedingSchedules(petId)) {
                if (item.id == scheduleId) {
                    schedule = item;
                    break;
                }
            }
            if (schedule != null) {
                repository.logFeeding(petId, schedule.id, schedule.mealName, schedule.portion + " " + schedule.portionUnit);
                ReminderScheduler.scheduleFeeding(context, schedule);
                Toast.makeText(context, "Feeding logged", Toast.LENGTH_SHORT).show();
            }
        } else if ("PETCARE_MEDICATION_DONE".equals(action)) {
            long medicationId = intent.getLongExtra("medicationId", 0L);
            Medication medication = null;
            for (Medication item : repository.getMedications(petId)) {
                if (item.id == medicationId) {
                    medication = item;
                    break;
                }
            }
            if (medication != null) {
                repository.logMedication(petId, medication.id, false);
                medication.nextReminderAt = System.currentTimeMillis()
                        + (Math.max(1, medication.frequencyIntervalDays) * 24L * 60 * 60 * 1000);
                repository.getDb().medicationDao().update(medication);
                ReminderScheduler.scheduleMedication(context, medication);
                Toast.makeText(context, "Medication logged", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
