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

    /** Kept with the old DB column name for migration compatibility; UI treats it as daily minutes. */
    public int weeklyActivityGoalMinutes;

    public Double minHealthyWeight;
    public Double maxHealthyWeight;

    public String internationalPetPassport;
    public String nationalPetPassport;
    public String microchipCode;
    public String microchipImplantationDate;

    public boolean archived;
    public long createdAt;
}
