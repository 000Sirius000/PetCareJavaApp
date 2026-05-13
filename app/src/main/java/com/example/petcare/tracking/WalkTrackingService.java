package com.example.petcare.tracking;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.ui.petdetail.PetDetailActivity;

public class WalkTrackingService extends Service {
    public static final String ACTION_START = "com.example.petcare.action.WALK_START";
    public static final String ACTION_PAUSE = "com.example.petcare.action.WALK_PAUSE";
    public static final String ACTION_RESUME = "com.example.petcare.action.WALK_RESUME";
    public static final String ACTION_STOP = "com.example.petcare.action.WALK_STOP";
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_ACTIVITY_TYPE = "extra_activity_type";

    private static final String CHANNEL_ID = "petcare_walk_tracking";
    private static final int NOTIFICATION_ID = 42042;

    private static final float MAX_ACCEPTED_ACCURACY_METERS = 35f;
    private static final float MIN_SEGMENT_METERS = 2f;
    private static final float MAX_REASONABLE_SPEED_MPS = 7.5f; // fast run buffer, filters GPS jumps

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable notificationTicker = new Runnable() {
        @Override
        public void run() {
            WalkTrackingStore.State state = WalkTrackingStore.read(WalkTrackingService.this);
            if (state.active) {
                updateNotification(state);
                handler.postDelayed(this, 1000L);
            }
        }
    };

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) {
            WalkTrackingStore.State state = WalkTrackingStore.read(this);
            if (state.active) {
                startForegroundInternal(state);
                if (!state.paused) startLocationUpdates();
                return START_STICKY;
            }
            stopSelf();
            return START_NOT_STICKY;
        }

        switch (action) {
            case ACTION_START:
                handleStart(intent.getLongExtra(EXTRA_PET_ID, 0L), intent.getStringExtra(EXTRA_ACTIVITY_TYPE));
                return START_STICKY;
            case ACTION_PAUSE:
                handlePause();
                return START_STICKY;
            case ACTION_RESUME:
                handleResume();
                return START_STICKY;
            case ACTION_STOP:
                handleStopAndSave();
                return START_NOT_STICKY;
            default:
                return START_STICKY;
        }
    }

    private void handleStart(long petId, String requestedActivityType) {
        if (petId <= 0L) {
            stopSelf();
            return;
        }

        WalkTrackingStore.State state = WalkTrackingStore.read(this);
        if (state.active && state.petId == petId) {
            startForegroundInternal(state);
            if (!state.paused) startLocationUpdates();
            return;
        }

        if (state.active) return;

        WalkTrackingStore.start(this, petId, requestedActivityType);
        WalkTrackingStore.clearLastLocation(this);
        WalkTrackingStore.State newState = WalkTrackingStore.read(this);
        startForegroundInternal(newState);
        startLocationUpdates();
    }

    private void handlePause() {
        WalkTrackingStore.State state = WalkTrackingStore.read(this);
        if (!state.active || state.paused) return;

        WalkTrackingStore.pause(this);
        stopLocationUpdates();
        updateNotification(WalkTrackingStore.read(this));
    }

    private void handleResume() {
        WalkTrackingStore.State state = WalkTrackingStore.read(this);
        if (!state.active || !state.paused) return;

        WalkTrackingStore.resume(this);
        startLocationUpdates();
        updateNotification(WalkTrackingStore.read(this));
    }

    private void handleStopAndSave() {
        WalkTrackingStore.State state = WalkTrackingStore.read(this);
        stopLocationUpdates();
        handler.removeCallbacks(notificationTicker);

        if (state.active) {
            long elapsedMs = state.elapsedNow();

            ActivitySession session = new ActivitySession();
            session.petId = state.petId;
            session.activityType = WalkTrackingStore.normalizeActivityType(state.activityType);
            session.durationMinutes = Math.max(1, (int) Math.round(elapsedMs / 60000d));
            if (WalkTrackingStore.supportsLiveDistance(state.activityType) && state.distanceMeters > 0d) {
                session.distance = state.distanceMeters / 1000d;
                session.distanceUnit = "km";
            } else {
                session.distance = null;
                session.distanceUnit = null;
            }
            session.sessionDateEpochMillis = state.startedAt > 0L ? state.startedAt : System.currentTimeMillis();
            session.notes = "Tracked live " + WalkTrackingStore.normalizeActivityType(state.activityType).toLowerCase(java.util.Locale.ROOT);
            new PetRepository(this).getDb().activitySessionDao().insert(session);
        }

        WalkTrackingStore.clear(this);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void startForegroundInternal(WalkTrackingStore.State state) {
        ensureChannel();
        startForeground(NOTIFICATION_ID, buildNotification(state));
        handler.removeCallbacks(notificationTicker);
        handler.post(notificationTicker);
    }

    private void updateNotification(WalkTrackingStore.State state) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(state));
        }
    }

    private android.app.Notification buildNotification(WalkTrackingStore.State state) {
        Pet pet = new PetRepository(this).getPet(state.petId);
        String petName = pet == null || pet.name == null || pet.name.trim().isEmpty() ? "Pet" : pet.name.trim();
        String activityName = WalkTrackingStore.normalizeActivityType(state.activityType);
        String title = state.paused ? activityName + " paused" : "Tracking " + activityName.toLowerCase(java.util.Locale.ROOT);
        String text = petName + " • " + WalkTrackingStore.formatElapsed(state.elapsedNow());
        if (WalkTrackingStore.supportsLiveDistance(activityName)) {
            text += " • " + WalkTrackingStore.formatDistanceKm(state.distanceMeters);
        }

        Intent openIntent = new Intent(this, PetDetailActivity.class);
        openIntent.putExtra(PetDetailActivity.EXTRA_PET_ID, state.petId);
        openIntent.putExtra(PetDetailActivity.EXTRA_INITIAL_TAB, PetDetailActivity.TAB_ACTIVITY);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                11,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent pauseResumePendingIntent = PendingIntent.getService(
                this,
                12,
                new Intent(this, WalkTrackingService.class).setAction(state.paused ? ACTION_RESUME : ACTION_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent finishPendingIntent = PendingIntent.getService(
                this,
                13,
                new Intent(this, WalkTrackingService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(openPendingIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(0, state.paused ? "Resume" : "Pause", pauseResumePendingIntent)
                .addAction(0, "Finish", finishPendingIntent)
                .build();
    }

    private void startLocationUpdates() {
        if (locationManager == null || !hasLocationPermission()) return;

        stopLocationUpdates();
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                handleLocation(location);
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onProviderDisabled(String provider) { }
        };

        try {
            // Use GPS as the only distance source. Network provider is intentionally not used for
            // distance accumulation because mixed providers can duplicate and inflate segments.
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        3000L,
                        3f,
                        locationListener,
                        Looper.getMainLooper()
                );
            }
        } catch (SecurityException ignored) {
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void handleLocation(Location location) {
        if (location == null) return;
        if (location.hasAccuracy() && location.getAccuracy() > MAX_ACCEPTED_ACCURACY_METERS) return;

        WalkTrackingStore.State state = WalkTrackingStore.read(this);
        if (!state.active || state.paused) return;
        if (!WalkTrackingStore.supportsLiveDistance(state.activityType)) return;

        if (state.hasLastLocation) {
            float[] result = new float[1];
            Location.distanceBetween(
                    state.lastLat,
                    state.lastLon,
                    location.getLatitude(),
                    location.getLongitude(),
                    result
            );

            float segmentMeters = result[0];
            long dtMs = Math.max(1L, location.getTime() > 0L ? location.getTime() - state.lastLocationTime : 3000L);
            double speedMps = segmentMeters / (dtMs / 1000d);

            if (segmentMeters >= MIN_SEGMENT_METERS && speedMps <= MAX_REASONABLE_SPEED_MPS) {
                WalkTrackingStore.addDistanceMeters(this, segmentMeters);
            }
        }

        WalkTrackingStore.setLastLocation(
                this,
                location.getLatitude(),
                location.getLongitude(),
                location.getTime() > 0L ? location.getTime() : System.currentTimeMillis()
        );
    }

    private void stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException ignored) {
            }
        }
        locationListener = null;
        WalkTrackingStore.clearLastLocation(this);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void ensureChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Live walk tracking",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Shows the currently running walk tracker");
        manager.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        handler.removeCallbacks(notificationTicker);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
