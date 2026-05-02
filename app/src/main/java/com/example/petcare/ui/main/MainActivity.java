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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // no-op
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
                return true;
            }

            return false;
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                try {
                    Intent intent = new Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())
                    );
                    startActivity(intent);
                } catch (Exception ignored) {
                    // якщо на конкретному девайсі екран налаштувань не відкрився — не падаємо
                }
            }
        }
    }

    private void scheduleAllExistingReminders(PetRepository repository) {
        SharedPreferences prefs = getSharedPreferences("petcare_prefs", MODE_PRIVATE);
        int leadDays = prefs.getInt("vax_days", 30);

        try {
            for (Pet pet : repository.getActivePets()) {
                for (FeedingSchedule schedule : repository.getFeedingSchedules(pet.id)) {
                    ReminderScheduler.cancelFeeding(this, schedule.id);
                }

                for (Medication medication : repository.getMedications(pet.id)) {
                    if (!medication.archived) {
                        ReminderScheduler.scheduleMedication(this, medication);
                    }
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
