package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "symptom_entries")
public class SymptomEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long petId;
    public long recordedAt;
    public String tagsCsv;
    public String severity;
    public String notes;
    public Long linkedVetVisitId;
}
