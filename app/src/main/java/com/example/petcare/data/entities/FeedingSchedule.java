package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "feeding_schedules")
public class FeedingSchedule {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long petId;
    public String mealName;
    public int hourOfDay;
    public int minute;
    public String foodType;
    public String portion;
    public String portionUnit;
    public boolean snoozed;
    public long snoozeUntil;

    /** Date used by feeding charts when a schedule has not yet generated a completed feeding log. */
    public long createdAtEpochMillis;
}
