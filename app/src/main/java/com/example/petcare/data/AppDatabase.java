package com.example.petcare.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.petcare.data.dao.ActivitySessionDao;
import com.example.petcare.data.dao.FeedingLogDao;
import com.example.petcare.data.dao.FeedingScheduleDao;
import com.example.petcare.data.dao.MedicationDao;
import com.example.petcare.data.dao.MedicationLogDao;
import com.example.petcare.data.dao.PetDao;
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
                SymptomEntry.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

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

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "petcare.db")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }
}
