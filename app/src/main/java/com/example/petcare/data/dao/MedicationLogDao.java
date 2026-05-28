package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.petcare.data.entities.MedicationLog;

import java.util.List;

@Dao
public interface MedicationLogDao {
    @Query("SELECT * FROM medication_logs WHERE petId = :petId ORDER BY administeredAt DESC")
    List<MedicationLog> getForPet(long petId);

    @Query("SELECT * FROM medication_logs WHERE id = :id LIMIT 1")
    MedicationLog getById(long id);

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId AND sourceReminderAt = :sourceReminderAt LIMIT 1")
    MedicationLog getByMedicationAndSourceReminder(long medicationId, long sourceReminderAt);

    @Insert
    long insert(MedicationLog log);

    @Update
    void update(MedicationLog log);

    @Delete
    void delete(MedicationLog log);
}
