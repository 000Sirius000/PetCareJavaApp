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
        entities = {
                Pet.class,
                VetVisit.class,
                Vaccination.class,
                Medication.class,
                FeedingSchedule.class,
                FeedingLog.class,
                MedicationLog.class,
                ActivitySession.class,
                WeightEntry.class,
                SymptomTag.class,
                SymptomEntry.class,
                ReproductiveEvent.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    /**
     * Preserves existing user data from the old schema and only adds the new table.
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `reproductive_events` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`petId` INTEGER NOT NULL, " +
                    "`eventType` TEXT, " +
                    "`startDateEpochMillis` INTEGER NOT NULL, " +
                    "`estimatedEndDateEpochMillis` INTEGER, " +
                    "`resolutionDateEpochMillis` INTEGER, " +
                    "`clinic` TEXT, " +
                    "`symptomsObserved` TEXT, " +
                    "`vetConsulted` INTEGER NOT NULL, " +
                    "`notes` TEXT)");
        }
    };

    /**
     * UI/feature update:
     * - healthy min/max weight are now stored on the pet profile;
     * - feeding log rows now carry the food type used by the stacked chart.
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `pets` ADD COLUMN `minHealthyWeight` REAL");
            db.execSQL("ALTER TABLE `pets` ADD COLUMN `maxHealthyWeight` REAL");
            db.execSQL("ALTER TABLE `feeding_logs` ADD COLUMN `foodType` TEXT");
            db.execSQL("UPDATE `feeding_logs` SET `foodType` = 'Dry food' WHERE `foodType` IS NULL OR TRIM(`foodType`) = ''");
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
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "petcare.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }
}
