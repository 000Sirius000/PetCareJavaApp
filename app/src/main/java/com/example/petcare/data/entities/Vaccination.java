package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vaccinations")
public class Vaccination {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long petId;
    public String vaccineName;
    public long administeredAt;
    public Long nextDueAt;
    public String batchNumber;
}
