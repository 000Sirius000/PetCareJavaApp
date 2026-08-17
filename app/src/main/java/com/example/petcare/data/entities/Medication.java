package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "medications")
public class Medication {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long petId;
    public long linkedVisitId;
    public String medicationName;
    public String dosage;
    public String dosageUnit;
    public String frequencyType;
    public int frequencyIntervalDays;
    public long startDateEpochMillis;
    public Long endDateEpochMillis;
    public boolean reminderEnabled;
    public int reminderMinuteOfDay1;
    public int reminderMinuteOfDay2;
    public int reminderWeekdayMask;
    public long nextReminderAt;
    public boolean archived;
}
