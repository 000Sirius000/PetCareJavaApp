package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.petcare.data.entities.MedicationLog;

import java.util.List;

@Dao
public interface MedicationLogDao {
    @Query("SELECT * FROM medication_logs WHERE petId = :petId ORDER BY administeredAt DESC")
    List<MedicationLog> getForPet(long petId);

    @Insert
    long insert(MedicationLog log);
}
