package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.petcare.data.entities.FeedingSchedule;

import java.util.List;

@Dao
public interface FeedingScheduleDao {
    @Query("SELECT * FROM feeding_schedules WHERE petId = :petId ORDER BY hourOfDay, minute")
    List<FeedingSchedule> getForPet(long petId);

    @Query("SELECT * FROM feeding_schedules")
    List<FeedingSchedule> getAll();

    @Query("SELECT * FROM feeding_schedules WHERE id = :id LIMIT 1")
    FeedingSchedule getById(long id);

    @Insert
    long insert(FeedingSchedule schedule);

    @Update
    void update(FeedingSchedule schedule);

    @Delete
    void delete(FeedingSchedule schedule);
}
