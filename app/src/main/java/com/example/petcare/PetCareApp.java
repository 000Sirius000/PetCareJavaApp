package com.example.petcare;

import android.app.Application;

import com.example.petcare.data.AppDatabase;
import com.example.petcare.data.DefaultSymptomSeeder;
import com.example.petcare.util.ThemeUtils;

public class PetCareApp extends Application {
    private static PetCareApp instance;
    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        ThemeUtils.applySavedTheme(this);
        database = AppDatabase.getInstance(this);
        DefaultSymptomSeeder.seed(database);
    }

    public static PetCareApp getInstance() {
        return instance;
    }

    public AppDatabase getDatabase() {
        return database;
    }
}
