package com.example.petcare.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.Vaccination;

public class ReminderActionReceiver extends BroadcastReceiver {
    private static final long DAY = 24L * 60 * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        PetRepository repository = new PetRepository(context);
        String action = intent.getAction();
        long petId = intent.getLongExtra("petId", 0L);

        if (action == null) return;

        if (action.startsWith("PETCARE_MEDICATION")) {
            long medicationId = intent.getLongExtra("medicationId", 0L);
            Medication medication = findMedication(repository, petId, medicationId);
            if (medication == null) return;

            if (action.endsWith("_DONE")) {
                repository.logMedication(petId, medication.id, false);
                medication.nextReminderAt = System.currentTimeMillis() + Math.max(1, medication.frequencyIntervalDays) * DAY;
                repository.getDb().medicationDao().update(medication);
                ReminderScheduler.scheduleMedication(context, medication);
                Toast.makeText(context, "Medication completed", Toast.LENGTH_SHORT).show();
            } else if (action.endsWith("_POSTPONE")) {
                medication.nextReminderAt = System.currentTimeMillis() + DAY;
                repository.getDb().medicationDao().update(medication);
                ReminderScheduler.scheduleMedication(context, medication);
                Toast.makeText(context, "Medication reminder postponed", Toast.LENGTH_SHORT).show();
            } else if (action.endsWith("_CANCEL")) {
                medication.nextReminderAt = 0L;
                repository.getDb().medicationDao().update(medication);
                Toast.makeText(context, "Medication reminder cancelled", Toast.LENGTH_SHORT).show();
            }
        } else if (action.startsWith("PETCARE_VACCINATION")) {
            long vaccinationId = intent.getLongExtra("vaccinationId", 0L);
            Vaccination vaccination = findVaccination(repository, petId, vaccinationId);
            if (vaccination == null) return;

            if (action.endsWith("_DONE")) {
                vaccination.administeredAt = System.currentTimeMillis();
                repository.getDb().vaccinationDao().update(vaccination);
                Toast.makeText(context, "Vaccination marked completed", Toast.LENGTH_SHORT).show();
            } else if (action.endsWith("_POSTPONE")) {
                vaccination.nextDueAt = System.currentTimeMillis() + DAY;
                repository.getDb().vaccinationDao().update(vaccination);
                ReminderScheduler.scheduleVaccinationDue(context, vaccination, 0);
                Toast.makeText(context, "Vaccination reminder postponed", Toast.LENGTH_SHORT).show();
            } else if (action.endsWith("_CANCEL")) {
                vaccination.nextDueAt = null;
                repository.getDb().vaccinationDao().update(vaccination);
                Toast.makeText(context, "Vaccination reminder cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Medication findMedication(PetRepository repository, long petId, long medicationId) {
        for (Medication item : repository.getMedications(petId)) if (item.id == medicationId) return item;
        return null;
    }

    private Vaccination findVaccination(PetRepository repository, long petId, long vaccinationId) {
        for (Vaccination item : repository.getVaccinations(petId)) if (item.id == vaccinationId) return item;
        return null;
    }
}
