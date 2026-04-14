package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "feeding_logs")
public class FeedingLog {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long petId;
    public long scheduleId;
    public long completedAt;
    public String mealName;
    public String portion;
}
