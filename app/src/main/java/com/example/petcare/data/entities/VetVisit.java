package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vet_visits")
public class VetVisit {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long petId;
    public long visitDateEpochMillis;
    public String clinicName;
    public String vetName;
    public String reason;
    public String diagnosisNotes;
    public String attachmentUri;
}
