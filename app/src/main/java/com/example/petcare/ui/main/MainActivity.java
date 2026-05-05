package com.example.petcare.ui.main;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.databinding.ActivityMainBinding;
import com.example.petcare.reminders.ReminderScheduler;
import com.example.petcare.ui.dashboard.DashboardFragment;
import com.example.petcare.ui.pets.PetsFragment;
import com.example.petcare.ui.settings.SettingsActivity;
import com.example.petcare.util.ThemeUtils;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "petcare_prefs";
    private static final String KEY_PET_ICON = "pet_icon";

    private ActivityMainBinding binding;
    private String themeAtCreate;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyActivityTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        themeAtCreate = ThemeUtils.getSavedTheme(this);
        applyPreferredPetIcon();

        PetRepository repository = new PetRepository(this);
        repository.insertDemoDataIfEmpty();

        requestNotificationPermissionIfNeeded();
        requestExactAlarmPermissionIfNeeded();
        scheduleAllExistingReminders(repository);

        if (savedInstanceState == null) {
            showFragment(new DashboardFragment());
        }

        binding.mainBottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                showFragment(new DashboardFragment());
                return true;
            }
            if (id == R.id.nav_pets) {
                showFragment(new PetsFragment());
                return true;
            }
            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return false;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Changing between Black-yellow and Black-purple does not necessarily change
        // Android's night mode, so the system does not recreate MainActivity by itself.
        // Recreate when returning from Settings so all theme-dependent views
        // (bottom nav, buttons, icons, cards and custom charts) receive the new palette
        // immediately, without forcing the user to restart the app.
        String savedTheme = ThemeUtils.getSavedTheme(this);
        if (themeAtCreate != null && !savedTheme.equals(themeAtCreate)) {
            recreate();
            return;
        }

        if (binding != null) applyPreferredPetIcon();
    }

    private void applyPreferredPetIcon() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String icon = prefs.getString(KEY_PET_ICON, "dog_cat");
        int drawable = R.drawable.ic_pets_dog_cat;
        if ("dog".equals(icon)) drawable = R.drawable.ic_pets_dog;
        else if ("cat".equals(icon)) drawable = R.drawable.ic_pets_cat;
        binding.mainBottomNav.getMenu().findItem(R.id.nav_pets).setIcon(drawable);
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_container, fragment).commit();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) { }
            }
        }
    }

    private void scheduleAllExistingReminders(PetRepository repository) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int leadDays = prefs.getInt("vax_days", 30);
        try {
            for (Pet pet : repository.getActivePets()) {
                for (FeedingSchedule schedule : repository.getFeedingSchedules(pet.id)) {
                    ReminderScheduler.cancelFeeding(this, schedule.id);
                }
                for (Medication medication : repository.getMedications(pet.id)) {
                    if (!medication.archived && medication.nextReminderAt > 0L) ReminderScheduler.scheduleMedication(this, medication);
                }
                for (Vaccination vaccination : repository.getVaccinations(pet.id)) {
                    ReminderScheduler.scheduleVaccinationDue(this, vaccination, leadDays);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
