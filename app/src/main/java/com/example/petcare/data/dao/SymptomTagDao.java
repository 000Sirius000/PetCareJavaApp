package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.petcare.data.entities.SymptomTag;

import java.util.List;

@Dao
public interface SymptomTagDao {
    @Query("SELECT * FROM symptom_tags ORDER BY bodySystem, name")
    List<SymptomTag> getAll();

    @Query("SELECT COUNT(*) FROM symptom_tags")
    int count();

    @Insert
    long insert(SymptomTag tag);
}
