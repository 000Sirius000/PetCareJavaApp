package com.example.petcare.tracking;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public final class WalkTrackingStore {
    private static final String PREFS = "walk_tracker_prefs";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_PET_ID = "pet_id";
    private static final String KEY_STARTED_AT = "started_at";
    private static final String KEY_LAST_RESUME_AT = "last_resume_at";
    private static final String KEY_ACCUMULATED_MS = "accumulated_ms";
    private static final String KEY_PAUSED = "paused";
    private static final String KEY_DISTANCE_METERS = "distance_meters";
    private static final String KEY_HAS_LAST_LOCATION = "has_last_location";
    private static final String KEY_LAST_LAT = "last_lat";
    private static final String KEY_LAST_LON = "last_lon";

    private WalkTrackingStore() {
    }

    public static State read(Context context) {
        SharedPreferences prefs = prefs(context);
        State state = new State();
        state.active = prefs.getBoolean(KEY_ACTIVE, false);
        state.petId = prefs.getLong(KEY_PET_ID, 0L);
        state.startedAt = prefs.getLong(KEY_STARTED_AT, 0L);
        state.lastResumeAt = prefs.getLong(KEY_LAST_RESUME_AT, 0L);
        state.accumulatedMs = prefs.getLong(KEY_ACCUMULATED_MS, 0L);
        state.paused = prefs.getBoolean(KEY_PAUSED, false);
        state.distanceMeters = Double.longBitsToDouble(prefs.getLong(KEY_DISTANCE_METERS, Double.doubleToRawLongBits(0d)));
        state.hasLastLocation = prefs.getBoolean(KEY_HAS_LAST_LOCATION, false);
        state.lastLat = Double.longBitsToDouble(prefs.getLong(KEY_LAST_LAT, Double.doubleToRawLongBits(0d)));
        state.lastLon = Double.longBitsToDouble(prefs.getLong(KEY_LAST_LON, Double.doubleToRawLongBits(0d)));
        return state;
    }

    public static void start(Context context, long petId) {
        long now = System.currentTimeMillis();
        prefs(context).edit()
                .putBoolean(KEY_ACTIVE, true)
                .putLong(KEY_PET_ID, petId)
                .putLong(KEY_STARTED_AT, now)
                .putLong(KEY_LAST_RESUME_AT, now)
                .putLong(KEY_ACCUMULATED_MS, 0L)
                .putBoolean(KEY_PAUSED, false)
                .putLong(KEY_DISTANCE_METERS, Double.doubleToRawLongBits(0d))
                .putBoolean(KEY_HAS_LAST_LOCATION, false)
                .apply();
    }

    public static void pause(Context context) {
        State state = read(context);
        if (!state.active || state.paused) {
            return;
        }
        long now = System.currentTimeMillis();
        long accumulated = state.accumulatedMs + Math.max(0L, now - state.lastResumeAt);
        prefs(context).edit()
                .putLong(KEY_ACCUMULATED_MS, accumulated)
                .putBoolean(KEY_PAUSED, true)
                .putBoolean(KEY_HAS_LAST_LOCATION, false)
                .apply();
    }

    public static void resume(Context context) {
        State state = read(context);
        if (!state.active || !state.paused) {
            return;
        }
        long now = System.currentTimeMillis();
        prefs(context).edit()
                .putLong(KEY_LAST_RESUME_AT, now)
                .putBoolean(KEY_PAUSED, false)
                .putBoolean(KEY_HAS_LAST_LOCATION, false)
                .apply();
    }

    public static void addDistanceMeters(Context context, double meters) {
        State state = read(context);
        prefs(context).edit()
                .putLong(KEY_DISTANCE_METERS, Double.doubleToRawLongBits(Math.max(0d, state.distanceMeters + Math.max(0d, meters))))
                .apply();
    }

    public static void setLastLocation(Context context, double latitude, double longitude) {
        prefs(context).edit()
                .putBoolean(KEY_HAS_LAST_LOCATION, true)
                .putLong(KEY_LAST_LAT, Double.doubleToRawLongBits(latitude))
                .putLong(KEY_LAST_LON, Double.doubleToRawLongBits(longitude))
                .apply();
    }

    public static void clearLastLocation(Context context) {
        prefs(context).edit()
                .putBoolean(KEY_HAS_LAST_LOCATION, false)
                .apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    public static String formatElapsed(long elapsedMs) {
        long totalSeconds = Math.max(0L, elapsedMs / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String formatDistanceKm(double meters) {
        return String.format(Locale.getDefault(), "%.2f km", Math.max(0d, meters) / 1000d);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static final class State {
        public boolean active;
        public long petId;
        public long startedAt;
        public long lastResumeAt;
        public long accumulatedMs;
        public boolean paused;
        public double distanceMeters;
        public boolean hasLastLocation;
        public double lastLat;
        public double lastLon;

        public long elapsedNow() {
            if (!active) {
                return 0L;
            }
            if (paused) {
                return accumulatedMs;
            }
            return accumulatedMs + Math.max(0L, System.currentTimeMillis() - lastResumeAt);
        }
    }
}
