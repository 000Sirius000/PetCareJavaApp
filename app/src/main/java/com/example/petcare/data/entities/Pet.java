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

    /**
     * Kept with the old DB column name for migration compatibility.
     * The UI now treats this value as a DAILY goal in minutes.
     */
    public int weeklyActivityGoalMinutes;

    public Double minHealthyWeight;
    public Double maxHealthyWeight;

    public boolean archived;
    public long createdAt;
}
