package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "medication_logs")
public class MedicationLog {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long petId;
    public long medicationId;
    public long administeredAt;
    public long sourceReminderAt;
    public String medicationName;
    public String dosage;
    public String markedBy;
    public boolean missed;
}
