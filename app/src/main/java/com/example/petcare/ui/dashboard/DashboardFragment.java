package com.example.petcare.ui.dashboard;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.databinding.FragmentDashboardBinding;
import com.example.petcare.tracking.WalkTrackingService;
import com.example.petcare.tracking.WalkTrackingStore;
import com.example.petcare.ui.petdetail.PetDetailActivity;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;

import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {
    private static final String PREFS = "petcare_prefs";

    private FragmentDashboardBinding binding;
    private PetRepository repository;
    private Pet selectedPet;

    private final Handler trackerUiHandler = new Handler(Looper.getMainLooper());
    private long finishedElapsedMs = -1L;
    private double finishedDistanceMeters = 0d;
    private String finishedActivityType = "Walk";
    private long showFinishedUntil = 0L;

    private final Runnable trackerUiTicker = new Runnable() {
        @Override public void run() {
            if (binding == null) return;
            refreshTrackerCard();
            trackerUiHandler.postDelayed(this, 1000L);
        }
    };

    private final Runnable resetAfterFinish = new Runnable() {
        @Override public void run() {
            finishedElapsedMs = -1L;
            finishedDistanceMeters = 0d;
            showFinishedUntil = 0L;
            refreshTrackerCard();
            reload();
        }
    };

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onLocationPermissionResult);

    private String pendingStartActivityType = "Walk";

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());

        binding.dashboardSwipe.setOnRefreshListener(this::reload);
        binding.petSummaryCard.setOnClickListener(v -> openPetDetail(PetDetailActivity.TAB_WEIGHT));
        binding.activitySummaryCard.setOnClickListener(v -> openPetDetail(PetDetailActivity.TAB_ACTIVITY));
        binding.remindersCard.setOnClickListener(v -> openPetDetail(PetDetailActivity.TAB_HEALTH));

        binding.buttonStartWalk.setOnClickListener(v -> startTrackingFromButton(binding.buttonStartWalk));
        binding.buttonStartRun.setOnClickListener(v -> startTrackingFromButton(binding.buttonStartRun));
        binding.buttonStartPlay.setOnClickListener(v -> startTrackingFromButton(binding.buttonStartPlay));
        binding.buttonPauseResumeWalk.setOnClickListener(v -> togglePauseResume());
        binding.buttonFinishWalk.setOnClickListener(v -> finishWalkTracking());

        reload();
        return binding.getRoot();
    }

    @Override public void onResume() {
        super.onResume();
        trackerUiHandler.removeCallbacks(trackerUiTicker);
        trackerUiHandler.post(trackerUiTicker);
        reload();
    }

    @Override public void onPause() {
        trackerUiHandler.removeCallbacks(trackerUiTicker);
        trackerUiHandler.removeCallbacks(resetAfterFinish);
        super.onPause();
    }

    @Override public void onDestroyView() {
        trackerUiHandler.removeCallbacks(trackerUiTicker);
        trackerUiHandler.removeCallbacks(resetAfterFinish);
        binding = null;
        super.onDestroyView();
    }

    private void reload() {
        selectedPet = repository.getSelectedPet();
        if (selectedPet == null) {
            binding.dashboardContent.setVisibility(View.GONE);
            binding.dashboardEmpty.setVisibility(View.VISIBLE);
            binding.dashboardSwipe.setRefreshing(false);
            return;
        }

        binding.dashboardContent.setVisibility(View.VISIBLE);
        binding.dashboardEmpty.setVisibility(View.GONE);
        binding.petName.setText(selectedPet.name);
        binding.petSubtitle.setText(selectedPet.species + " • " + nullable(selectedPet.breed));
        binding.petWeight.setText(repository.getLastWeightSummary(selectedPet));
        binding.weightWarning.setVisibility(repository.isLatestWeightOutOfRange(selectedPet) ? View.VISIBLE : View.GONE);

        if (!TextUtils.isEmpty(selectedPet.photoUri)) binding.petPhoto.setImageURI(Uri.parse(selectedPet.photoUri));
        else binding.petPhoto.setImageResource(android.R.drawable.ic_menu_gallery);

        binding.activityRows.setText(repository.getTodayActivitySummaryText(selectedPet.id));
        if (selectedPet.weeklyActivityGoalMinutes > 0) {
            int percent = repository.getDailyActivityProgressPercent(selectedPet.id, selectedPet.weeklyActivityGoalMinutes);
            binding.activityProgress.setVisibility(View.VISIBLE);
            binding.activityPercent.setVisibility(View.VISIBLE);
            binding.activityProgress.setProgress(percent);
            binding.activityPercent.setText(percent + "%");
        } else {
            binding.activityProgress.setVisibility(View.GONE);
            binding.activityPercent.setVisibility(View.GONE);
        }

        renderUpcomingReminders(selectedPet.id);
        setupTrackerButtons();
        refreshTrackerCard();
        binding.dashboardSwipe.setRefreshing(false);
    }

    private void renderUpcomingReminders(long petId) {
        binding.reminderRowsContainer.removeAllViews();
        List<Object> reminders = repository.getUpcomingReminderPreview(petId);
        if (reminders.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No upcoming reminders.");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.pet_text_secondary));
            binding.reminderRowsContainer.addView(empty);
            return;
        }
        for (Object item : reminders) binding.reminderRowsContainer.addView(reminderRow(item));
    }

    private View reminderRow(Object item) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setOnClickListener(v -> openPetDetail(PetDetailActivity.TAB_HEALTH));

        CheckBox checkBox = new CheckBox(requireContext());
        checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(ThemeUtils.getAccentColor(requireContext())));
        checkBox.setOnClickListener(v -> completeReminder(item));
        row.addView(checkBox);

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(requireContext());
        title.setText(reminderTitle(item));
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.pet_text_primary));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView date = new TextView(requireContext());
        date.setText(reminderDate(item));
        date.setTextColor(FormatUtils.isNearMidnight(repository.reminderTime(item))
                ? ContextCompat.getColor(requireContext(), R.color.pet_warning)
                : ContextCompat.getColor(requireContext(), R.color.pet_text_secondary));
        body.addView(title);
        body.addView(date);
        row.addView(body, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView chevron = new TextView(requireContext());
        chevron.setText("›");
        chevron.setTextSize(22);
        chevron.setTextColor(ContextCompat.getColor(requireContext(), R.color.pet_text_secondary));
        row.addView(chevron);
        return row;
    }

    private String reminderTitle(Object item) {
        if (item instanceof Medication) return safeName(((Medication) item).medicationName, "Medication");
        if (item instanceof Vaccination) return safeName(((Vaccination) item).vaccineName, "Vaccination");
        return "Reminder";
    }

    private String reminderDate(Object item) {
        long time = repository.reminderTime(item);
        String text = item instanceof Vaccination ? FormatUtils.humanDate(time) : FormatUtils.humanDateTime(time);
        if (FormatUtils.isNearMidnight(time)) text += "  ⚠ check stored time";
        return text;
    }

    private void completeReminder(Object item) {
        if (item instanceof Medication) {
            Medication medication = (Medication) item;
            repository.logMedication(selectedPet.id, medication.id, false);
            medication.nextReminderAt = System.currentTimeMillis() + Math.max(1, medication.frequencyIntervalDays) * 24L * 60 * 60 * 1000;
            repository.getDb().medicationDao().update(medication);
            toast("Medication completed");
        } else if (item instanceof Vaccination) {
            Vaccination vaccination = (Vaccination) item;
            vaccination.administeredAt = System.currentTimeMillis();
            vaccination.nextDueAt = null;
            repository.getDb().vaccinationDao().update(vaccination);
            toast("Vaccination completed");
        }
        reload();
    }

    private void setupTrackerButtons() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("tracking_enabled", true);
        binding.trackerCard.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) return;
        bindLaunchButton(binding.buttonStartWalk, prefs.getString("tracking_button_1", "Walk"));
        bindLaunchButton(binding.buttonStartRun, prefs.getString("tracking_button_2", "Run"));
        bindLaunchButton(binding.buttonStartPlay, prefs.getString("tracking_button_3", "Play"));
    }

    private void bindLaunchButton(android.widget.Button button, String type) {
        String normalized = WalkTrackingStore.normalizeActivityType(type);
        button.setText("▶ " + normalized);
        button.setTag(normalized);
    }

    private void startTrackingFromButton(View button) {
        Object tag = button.getTag();
        startActivityTracking(tag == null ? "Walk" : String.valueOf(tag));
    }

    private void openPetDetail(String tab) {
        if (selectedPet == null) return;
        Intent intent = new Intent(requireContext(), PetDetailActivity.class);
        intent.putExtra(PetDetailActivity.EXTRA_PET_ID, selectedPet.id);
        intent.putExtra(PetDetailActivity.EXTRA_INITIAL_TAB, tab);
        startActivity(intent);
    }

    private void refreshTrackerCard() {
        if (selectedPet == null || binding == null) return;
        if (finishedElapsedMs >= 0L && System.currentTimeMillis() < showFinishedUntil) {
            binding.trackerIdleButtons.setVisibility(View.GONE);
            binding.trackerLivePanel.setVisibility(View.VISIBLE);
            binding.trackerStatus.setText(finishedActivityType + " saved");
            binding.trackerElapsed.setText(WalkTrackingStore.formatElapsed(finishedElapsedMs));
            binding.trackerDistance.setVisibility(WalkTrackingStore.supportsLiveDistance(finishedActivityType) ? View.VISIBLE : View.GONE);
            binding.trackerDistance.setText(WalkTrackingStore.formatDistanceKm(finishedDistanceMeters));
            binding.buttonPauseResumeWalk.setVisibility(View.GONE);
            binding.buttonFinishWalk.setVisibility(View.GONE);
            return;
        }

        WalkTrackingStore.State state = WalkTrackingStore.read(requireContext());
        boolean activeForSelectedPet = state.active && state.petId == selectedPet.id;
        if (state.active && state.petId != selectedPet.id) {
            binding.trackerIdleButtons.setVisibility(View.GONE);
            binding.trackerLivePanel.setVisibility(View.VISIBLE);
            binding.trackerStatus.setText("A live activity is already running for another pet.");
            binding.trackerElapsed.setText(WalkTrackingStore.formatElapsed(state.elapsedNow()));
            binding.trackerDistance.setVisibility(View.GONE);
            binding.buttonPauseResumeWalk.setVisibility(View.GONE);
            binding.buttonFinishWalk.setVisibility(View.GONE);
            return;
        }
        if (!activeForSelectedPet) {
            binding.trackerIdleButtons.setVisibility(View.VISIBLE);
            binding.trackerLivePanel.setVisibility(View.GONE);
            return;
        }
        String activityType = WalkTrackingStore.normalizeActivityType(state.activityType);
        binding.trackerIdleButtons.setVisibility(View.GONE);
        binding.trackerLivePanel.setVisibility(View.VISIBLE);
        binding.trackerStatus.setText(state.paused ? activityType + " paused" : "Live " + activityType.toLowerCase(java.util.Locale.ROOT) + " tracking");
        binding.trackerElapsed.setText(WalkTrackingStore.formatElapsed(state.elapsedNow()));
        binding.trackerDistance.setVisibility(WalkTrackingStore.supportsLiveDistance(activityType) ? View.VISIBLE : View.GONE);
        binding.trackerDistance.setText(WalkTrackingStore.formatDistanceKm(state.distanceMeters));
        binding.buttonPauseResumeWalk.setVisibility(View.VISIBLE);
        binding.buttonFinishWalk.setVisibility(View.VISIBLE);
        binding.buttonPauseResumeWalk.setText(state.paused ? "Resume" : "Pause");
    }

    private void startActivityTracking(String activityType) {
        if (selectedPet == null) return;
        pendingStartActivityType = WalkTrackingStore.normalizeActivityType(activityType);
        WalkTrackingStore.State state = WalkTrackingStore.read(requireContext());
        if (state.active && state.petId != selectedPet.id) { toast("Finish the current live activity first."); return; }
        if (hasLocationPermission() || !WalkTrackingStore.supportsLiveDistance(pendingStartActivityType)) startTrackerService();
        else locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    private void onLocationPermissionResult(Map<String, Boolean> result) {
        boolean granted = false;
        for (Boolean value : result.values()) granted |= Boolean.TRUE.equals(value);
        if (granted) startTrackerService();
        else toast("Location permission is needed for live walk tracking.");
    }

    private void startTrackerService() {
        Intent intent = new Intent(requireContext(), WalkTrackingService.class);
        intent.setAction(WalkTrackingService.ACTION_START);
        intent.putExtra(WalkTrackingService.EXTRA_PET_ID, selectedPet.id);
        intent.putExtra(WalkTrackingService.EXTRA_ACTIVITY_TYPE, pendingStartActivityType);
        ContextCompat.startForegroundService(requireContext(), intent);
        refreshTrackerCard();
        toast(pendingStartActivityType + " started");
    }

    private void togglePauseResume() {
        WalkTrackingStore.State state = WalkTrackingStore.read(requireContext());
        if (selectedPet == null || !state.active || state.petId != selectedPet.id) return;
        Intent intent = new Intent(requireContext(), WalkTrackingService.class);
        intent.setAction(state.paused ? WalkTrackingService.ACTION_RESUME : WalkTrackingService.ACTION_PAUSE);
        requireContext().startService(intent);
        refreshTrackerCard();
    }

    private void finishWalkTracking() {
        WalkTrackingStore.State state = WalkTrackingStore.read(requireContext());
        if (selectedPet == null || !state.active || state.petId != selectedPet.id) return;
        finishedElapsedMs = state.elapsedNow();
        finishedDistanceMeters = state.distanceMeters;
        finishedActivityType = WalkTrackingStore.normalizeActivityType(state.activityType);
        showFinishedUntil = System.currentTimeMillis() + 5000L;
        Intent intent = new Intent(requireContext(), WalkTrackingService.class);
        intent.setAction(WalkTrackingService.ACTION_STOP);
        requireContext().startService(intent);
        toast(finishedActivityType + " saved to activity log");
        refreshTrackerCard();
        trackerUiHandler.removeCallbacks(resetAfterFinish);
        trackerUiHandler.postDelayed(resetAfterFinish, 5000L);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private String nullable(String value) { return value == null || value.trim().isEmpty() ? "Unknown breed" : value.trim(); }
    private String safeName(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }
    private void toast(String message) { Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show(); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
