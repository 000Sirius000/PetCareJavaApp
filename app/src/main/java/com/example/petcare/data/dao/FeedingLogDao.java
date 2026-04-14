package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.petcare.data.entities.FeedingLog;

import java.util.List;

@Dao
public interface FeedingLogDao {
    @Query("SELECT * FROM feeding_logs WHERE petId = :petId ORDER BY completedAt DESC")
    List<FeedingLog> getForPet(long petId);

    @Insert
    long insert(FeedingLog log);
}
