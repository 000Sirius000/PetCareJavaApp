package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "weight_entries")
public class WeightEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long petId;
    public long measuredAt;
    public double weightValue;
    public String unit;
    public Double healthyMin;
    public Double healthyMax;
}
