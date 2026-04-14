package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.petcare.data.entities.Vaccination;

import java.util.List;

@Dao
public interface VaccinationDao {
    @Query("SELECT * FROM vaccinations WHERE petId = :petId ORDER BY administeredAt DESC")
    List<Vaccination> getForPet(long petId);

    @Query("SELECT * FROM vaccinations WHERE nextDueAt IS NOT NULL AND nextDueAt <= :untilEpoch")
    List<Vaccination> getDueBefore(long untilEpoch);

    @Query("SELECT * FROM vaccinations WHERE id = :id LIMIT 1")
    Vaccination getById(long id);

    @Insert
    long insert(Vaccination vaccination);

    @Update
    void update(Vaccination vaccination);

    @Delete
    void delete(Vaccination vaccination);
}
