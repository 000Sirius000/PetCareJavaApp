package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "activity_sessions")
public class ActivitySession {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long petId;
    public String activityType;
    public int durationMinutes;
    public Double distance;
    public String distanceUnit;
    public long sessionDateEpochMillis;
    public String notes;
}
