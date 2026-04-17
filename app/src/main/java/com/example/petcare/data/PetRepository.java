package com.example.petcare.data;

import android.content.Context;

import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.data.entities.FeedingLog;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.MedicationLog;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.ReproductiveEvent;
import com.example.petcare.data.entities.SymptomEntry;
import com.example.petcare.data.entities.SymptomTag;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.data.entities.VetVisit;
import com.example.petcare.data.entities.WeightEntry;
import com.example.petcare.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class PetRepository {
    private final AppDatabase db;

    public PetRepository(Context context) {
        this.db = AppDatabase.getInstance(context);
    }

    public List<Pet> getActivePets() { return db.petDao().getActivePets(); }
    public List<Pet> getArchivedPets() { return db.petDao().getArchivedPets(); }
    public Pet getPet(long petId) { return db.petDao().getById(petId); }

    public long savePet(Pet pet) {
        if (pet.id == 0) {
            pet.createdAt = System.currentTimeMillis();
            return db.petDao().insert(pet);
        }
        db.petDao().update(pet);
        return pet.id;
    }

    public void archivePet(long petId) { db.petDao().archive(petId); }
    public void recoverPet(long petId) { db.petDao().recover(petId); }
    public void deletePet(Pet pet) { db.petDao().delete(pet); }
    public List<VetVisit> getVetVisits(long petId) { return db.vetVisitDao().getForPet(petId); }
    public List<Vaccination> getVaccinations(long petId) { return db.vaccinationDao().getForPet(petId); }
    public List<Medication> getMedications(long petId) { return db.medicationDao().getForPet(petId); }
    public List<FeedingSchedule> getFeedingSchedules(long petId) { return db.feedingScheduleDao().getForPet(petId); }
    public List<FeedingLog> getFeedingLogs(long petId) { return db.feedingLogDao().getForPet(petId); }
    public List<MedicationLog> getMedicationLogs(long petId) { return db.medicationLogDao().getForPet(petId); }
    public List<ActivitySession> getActivitySessions(long petId) { return db.activitySessionDao().getForPet(petId); }

    public ActivitySession getLatestActivitySession(long petId) {
        List<ActivitySession> items = getActivitySessions(petId);
        return items.isEmpty() ? null : items.get(0);
    }

    public Vaccination getNextVaccinationDue(long petId) {
        Vaccination next = null;
        for (Vaccination vaccination : getVaccinations(petId)) {
            if (vaccination.nextDueAt == null) continue;
            if (next == null || vaccination.nextDueAt < next.nextDueAt) {
                next = vaccination;
            }
        }
        return next;
    }

    public List<WeightEntry> getWeightEntries(long petId) { return db.weightEntryDao().getForPet(petId); }
    public List<SymptomEntry> getSymptomEntries(long petId) { return db.symptomEntryDao().getForPet(petId); }
    public List<SymptomTag> getSymptomTags() { return db.symptomTagDao().getAll(); }
    public List<ReproductiveEvent> getReproductiveEvents(long petId) { return db.reproductiveEventDao().getForPet(petId); }

    public int getWeeklyActivityProgressPercent(long petId, int goalMinutes) {
        if (goalMinutes <= 0) return 0;
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        Integer done = db.activitySessionDao().getMinutesSince(petId, sevenDaysAgo);
        int minutes = done == null ? 0 : done;
        return Math.min(100, (int) ((minutes * 100f) / goalMinutes));
    }

    public int getPendingReminderCount(long petId) {
        int count = db.feedingScheduleDao().getForPet(petId).size();
        for (Medication medication : db.medicationDao().getForPet(petId)) {
            if (!medication.archived) count++;
        }
        long leadTime = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000);
        for (Vaccination vaccination : db.vaccinationDao().getForPet(petId)) {
            if (vaccination.nextDueAt != null && vaccination.nextDueAt <= leadTime) count++;
        }
        return count;
    }

    public String getLastActivitySummary(long petId) {
        List<ActivitySession> sessions = getActivitySessions(petId);
        if (sessions.isEmpty()) return "No activity yet";
        ActivitySession item = sessions.get(0);
        return item.activityType + " • " + item.durationMinutes + " min";
    }

    public String getNextVaccinationSummary(long petId) {
        Vaccination best = null;
        for (Vaccination item : getVaccinations(petId)) {
            if (item.nextDueAt == null) continue;
            if (best == null || item.nextDueAt < best.nextDueAt) best = item;
        }
        return best == null ? "No due vaccine" : best.vaccineName + " • " + FormatUtils.date(best.nextDueAt);
    }

    public long insertDemoDataIfEmpty() {
        if (!getActivePets().isEmpty()) return getActivePets().get(0).id;

        Pet pet = new Pet();
        pet.name = "Milo";
        pet.species = "Dog";
        pet.breed = "Corgi";
        pet.birthInfo = "2021-05-20";
        pet.sex = "Male";
        pet.weeklyActivityGoalMinutes = 210;
        long petId = savePet(pet);

        VetVisit visit = new VetVisit();
        visit.petId = petId;
        visit.visitDateEpochMillis = System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000;
        visit.clinicName = "Happy Paws Clinic";
        visit.vetName = "Dr. Ivanenko";
        visit.reason = "Annual check";
        visit.diagnosisNotes = "Healthy, advised weight monitoring.";
        long visitId = db.vetVisitDao().insert(visit);

        Vaccination vaccination = new Vaccination();
        vaccination.petId = petId;
        vaccination.vaccineName = "Rabies";
        vaccination.administeredAt = System.currentTimeMillis() - 20L * 24 * 60 * 60 * 1000;
        vaccination.nextDueAt = System.currentTimeMillis() + 15L * 24 * 60 * 60 * 1000;
        vaccination.batchNumber = "RB-204";
        db.vaccinationDao().insert(vaccination);

        FeedingSchedule schedule = new FeedingSchedule();
        schedule.petId = petId;
        schedule.mealName = "Breakfast";
        schedule.hourOfDay = 8;
        schedule.minute = 0;
        schedule.foodType = "Dry food";
        schedule.portion = "100";
        schedule.portionUnit = "g";
        long scheduleId = db.feedingScheduleDao().insert(schedule);
        logFeeding(petId, scheduleId, schedule.mealName, schedule.portion + " " + schedule.portionUnit);

        ActivitySession session = new ActivitySession();
        session.petId = petId;
        session.activityType = "Walk";
        session.durationMinutes = 95;
        session.distance = 2.8;
        session.distanceUnit = "km";
        session.sessionDateEpochMillis = System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000;
        session.notes = "Park walk";
        db.activitySessionDao().insert(session);

        WeightEntry weight = new WeightEntry();
        weight.petId = petId;
        weight.measuredAt = System.currentTimeMillis() - 6L * 24 * 60 * 60 * 1000;
        weight.weightValue = 12.3;
        weight.unit = "kg";
        weight.healthyMin = 10.0;
        weight.healthyMax = 14.0;
        db.weightEntryDao().insert(weight);

        SymptomEntry symptom = new SymptomEntry();
        symptom.petId = petId;
        symptom.recordedAt = System.currentTimeMillis() - 4L * 24 * 60 * 60 * 1000;
        symptom.tagsCsv = "Lethargy, Appetite loss";
        symptom.severity = "Mild";
        symptom.notes = "Was less active than usual for half a day.";
        symptom.linkedVetVisitId = visitId;
        db.symptomEntryDao().insert(symptom);

        Medication medication = new Medication();
        medication.petId = petId;
        medication.linkedVisitId = visitId;
        medication.medicationName = "Omega supplement";
        medication.dosage = "1";
        medication.dosageUnit = "capsule";
        medication.frequencyType = "Once daily";
        medication.frequencyIntervalDays = 1;
        medication.startDateEpochMillis = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000;
        medication.endDateEpochMillis = System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000;
        medication.nextReminderAt = System.currentTimeMillis() + 2L * 60 * 60 * 1000;
        medication.archived = false;
        db.medicationDao().insert(medication);

        return petId;
    }

    public void logFeeding(long petId, long scheduleId, String mealName, String portion) {
        FeedingLog log = new FeedingLog();
        log.petId = petId;
        log.scheduleId = scheduleId;
        log.completedAt = System.currentTimeMillis();
        log.mealName = mealName;
        log.portion = portion;
        db.feedingLogDao().insert(log);
    }

    public void logMedication(long petId, long medicationId, boolean missed) {
        MedicationLog log = new MedicationLog();
        log.petId = petId;
        log.medicationId = medicationId;
        log.administeredAt = System.currentTimeMillis();
        log.markedBy = "Owner";
        log.missed = missed;
        db.medicationLogDao().insert(log);
    }

    public List<Object> getHealthTimeline(long petId) {
        List<Object> items = new ArrayList<>();
        items.addAll(getVetVisits(petId));
        items.addAll(getVaccinations(petId));
        items.addAll(getMedications(petId));
        items.addAll(getWeightEntries(petId));
        items.addAll(getSymptomEntries(petId));
        items.addAll(getReproductiveEvents(petId));
        return items;
    }

    public AppDatabase getDb() { return db; }
}
