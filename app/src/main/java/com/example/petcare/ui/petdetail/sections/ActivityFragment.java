package com.example.petcare.ui.petdetail.sections;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.databinding.FragmentActivityOnlySectionBinding;
import com.example.petcare.tracking.WalkTrackingService;
import com.example.petcare.tracking.WalkTrackingStore;
import com.example.petcare.ui.common.FilterRange;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.ActivitySessionFormActivity;
import com.example.petcare.util.FormatUtils;

import java.util.List;
import java.util.Map;

public class ActivityFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentActivityOnlySectionBinding binding;
    private PetRepository repository;
    private SimpleRowAdapter adapter;
    private FilterRange range = FilterRange.WEEK;
    private final Handler trackerUiHandler = new Handler(Looper.getMainLooper());
    private boolean trackerWasActiveForThisPet;

    private final Runnable trackerUiTicker = new Runnable() {
        @Override
        public void run() {
            if (binding == null) {
                return;
            }
            refreshTrackerCard();
            trackerUiHandler.postDelayed(this, 1000L);
        }
    };

    private final ActivityResultLauncher<Intent> formLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reload());

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onLocationPermissionResult);

    public static ActivityFragment newInstance(long petId) {
        ActivityFragment fragment = new ActivityFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PET_ID, petId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentActivityOnlySectionBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        adapter = new SimpleRowAdapter(new SimpleRowAdapter.RowMapper<ActivitySession>() {
            @Override
            public String title(ActivitySession item) {
                String distance = item.distance == null || item.distanceUnit == null
                        ? ""
                        : " • " + FormatUtils.number(item.distance) + " " + item.distanceUnit;
                return item.activityType + " • " + item.durationMinutes + " min" + distance;
            }

            @Override
            public String subtitle(ActivitySession item) {
                return FormatUtils.nullable(item.notes);
            }

            @Override
            public String meta(ActivitySession item) {
                return FormatUtils.date(item.sessionDateEpochMillis);
            }
        });
        adapter.setOnRowClickListener(item -> {
            Intent intent = new Intent(requireContext(), ActivitySessionFormActivity.class);
            intent.putExtra(ActivitySessionFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(ActivitySessionFormActivity.EXTRA_ACTIVITY_ID, ((ActivitySession) item).id);
            formLauncher.launch(intent);
        });

        binding.sectionRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.sectionRecycler.setAdapter(adapter);

        binding.buttonAddActivity.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ActivitySessionFormActivity.class);
            intent.putExtra(ActivitySessionFormActivity.EXTRA_PET_ID, petId);
            formLauncher.launch(intent);
        });
        binding.buttonWeek.setOnClickListener(v -> setRange(FilterRange.WEEK));
        binding.buttonMonth.setOnClickListener(v -> setRange(FilterRange.MONTH));
        binding.buttonYear.setOnClickListener(v -> setRange(FilterRange.YEAR));

        binding.buttonStartWalk.setOnClickListener(v -> startWalkTracking());
        binding.buttonPauseResumeWalk.setOnClickListener(v -> togglePauseResume());
        binding.buttonFinishWalk.setOnClickListener(v -> finishWalkTracking());

        reload();
        refreshTrackerCard();
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
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        trackerUiHandler.removeCallbacks(trackerUiTicker);
        binding = null;
        super.onDestroyView();
    }

    private void setRange(FilterRange newRange) {
        range = newRange;
        reload();
    }

    private void reload() {
        List<ActivitySession> items = repository.getActivitySessions(petId);
        adapter.submitList(items);
        binding.sectionEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.activityChart.setData(items, range);
        binding.sectionSubtitle.setText("Daily walking time in hours • " + range.name());
    }

    private void refreshTrackerCard() {
        WalkTrackingStore.State state = WalkTrackingStore.read(requireContext());
        boolean activeForThisPet = state.active && state.petId == petId;

        if (trackerWasActiveForThisPet && !activeForThisPet) {
            reload();
        }
        trackerWasActiveForThisPet = activeForThisPet;

        if (state.active && state.petId != petId) {
            binding.trackerStatus.setText("A live walk is already running for another pet.");
            binding.trackerElapsed.setText(WalkTrackingStore.formatElapsed(state.elapsedNow()));
            binding.trackerDistance.setText(WalkTrackingStore.formatDistanceKm(state.distanceMeters));
            binding.buttonStartWalk.setEnabled(false);
            binding.buttonPauseResumeWalk.setVisibility(View.GONE);
            binding.buttonFinishWalk.setVisibility(View.GONE);
            return;
        }

        binding.buttonStartWalk.setEnabled(true);
        if (!activeForThisPet) {
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
        WalkTrackingStore.State state = WalkTrackingStore.read(requireContext());
        if (state.active && state.petId != petId) {
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
        if (granted) {
            startWalkService();
        } else {
            toast("Location permission is needed for live walk tracking.");
        }
    }

    private void startWalkService() {
        Intent intent = new Intent(requireContext(), WalkTrackingService.class);
        intent.setAction(WalkTrackingService.ACTION_START);
        intent.putExtra(WalkTrackingService.EXTRA_PET_ID, petId);
        ContextCompat.startForegroundService(requireContext(), intent);
        refreshTrackerCard();
        toast("Live walk started");
    }

    private void togglePauseResume() {
        WalkTrackingStore.State state = WalkTrackingStore.read(requireContext());
        if (!state.active || state.petId != petId) {
            return;
        }
        Intent intent = new Intent(requireContext(), WalkTrackingService.class);
        intent.setAction(state.paused ? WalkTrackingService.ACTION_RESUME : WalkTrackingService.ACTION_PAUSE);
        requireContext().startService(intent);
        refreshTrackerCard();
    }

    private void finishWalkTracking() {
        WalkTrackingStore.State state = WalkTrackingStore.read(requireContext());
        if (!state.active || state.petId != petId) {
            return;
        }
        Intent intent = new Intent(requireContext(), WalkTrackingService.class);
        intent.setAction(WalkTrackingService.ACTION_STOP);
        requireContext().startService(intent);
        toast("Walk saved to activity log");
        refreshTrackerCard();
        reload();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
