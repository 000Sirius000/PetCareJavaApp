package com.example.petcare.data;

import android.content.Context;
import android.content.SharedPreferences;

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
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PetRepository {
    private static final String PREFS = "petcare_prefs";
    private static final String KEY_ACTIVE_PET_ID = "active_pet_id";

    private final Context appContext;
    private final AppDatabase db;

    public PetRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.db = AppDatabase.getInstance(context);
    }

    public List<Pet> getActivePets() { return db.petDao().getActivePets(); }
    public List<Pet> getArchivedPets() { return db.petDao().getArchivedPets(); }
    public Pet getPet(long petId) { return db.petDao().getById(petId); }

    public long savePet(Pet pet) {
        if (pet.id == 0) {
            pet.createdAt = System.currentTimeMillis();
            long newId = db.petDao().insert(pet);
            setSelectedPetId(newId);
            return newId;
        }
        db.petDao().update(pet);
        return pet.id;
    }

    public void archivePet(long petId) {
        db.petDao().archive(petId);
        if (getSelectedPetIdRaw() == petId) {
            clearSelectedPetId();
            ensureSelectedPetId();
        }
    }

    public void recoverPet(long petId) { db.petDao().recover(petId); }

    public void deletePet(Pet pet) {
        if (pet != null && getSelectedPetIdRaw() == pet.id) {
            clearSelectedPetId();
        }
        db.petDao().delete(pet);
        ensureSelectedPetId();
    }

    public Pet getSelectedPet() {
        long petId = ensureSelectedPetId();
        return petId <= 0L ? null : getPet(petId);
    }

    public long getSelectedPetId() {
        return ensureSelectedPetId();
    }

    public void setSelectedPetId(long petId) {
        if (petId <= 0L) {
            clearSelectedPetId();
            return;
        }
        Pet pet = getPet(petId);
        if (pet != null && !pet.archived) {
            prefs().edit().putLong(KEY_ACTIVE_PET_ID, petId).apply();
        }
    }

    private long ensureSelectedPetId() {
        long savedId = getSelectedPetIdRaw();
        if (savedId > 0L) {
            Pet saved = getPet(savedId);
            if (saved != null && !saved.archived) {
                return savedId;
            }
        }

        Pet newest = null;
        for (Pet pet : getActivePets()) {
            if (newest == null || pet.createdAt > newest.createdAt) {
                newest = pet;
            }
        }

        long newId = newest == null ? 0L : newest.id;
        if (newId > 0L) {
            prefs().edit().putLong(KEY_ACTIVE_PET_ID, newId).apply();
        } else {
            clearSelectedPetId();
        }
        return newId;
    }

    private long getSelectedPetIdRaw() {
        return prefs().getLong(KEY_ACTIVE_PET_ID, 0L);
    }

    private void clearSelectedPetId() {
        prefs().edit().remove(KEY_ACTIVE_PET_ID).apply();
    }

    private SharedPreferences prefs() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

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

    public WeightEntry getLatestWeightEntry(long petId) {
        List<WeightEntry> items = getWeightEntries(petId);
        return items.isEmpty() ? null : items.get(0);
    }

    public List<SymptomEntry> getSymptomEntries(long petId) { return db.symptomEntryDao().getForPet(petId); }
    public List<SymptomTag> getSymptomTags() { return db.symptomTagDao().getAll(); }
    public List<ReproductiveEvent> getReproductiveEvents(long petId) { return db.reproductiveEventDao().getForPet(petId); }

    public int getWeeklyActivityProgressPercent(long petId, int goalMinutes) {
        return getDailyActivityProgressPercent(petId, goalMinutes);
    }

    public int getDailyActivityProgressPercent(long petId, int goalMinutes) {
        if (goalMinutes <= 0) return 0;
        int minutes = getTodayActiveMinutes(petId);
        return Math.min(100, (int) ((minutes * 100f) / goalMinutes));
    }

    public int getTodayActiveMinutes(long petId) {
        int total = 0;
        long startOfDay = startOfTodayMillis();
        for (ActivitySession session : getActivitySessions(petId)) {
            if (session.sessionDateEpochMillis >= startOfDay) {
                total += Math.max(0, session.durationMinutes);
            }
        }
        return total;
    }

    public String getTodayActivitySummaryText(long petId) {
        long startOfDay = startOfTodayMillis();
        Map<String, ActivityTotals> totalsByType = new LinkedHashMap<>();
        int totalMinutes = 0;
        double totalDistance = 0d;

        for (ActivitySession session : getActivitySessions(petId)) {
            if (session.sessionDateEpochMillis < startOfDay) continue;

            String type = session.activityType == null || session.activityType.trim().isEmpty()
                    ? "Activity"
                    : session.activityType.trim();

            ActivityTotals totals = totalsByType.get(type);
            if (totals == null) {
                totals = new ActivityTotals();
                totalsByType.put(type, totals);
            }

            int minutes = Math.max(0, session.durationMinutes);
            totals.minutes += minutes;
            totalMinutes += minutes;

            if (session.distance != null && supportsDistance(type)) {
                totals.distanceKm += Math.max(0d, session.distance);
                totalDistance += Math.max(0d, session.distance);
            }
        }

        if (totalsByType.isEmpty()) {
            return "No activity logged today.";
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, ActivityTotals> entry : totalsByType.entrySet()) {
            builder.append(entry.getKey())
                    .append("     ")
                    .append(entry.getValue().minutes)
                    .append(" min");
            if (supportsDistance(entry.getKey()) && entry.getValue().distanceKm > 0d) {
                builder.append(" · ")
                        .append(String.format(Locale.getDefault(), "%.1f km", entry.getValue().distanceKm));
            }
            builder.append('\n');
        }

        builder.append("Total   ").append(totalMinutes).append(" min");
        if (totalDistance > 0d) {
            builder.append(" · ").append(String.format(Locale.getDefault(), "%.1f km", totalDistance));
        }
        return builder.toString();
    }

    private boolean supportsDistance(String type) {
        return "walk".equalsIgnoreCase(type) || "run".equalsIgnoreCase(type);
    }

    private long startOfTodayMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public int getPendingReminderCount(long petId) {
        int count = 0;
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

    public String getLastWeightSummary(Pet pet) {
        if (pet == null) return "No weight recorded yet.";
        WeightEntry latest = getLatestWeightEntry(pet.id);
        if (latest == null) return "No weight recorded yet.";

        String unit = latest.unit == null || latest.unit.trim().isEmpty() ? "kg" : latest.unit.trim();
        return String.format(Locale.getDefault(), "Last weight: %.1f %s · %s",
                latest.weightValue,
                unit,
                ageLabel(latest.measuredAt));
    }

    public boolean isLatestWeightOutOfRange(Pet pet) {
        if (pet == null) return false;
        WeightEntry latest = getLatestWeightEntry(pet.id);
        if (latest == null) return false;
        if (pet.minHealthyWeight != null && latest.weightValue < pet.minHealthyWeight) return true;
        return pet.maxHealthyWeight != null && latest.weightValue > pet.maxHealthyWeight;
    }

    private String ageLabel(long timestamp) {
        long diff = Math.max(0L, System.currentTimeMillis() - timestamp);
        long days = diff / (24L * 60 * 60 * 1000);
        if (days <= 0) return "today";
        if (days == 1) return "1 day ago";
        return days + " days ago";
    }

    public String getNextVaccinationSummary(long petId) {
        Vaccination best = null;
        for (Vaccination item : getVaccinations(petId)) {
            if (item.nextDueAt == null) continue;
            if (best == null || item.nextDueAt < best.nextDueAt) best = item;
        }
        return best == null ? "No due vaccine" : best.vaccineName + " • " + FormatUtils.date(best.nextDueAt);
    }

    public List<Object> getUpcomingReminderPreview(long petId) {
        List<Object> items = new ArrayList<>();

        for (Medication medication : getMedications(petId)) {
            if (!medication.archived && medication.nextReminderAt > 0L) {
                items.add(medication);
            }
        }

        long leadTime = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000);
        for (Vaccination vaccination : getVaccinations(petId)) {
            if (vaccination.nextDueAt != null && vaccination.nextDueAt <= leadTime) {
                items.add(vaccination);
            }
        }

        items.sort((left, right) -> Long.compare(reminderTime(left), reminderTime(right)));
        if (items.size() > 5) {
            return new ArrayList<>(items.subList(0, 5));
        }
        return items;
    }

    public long reminderTime(Object item) {
        if (item instanceof Medication) return ((Medication) item).nextReminderAt;
        if (item instanceof Vaccination) return ((Vaccination) item).nextDueAt == null ? Long.MAX_VALUE : ((Vaccination) item).nextDueAt;
        return Long.MAX_VALUE;
    }

    public long insertDemoDataIfEmpty() {
        if (!getActivePets().isEmpty()) return getSelectedPetId();

        Pet pet = new Pet();
        pet.name = "Milo";
        pet.species = "Dog";
        pet.breed = "Corgi";
        pet.birthInfo = "2021-05-20";
        pet.sex = "Male";
        pet.weeklyActivityGoalMinutes = 45;
        pet.minHealthyWeight = 10.0;
        pet.maxHealthyWeight = 14.0;
        pet.internationalPetPassport = "INT-MILO-001";
        pet.nationalPetPassport = "UA-MILO-001";
        pet.microchipCode = "900000000000001";
        pet.microchipImplantationDate = "2021-06-01";
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
        schedule.createdAtEpochMillis = System.currentTimeMillis();
        long scheduleId = db.feedingScheduleDao().insert(schedule);
        logFeeding(petId, scheduleId, schedule.mealName, schedule.portion + " g");

        ActivitySession session = new ActivitySession();
        session.petId = petId;
        session.activityType = "Walk";
        session.durationMinutes = 35;
        session.distance = 2.8;
        session.distanceUnit = "km";
        session.sessionDateEpochMillis = System.currentTimeMillis() - 2L * 60 * 60 * 1000;
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
        log.portion = ensureGrams(portion);

        FeedingSchedule schedule = db.feedingScheduleDao().getById(scheduleId);
        log.foodType = schedule == null || schedule.foodType == null || schedule.foodType.trim().isEmpty()
                ? "Dry food"
                : schedule.foodType.trim();

        db.feedingLogDao().insert(log);
    }

    private String ensureGrams(String portion) {
        double amount = FormatUtils.parseLeadingNumber(portion);
        if (amount <= 0d) return "0 g";
        return FormatUtils.number(amount) + " g";
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
        items.addAll(getSymptomEntries(petId));
        items.addAll(getReproductiveEvents(petId));
        return items;
    }

    public AppDatabase getDb() { return db; }

    private static class ActivityTotals {
        int minutes;
        double distanceKm;
    }
}
