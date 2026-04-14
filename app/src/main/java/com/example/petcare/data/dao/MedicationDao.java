package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.petcare.data.entities.Medication;

import java.util.List;

@Dao
public interface MedicationDao {
    @Query("SELECT * FROM medications WHERE petId = :petId ORDER BY archived ASC, medicationName ASC")
    List<Medication> getForPet(long petId);

    @Query("SELECT * FROM medications WHERE archived = 0")
    List<Medication> getActive();

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    Medication getById(long id);

    @Insert
    long insert(Medication medication);

    @Update
    void update(Medication medication);

    @Delete
    void delete(Medication medication);
}
