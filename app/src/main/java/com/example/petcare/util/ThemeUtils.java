package com.example.petcare.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.petcare.R;

public final class ThemeUtils {
    private static final String PREFS = "petcare_prefs";
    private static final String KEY_THEME = "theme";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_BLACK_YELLOW = "black_yellow";
    public static final String THEME_BLACK_PURPLE = "black_purple";

    private ThemeUtils() { }

    public static void applySavedAppMode(Context context) {
        String mode = getSavedTheme(context);
        if (THEME_LIGHT.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (THEME_BLACK_YELLOW.equals(mode) || THEME_BLACK_PURPLE.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public static void applyActivityTheme(Activity activity) {
        String mode = getSavedTheme(activity);
        if (THEME_LIGHT.equals(mode)) {
            activity.setTheme(R.style.Theme_PetCare_Light);
        } else if (THEME_BLACK_PURPLE.equals(mode)) {
            activity.setTheme(R.style.Theme_PetCare_BlackPurple);
        } else if (THEME_BLACK_YELLOW.equals(mode)) {
            activity.setTheme(R.style.Theme_PetCare_BlackYellow);
        } else {
            activity.setTheme(R.style.Theme_PetCare);
        }
    }

    public static String getSavedTheme(Context context) {
        return normalizeTheme(prefs(context).getString(KEY_THEME, THEME_SYSTEM));
    }

    public static void saveAndApplyTheme(Activity activity, String mode) {
        prefs(activity).edit().putString(KEY_THEME, normalizeTheme(mode)).apply();
        applySavedAppMode(activity);
        activity.recreate();
    }


    public static String normalizeTheme(String mode) {
        if (mode == null) return THEME_SYSTEM;
        String value = mode.trim().toLowerCase(java.util.Locale.ROOT);
        switch (value) {
            case "black-purple":
            case "black purple":
            case "purple":
            case "black_purple":
                return THEME_BLACK_PURPLE;
            case "black-yellow":
            case "black yellow":
            case "yellow":
            case "dark":
            case "black_yellow":
                return THEME_BLACK_YELLOW;
            case "light":
                return THEME_LIGHT;
            default:
                return THEME_SYSTEM;
        }
    }

    public static int getAccentColor(Context context) {
        String mode = getSavedTheme(context);
        if (THEME_BLACK_PURPLE.equals(mode)) {
            return 0xFF7E57C2;
        }
        if (THEME_BLACK_YELLOW.equals(mode)) {
            return 0xFFFFD600;
        }

        TypedValue value = new TypedValue();
        boolean resolved = context.getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorPrimary,
                value,
                true
        );
        return resolved ? value.data : 0xFF7E57C2;
    }

    public static int getActivityDistanceColor(Context context) {
        return 0xFF2E7D32;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
