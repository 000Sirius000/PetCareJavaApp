package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "symptom_tags")
public class SymptomTag {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String bodySystem;
    public String name;
    public boolean customTag;
}
