package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.petcare.data.entities.WeightEntry;

import java.util.List;

@Dao
public interface WeightEntryDao {
    @Query("SELECT * FROM weight_entries WHERE petId = :petId ORDER BY measuredAt DESC")
    List<WeightEntry> getForPet(long petId);

    @Query("SELECT * FROM weight_entries WHERE id = :id LIMIT 1")
    WeightEntry getById(long id);

    @Insert
    long insert(WeightEntry entry);

    @Update
    void update(WeightEntry entry);

    @Delete
    void delete(WeightEntry entry);
}
