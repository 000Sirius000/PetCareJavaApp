package com.example.petcare.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.Vaccination;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PetRepository repository = new PetRepository(context);
        SharedPreferences prefs = context.getSharedPreferences("petcare_prefs", 0);
        int leadDays = prefs.getInt("vax_days", 30);

        for (Pet pet : repository.getActivePets()) {
            for (FeedingSchedule schedule : repository.getFeedingSchedules(pet.id)) {
                ReminderScheduler.cancelFeeding(context, schedule.id);
            }
            for (Medication medication : repository.getMedications(pet.id)) {
                if (!medication.archived && medication.nextReminderAt > System.currentTimeMillis()) {
                    ReminderScheduler.scheduleMedication(context, medication);
                }
            }
            for (Vaccination vaccination : repository.getVaccinations(pet.id)) {
                ReminderScheduler.scheduleVaccinationDue(context, vaccination, leadDays);
            }
        }
    }
}
