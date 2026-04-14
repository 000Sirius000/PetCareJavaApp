package com.example.petcare.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.petcare.data.entities.Pet;

import java.util.List;

@Dao
public interface PetDao {
    @Query("SELECT * FROM pets WHERE archived = 0 ORDER BY name ASC")
    List<Pet> getActivePets();

    @Query("SELECT * FROM pets WHERE archived = 1 ORDER BY name ASC")
    List<Pet> getArchivedPets();

    @Query("SELECT * FROM pets WHERE id = :petId LIMIT 1")
    Pet getById(long petId);

    @Insert
    long insert(Pet pet);

    @Update
    void update(Pet pet);

    @Delete
    void delete(Pet pet);

    @Query("UPDATE pets SET archived = 1 WHERE id = :petId")
    void archive(long petId);

    @Query("UPDATE pets SET archived = 0 WHERE id = :petId")
    void recover(long petId);
}
