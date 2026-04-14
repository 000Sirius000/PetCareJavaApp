package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.petcare.data.entities.VetVisit;

import java.util.List;

@Dao
public interface VetVisitDao {
    @Query("SELECT * FROM vet_visits WHERE petId = :petId ORDER BY visitDateEpochMillis DESC")
    List<VetVisit> getForPet(long petId);

    @Query("SELECT * FROM vet_visits WHERE id = :id LIMIT 1")
    VetVisit getById(long id);

    @Insert
    long insert(VetVisit visit);

    @Update
    void update(VetVisit visit);

    @Delete
    void delete(VetVisit visit);
}
