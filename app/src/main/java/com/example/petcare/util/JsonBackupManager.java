package com.example.petcare.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.data.entities.FeedingLog;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.MedicationLog;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.SymptomEntry;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.data.entities.VetVisit;
import com.example.petcare.data.entities.WeightEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonBackupManager {

    public static class PetBackup {
        public Pet pet;
        public List<VetVisit> vetVisits = new ArrayList<>();
        public List<Vaccination> vaccinations = new ArrayList<>();
        public List<Medication> medications = new ArrayList<>();
        public List<FeedingSchedule> feedingSchedules = new ArrayList<>();
        public List<FeedingLog> feedingLogs = new ArrayList<>();
        public List<MedicationLog> medicationLogs = new ArrayList<>();
        public List<ActivitySession> activitySessions = new ArrayList<>();
        public List<WeightEntry> weightEntries = new ArrayList<>();
        public List<SymptomEntry> symptomEntries = new ArrayList<>();
    }

    public static File exportAll(Context context) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(buildPayload(context));

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) {
            dir = context.getFilesDir();
        }
        File file = new File(dir, "petcare_backup.json");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    public static void exportAllToUri(Context context, Uri targetUri) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(buildPayload(context));
        try (java.io.OutputStream outputStream = context.getContentResolver().openOutputStream(targetUri, "w")) {
            if (outputStream == null) {
                throw new IllegalStateException("Unable to open export destination");
            }
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static List<PetBackup> buildPayload(Context context) {
        PetRepository repository = new PetRepository(context);
        List<PetBackup> payload = new ArrayList<>();
        List<Pet> pets = new ArrayList<>();
        pets.addAll(repository.getActivePets());
        pets.addAll(repository.getArchivedPets());

        for (Pet pet : pets) {
            PetBackup backup = new PetBackup();
            backup.pet = pet;
            backup.vetVisits = repository.getVetVisits(pet.id);
            backup.vaccinations = repository.getVaccinations(pet.id);
            backup.medications = repository.getMedications(pet.id);
            backup.feedingSchedules = repository.getFeedingSchedules(pet.id);
            backup.feedingLogs = repository.getFeedingLogs(pet.id);
            backup.medicationLogs = repository.getDb().medicationLogDao().getForPet(pet.id);
            backup.activitySessions = repository.getActivitySessions(pet.id);
            backup.weightEntries = repository.getWeightEntries(pet.id);
            backup.symptomEntries = repository.getSymptomEntries(pet.id);
            payload.add(backup);
        }
        return payload;
    }

    public static File getDefaultBackupFile(Context context) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) {
            dir = context.getFilesDir();
        }
        return new File(dir, "petcare_backup.json");
    }

    public static int importFromDefaultBackup(Context context) throws Exception {
        File file = getDefaultBackupFile(context);
        if (!file.exists()) {
            throw new IllegalStateException("Backup file not found: " + file.getAbsolutePath());
        }

        try (InputStream inputStream = new FileInputStream(file)) {
            return importJson(context, readText(inputStream));
        }
    }

    public static int importFromUri(Context context, Uri sourceUri) throws Exception {
        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri)) {
            if (inputStream == null) {
                throw new IllegalStateException("Unable to open import source");
            }
            return importJson(context, readText(inputStream));
        }
    }

    private static String readText(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private static int importJson(Context context, String json) {
        Gson gson = new GsonBuilder().create();
        Type type = new TypeToken<List<PetBackup>>() {}.getType();
        List<PetBackup> backups = gson.fromJson(json, type);

        PetRepository repository = new PetRepository(context);
        repository.getDb().clearAllTables();

        int importedPets = 0;
        if (backups == null) {
            return importedPets;
        }

        for (PetBackup backup : backups) {
            backup.pet.id = 0;
            long newPetId = repository.savePet(backup.pet);
            importedPets++;

            Map<Long, Long> visitIdMap = new HashMap<>();
            Map<Long, Long> medicationIdMap = new HashMap<>();
            Map<Long, Long> scheduleIdMap = new HashMap<>();

            for (VetVisit visit : backup.vetVisits) {
                long oldId = visit.id;
                visit.id = 0;
                visit.petId = newPetId;
                long newId = repository.getDb().vetVisitDao().insert(visit);
                visitIdMap.put(oldId, newId);
            }

            for (Vaccination vaccination : backup.vaccinations) {
                vaccination.id = 0;
                vaccination.petId = newPetId;
                repository.getDb().vaccinationDao().insert(vaccination);
            }

            for (Medication medication : backup.medications) {
                long oldId = medication.id;
                medication.id = 0;
                medication.petId = newPetId;
                if (medication.linkedVisitId != 0 && visitIdMap.containsKey(medication.linkedVisitId)) {
                    medication.linkedVisitId = visitIdMap.get(medication.linkedVisitId);
                }
                long newId = repository.getDb().medicationDao().insert(medication);
                medicationIdMap.put(oldId, newId);
            }

            for (FeedingSchedule schedule : backup.feedingSchedules) {
                long oldId = schedule.id;
                schedule.id = 0;
                schedule.petId = newPetId;
                long newId = repository.getDb().feedingScheduleDao().insert(schedule);
                scheduleIdMap.put(oldId, newId);
            }

            for (FeedingLog log : backup.feedingLogs) {
                log.id = 0;
                log.petId = newPetId;
                if (scheduleIdMap.containsKey(log.scheduleId)) {
                    log.scheduleId = scheduleIdMap.get(log.scheduleId);
                }
                repository.getDb().feedingLogDao().insert(log);
            }

            for (MedicationLog log : backup.medicationLogs) {
                log.id = 0;
                log.petId = newPetId;
                if (medicationIdMap.containsKey(log.medicationId)) {
                    log.medicationId = medicationIdMap.get(log.medicationId);
                }
                repository.getDb().medicationLogDao().insert(log);
            }

            for (ActivitySession session : backup.activitySessions) {
                session.id = 0;
                session.petId = newPetId;
                repository.getDb().activitySessionDao().insert(session);
            }

            for (WeightEntry entry : backup.weightEntries) {
                entry.id = 0;
                entry.petId = newPetId;
                repository.getDb().weightEntryDao().insert(entry);
            }

            for (SymptomEntry entry : backup.symptomEntries) {
                entry.id = 0;
                entry.petId = newPetId;
                if (entry.linkedVetVisitId != null && visitIdMap.containsKey(entry.linkedVetVisitId)) {
                    entry.linkedVetVisitId = visitIdMap.get(entry.linkedVetVisitId);
                }
                repository.getDb().symptomEntryDao().insert(entry);
            }
        }
        return importedPets;
    }
}