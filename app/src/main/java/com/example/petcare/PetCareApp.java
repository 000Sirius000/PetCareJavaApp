package com.example.petcare;

import android.app.Application;

import com.example.petcare.util.ThemeUtils;

public class PetCareApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeUtils.applySavedAppMode(this);
    }
}
