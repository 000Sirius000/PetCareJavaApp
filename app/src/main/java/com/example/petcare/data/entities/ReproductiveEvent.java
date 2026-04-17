package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reproductive_events")
public class ReproductiveEvent {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long petId;
    public String eventType;
    public long startDateEpochMillis;
    public Long estimatedEndDateEpochMillis;
    public Long resolutionDateEpochMillis;
    public String clinic;
    public String symptomsObserved;
    public boolean vetConsulted;
    public String notes;
}
