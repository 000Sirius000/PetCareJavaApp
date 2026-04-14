package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.petcare.data.entities.SymptomEntry;

import java.util.List;

@Dao
public interface SymptomEntryDao {
    @Query("SELECT * FROM symptom_entries WHERE petId = :petId ORDER BY recordedAt DESC")
    List<SymptomEntry> getForPet(long petId);

    @Query("SELECT * FROM symptom_entries WHERE id = :id LIMIT 1")
    SymptomEntry getById(long id);

    @Insert
    long insert(SymptomEntry entry);

    @Update
    void update(SymptomEntry entry);

    @Delete
    void delete(SymptomEntry entry);
}
