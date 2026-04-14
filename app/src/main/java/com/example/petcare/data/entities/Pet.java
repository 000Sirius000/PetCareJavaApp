package com.example.petcare.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pets")
public class Pet {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String species;
    public String breed;
    public String birthInfo;
    public String sex;
    public String photoUri;
    public int weeklyActivityGoalMinutes;
    public boolean archived;
    public long createdAt;
}
