package com.example.petcare.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.petcare.data.dao.ActivitySessionDao;
import com.example.petcare.data.dao.FeedingLogDao;
import com.example.petcare.data.dao.FeedingScheduleDao;
import com.example.petcare.data.dao.MedicationDao;
import com.example.petcare.data.dao.MedicationLogDao;
import com.example.petcare.data.dao.PetDao;
import com.example.petcare.data.dao.ReproductiveEventDao;
import com.example.petcare.data.dao.SymptomEntryDao;
import com.example.petcare.data.dao.SymptomTagDao;
import com.example.petcare.data.dao.VaccinationDao;
import com.example.petcare.data.dao.VetVisitDao;
import com.example.petcare.data.dao.WeightEntryDao;
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

@Database(
        entities = { Pet.class, VetVisit.class, Vaccination.class, Medication.class, FeedingSchedule.class,
                FeedingLog.class, MedicationLog.class, ActivitySession.class, WeightEntry.class,
                SymptomTag.class, SymptomEntry.class, ReproductiveEvent.class },
        version = 6,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `reproductive_events` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`petId` INTEGER NOT NULL, `eventType` TEXT, `startDateEpochMillis` INTEGER NOT NULL, " +
                    "`estimatedEndDateEpochMillis` INTEGER, `resolutionDateEpochMillis` INTEGER, `clinic` TEXT, " +
                    "`symptomsObserved` TEXT, `vetConsulted` INTEGER NOT NULL, `notes` TEXT)");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `pets` ADD COLUMN `minHealthyWeight` REAL");
            db.execSQL("ALTER TABLE `pets` ADD COLUMN `maxHealthyWeight` REAL");
            db.execSQL("ALTER TABLE `feeding_logs` ADD COLUMN `foodType` TEXT");
            db.execSQL("UPDATE `feeding_logs` SET `foodType` = 'Dry food' WHERE `foodType` IS NULL OR TRIM(`foodType`) = ''");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `pets` ADD COLUMN `internationalPetPassport` TEXT");
            db.execSQL("ALTER TABLE `pets` ADD COLUMN `nationalPetPassport` TEXT");
            db.execSQL("ALTER TABLE `pets` ADD COLUMN `microchipCode` TEXT");
            db.execSQL("ALTER TABLE `pets` ADD COLUMN `microchipImplantationDate` TEXT");
            db.execSQL("ALTER TABLE `feeding_schedules` ADD COLUMN `createdAtEpochMillis` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE `feeding_schedules` SET `createdAtEpochMillis` = strftime('%s','now') * 1000 WHERE `createdAtEpochMillis` = 0");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `medication_logs` ADD COLUMN `sourceReminderAt` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `medication_logs` ADD COLUMN `medicationName` TEXT");
            db.execSQL("ALTER TABLE `medication_logs` ADD COLUMN `dosage` TEXT");
            db.execSQL("UPDATE `medication_logs` SET `medicationName` = (" +
                    "SELECT `medicationName` FROM `medications` WHERE `medications`.`id` = `medication_logs`.`medicationId`" +
                    ") WHERE `medicationName` IS NULL OR TRIM(`medicationName`) = ''");
            db.execSQL("UPDATE `medication_logs` SET `dosage` = TRIM(COALESCE((" +
                    "SELECT `dosage` FROM `medications` WHERE `medications`.`id` = `medication_logs`.`medicationId`" +
                    "), '') || ' ' || COALESCE((" +
                    "SELECT `dosageUnit` FROM `medications` WHERE `medications`.`id` = `medication_logs`.`medicationId`" +
                    "), '')) WHERE `dosage` IS NULL OR TRIM(`dosage`) = ''");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `medications` ADD COLUMN `reminderEnabled` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `medications` ADD COLUMN `reminderMinuteOfDay1` INTEGER NOT NULL DEFAULT 540");
            db.execSQL("ALTER TABLE `medications` ADD COLUMN `reminderMinuteOfDay2` INTEGER NOT NULL DEFAULT 1260");
            db.execSQL("ALTER TABLE `medications` ADD COLUMN `reminderWeekdayMask` INTEGER NOT NULL DEFAULT 127");
            db.execSQL("UPDATE `medications` SET `reminderEnabled` = CASE " +
                    "WHEN `nextReminderAt` > 0 AND `archived` = 0 THEN 1 ELSE 0 END");
            db.execSQL("UPDATE `medications` SET `reminderMinuteOfDay1` = " +
                    "CAST(strftime('%H', `nextReminderAt` / 1000, 'unixepoch', 'localtime') AS INTEGER) * 60 + " +
                    "CAST(strftime('%M', `nextReminderAt` / 1000, 'unixepoch', 'localtime') AS INTEGER) " +
                    "WHERE `nextReminderAt` > 0");
            db.execSQL("UPDATE `medications` SET `reminderMinuteOfDay2` = " +
                    "(`reminderMinuteOfDay1` + 720) % 1440");
        }
    };

    public abstract PetDao petDao();
    public abstract VetVisitDao vetVisitDao();
    public abstract VaccinationDao vaccinationDao();
    public abstract MedicationDao medicationDao();
    public abstract FeedingScheduleDao feedingScheduleDao();
    public abstract FeedingLogDao feedingLogDao();
    public abstract MedicationLogDao medicationLogDao();
    public abstract ActivitySessionDao activitySessionDao();
    public abstract WeightEntryDao weightEntryDao();
    public abstract SymptomTagDao symptomTagDao();
    public abstract SymptomEntryDao symptomEntryDao();
    public abstract ReproductiveEventDao reproductiveEventDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "petcare.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }
}
