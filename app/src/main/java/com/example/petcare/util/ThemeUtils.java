package com.example.petcare.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeUtils {
    public static final String PREFS = "petcare_prefs";
    public static final String KEY_THEME = "theme_mode";

    private ThemeUtils() {
    }

    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String mode = prefs.getString(KEY_THEME, "system");
        AppCompatDelegate.setDefaultNightMode(resolveMode(mode));
    }

    public static void saveAndApplyTheme(Context context, String mode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, mode)
                .apply();
        AppCompatDelegate.setDefaultNightMode(resolveMode(mode));
    }

    public static String getSavedTheme(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_THEME, "system");
    }

    private static int resolveMode(String mode) {
        if ("light".equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if ("dark".equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
