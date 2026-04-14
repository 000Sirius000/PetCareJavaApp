package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.petcare.data.entities.ActivitySession;

import java.util.List;

@Dao
public interface ActivitySessionDao {
    @Query("SELECT * FROM activity_sessions WHERE petId = :petId ORDER BY sessionDateEpochMillis DESC")
    List<ActivitySession> getForPet(long petId);

    @Query("SELECT SUM(durationMinutes) FROM activity_sessions WHERE petId = :petId AND sessionDateEpochMillis >= :sinceEpoch")
    Integer getMinutesSince(long petId, long sinceEpoch);

    @Query("SELECT * FROM activity_sessions WHERE id = :id LIMIT 1")
    ActivitySession getById(long id);

    @Insert
    long insert(ActivitySession session);

    @Update
    void update(ActivitySession session);

    @Delete
    void delete(ActivitySession session);
}
