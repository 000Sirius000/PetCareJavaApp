package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.petcare.data.entities.ReproductiveEvent;

import java.util.List;

@Dao
public interface ReproductiveEventDao {
    @Query("SELECT * FROM reproductive_events WHERE petId = :petId ORDER BY startDateEpochMillis DESC")
    List<ReproductiveEvent> getForPet(long petId);

    @Query("SELECT * FROM reproductive_events WHERE id = :id LIMIT 1")
    ReproductiveEvent getById(long id);

    @Insert
    long insert(ReproductiveEvent item);

    @Update
    void update(ReproductiveEvent item);

    @Delete
    void delete(ReproductiveEvent item);
}
