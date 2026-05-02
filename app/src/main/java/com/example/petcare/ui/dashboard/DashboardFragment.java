package com.example.petcare.ui.dashboard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.databinding.FragmentDashboardBinding;
import com.example.petcare.tracking.WalkTrackingService;
import com.example.petcare.tracking.WalkTrackingStore;
import com.example.petcare.ui.petdetail.PetDetailActivity;
import com.example.petcare.util.FormatUtils;

import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private PetRepository repository;
    private Pet selectedPet;

    private final Handler trackerUiHandler = new Handler(Looper.getMainLooper());
    private WalkTrackingStore.State justFinishedState;
    private long showFinishedUntil = 0L;

    private final Runnable trackerUiTicker = new Runnable() {
        @Override
        public void run() {
            if (binding == null) return;
            refreshTrackerCard();
            trackerUiHandler.postDelayed(this, 1000L);
        }
    };

    private final Runnable resetAfterFinish = new Runnable() {
        @Override
        public void run() {
            justFinishedState = null;
            showFinishedUntil = 0L;
            refreshTrackerCard();
            reload();
        }
    };

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onLocationPermissionResult);

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());

        binding.dashboardSwipe.setOnRefreshListener(this::reload);
        binding.activitySummaryCard.setOnClickListener(v -> openPetDetail(PetDetailActivity.TAB_ACTIVITY));
        binding.remindersCard.setOnClickListener(v -> openPetDetail(PetDetailActivity.TAB_HEALTH));

        binding.buttonStartWalk.setOnClickListener(v -> startWalkTracking());
        binding.buttonPauseResumeWalk.setOnClickListener(v -> togglePauseResume());
        binding.buttonFinishWalk.setOnClickListener(v -> finishWalkTracking());

        reload();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        trackerUiHandler.removeCallbacks(trackerUiTicker);
        trackerUiHandler.post(trackerUiTicker);
        reload();
    }

    @Override
    public void onPause() {
        trackerUiHandler.removeCallbacks(trackerUiTicker);
        trackerUiHandler.removeCallbacks(resetAfterFinish);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
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

        if (!TextUtils.isEmpty(selectedPet.photoUri)) {
            binding.petPhoto.setImageURI(Uri.parse(selectedPet.photoUri));
        } else {
            binding.petPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
        }

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
        refreshTrackerCard();
        binding.dashboardSwipe.setRefreshing(false);
    }

    private void renderUpcomingReminders(long petId) {
        List<Object> reminders = repository.getUpcomingReminderPreview(petId);
        if (reminders.isEmpty()) {
            binding.reminderRows.setText("No upcoming reminders.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (Object item : reminders) {
            if (item instanceof Medication) {
                Medication med = (Medication) item;
                builder.append("☐ ")
                        .append(med.medicationName)
                        .append(" · ")
                        .append(FormatUtils.dateTime(med.nextReminderAt))
                        .append('\n');
            } else if (item instanceof Vaccination) {
                Vaccination vaccination = (Vaccination) item;
                long time = vaccination.nextDueAt == null ? 0L : vaccination.nextDueAt;
                builder.append("☐ Vaccination: ")
                        .append(vaccination.vaccineName)
                        .append(" · ")
                        .append(FormatUtils.date(time))
                        .append('\n');
            }
        }
        binding.reminderRows.setText(builder.toString().trim());
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

        if (justFinishedState != null && System.currentTimeMillis() < showFinishedUntil) {
            binding.trackerStatus.setText("Walk saved");
            binding.trackerElapsed.setText(WalkTrackingStore.formatElapsed(justFinishedState.elapsedNow()));
            binding.trackerDistance.setText(WalkTrackingStore.formatDistanceKm(justFinishedState.distanceMeters));
            binding.buttonStartWalk.setVisibility(View.GONE);
            binding.buttonPauseResumeWalk.setVisibility(View.GONE);
            binding.buttonFinishWalk.setVisibility(View.GONE);
            return;
        }

        WalkTrackingStore.State state = WalkTrackingStore.read(requireContext());
        boolean activeForSelectedPet = state.active && state.petId == selectedPet.id;

        if (state.active && state.petId != selectedPet.id) {
            binding.trackerStatus.setText("A live walk is already running for another pet.");
            binding.trackerElapsed.setText(WalkTrackingStore.formatElapsed(state.elapsedNow()));
            binding.trackerDistance.setText(WalkTrackingStore.formatDistanceKm(state.distanceMeters));
            binding.buttonStartWalk.setEnabled(false);
            binding.buttonStartWalk.setVisibility(View.VISIBLE);
            binding.buttonPauseResumeWalk.setVisibility(View.GONE);
            binding.buttonFinishWalk.setVisibility(View.GONE);
            return;
        }

        binding.buttonStartWalk.setEnabled(true);

        if (!activeForSelectedPet) {
            binding.trackerStatus.setText("No live walk in progress");
            binding.trackerElapsed.setText("00:00:00");
            binding.trackerDistance.setText("0.00 km");
            binding.buttonStartWalk.setVisibility(View.VISIBLE);
            binding.buttonPauseResumeWalk.setVisibility(View.GONE);
            binding.buttonFinishWalk.setVisibility(View.GONE);
            return;
        }

        binding.trackerStatus.setText(state.paused ? "Live walk paused" : "Live walk tracking");
        binding.trackerElapsed.setText(WalkTrackingStore.formatElapsed(state.elapsedNow()));
        binding.trackerDistance.setText(WalkTrackingStore.formatDistanceKm(state.distanceMeters));
        binding.buttonStartWalk.setVisibility(View.GONE);
        binding.buttonPauseResumeWalk.setVisibility(View.VISIBLE);
        binding.buttonFinishWalk.setVisibility(View.VISIBLE);
        binding.buttonPauseResumeWalk.setText(state.paused ? "Resume" : "Pause");
    }

    private void startWalkTracking() {
        if (selectedPet == null) return;

        WalkTrackingStore.State state = WalkTrackingStore.read(requireContext());
        if (state.active && state.petId != selectedPet.id) {
            toast("Finish the current live walk first.");
            return;
        }

        if (hasLocationPermission()) {
            startWalkService();
            return;
        }

        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void onLocationPermissionResult(Map<String, Boolean> result) {
        boolean granted = false;
        for (Boolean value : result.values()) {
            granted |= Boolean.TRUE.equals(value);
        }
        if (granted) startWalkService();
        else toast("Location permission is needed for live walk tracking.");
    }

    private void startWalkService() {
        Intent intent = new Intent(requireContext(), WalkTrackingService.class);
        intent.setAction(WalkTrackingService.ACTION_START);
        intent.putExtra(WalkTrackingService.EXTRA_PET_ID, selectedPet.id);
        ContextCompat.startForegroundService(requireContext(), intent);
        refreshTrackerCard();
        toast("Live walk started");
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

        justFinishedState = state;
        showFinishedUntil = System.currentTimeMillis() + 5000L;

        Intent intent = new Intent(requireContext(), WalkTrackingService.class);
        intent.setAction(WalkTrackingService.ACTION_STOP);
        requireContext().startService(intent);

        toast("Walk saved to activity log");
        refreshTrackerCard();
        trackerUiHandler.removeCallbacks(resetAfterFinish);
        trackerUiHandler.postDelayed(resetAfterFinish, 5000L);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private String nullable(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown breed" : value.trim();
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
