package com.example.petcare.ui.petdetail.sections;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.WeightEntry;
import com.example.petcare.databinding.FragmentActivitySectionBinding;
import com.example.petcare.ui.common.ActivityBarChartView;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.ActivitySessionFormActivity;
import com.example.petcare.ui.forms.WeightEntryFormActivity;
import com.example.petcare.util.FormatUtils;
import com.google.android.material.tabs.TabLayout;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";
    private static final int TAB_ACTIVITY = 0;
    private static final int TAB_WEIGHT = 1;
    private static final int RANGE_WEEK = 0;
    private static final int RANGE_MONTH = 1;
    private static final int RANGE_YEAR = 2;

    private long petId;
    private int selectedTab = TAB_ACTIVITY;
    private int selectedRange = RANGE_WEEK;

    private FragmentActivitySectionBinding binding;
    private PetRepository repository;
    private SimpleRowAdapter adapter;

    private final ActivityResultLauncher<Intent> formLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reload());

    public static ActivityFragment newInstance(long petId) {
        ActivityFragment fragment = new ActivityFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PET_ID, petId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentActivitySectionBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        adapter = new SimpleRowAdapter(new SimpleRowAdapter.RowMapper<Object>() {
            @Override
            public String title(Object item) {
                if (item instanceof ActivitySession) {
                    ActivitySession session = (ActivitySession) item;
                    return session.activityType + " • " + session.durationMinutes + " min";
                }
                if (item instanceof WeightEntry) {
                    WeightEntry entry = (WeightEntry) item;
                    return "Weight • " + entry.weightValue + " " + entry.unit;
                }
                return "";
            }

            @Override
            public String subtitle(Object item) {
                if (item instanceof ActivitySession) {
                    ActivitySession session = (ActivitySession) item;
                    return "Distance: " + (session.distance == null ? "—" : session.distance + " " + session.distanceUnit);
                }
                if (item instanceof WeightEntry) {
                    WeightEntry entry = (WeightEntry) item;
                    String marker = "";
                    if (entry.healthyMin != null && entry.healthyMax != null
                            && (entry.weightValue < entry.healthyMin || entry.weightValue > entry.healthyMax)) {
                        marker = " • outside range";
                    }
                    return "Measured on " + FormatUtils.date(entry.measuredAt) + marker;
                }
                return "";
            }

            @Override
            public String meta(Object item) {
                if (item instanceof ActivitySession) {
                    ActivitySession session = (ActivitySession) item;
                    return FormatUtils.date(session.sessionDateEpochMillis) + " • " + FormatUtils.nullable(session.notes);
                }
                if (item instanceof WeightEntry) {
                    WeightEntry entry = (WeightEntry) item;
                    if (entry.healthyMin == null || entry.healthyMax == null) {
                        return "Healthy range: not set";
                    }
                    return "Healthy range: " + entry.healthyMin + " - " + entry.healthyMax + " " + entry.unit;
                }
                return "";
            }
        });

        adapter.setOnRowClickListener(item -> {
            if (item instanceof ActivitySession) {
                Intent intent = new Intent(requireContext(), ActivitySessionFormActivity.class);
                intent.putExtra(ActivitySessionFormActivity.EXTRA_PET_ID, petId);
                intent.putExtra(ActivitySessionFormActivity.EXTRA_ACTIVITY_ID, ((ActivitySession) item).id);
                formLauncher.launch(intent);
            } else if (item instanceof WeightEntry) {
                Intent intent = new Intent(requireContext(), WeightEntryFormActivity.class);
                intent.putExtra(WeightEntryFormActivity.EXTRA_PET_ID, petId);
                intent.putExtra(WeightEntryFormActivity.EXTRA_WEIGHT_ID, ((WeightEntry) item).id);
                formLauncher.launch(intent);
            }
        });

        binding.sectionRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.sectionRecycler.setAdapter(adapter);

        binding.sectionTabs.addTab(binding.sectionTabs.newTab().setText("Activity"));
        binding.sectionTabs.addTab(binding.sectionTabs.newTab().setText("Weight"));
        binding.sectionTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                reload();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                reload();
            }
        });

        binding.rangeTabs.addTab(binding.rangeTabs.newTab().setText("Week"));
        binding.rangeTabs.addTab(binding.rangeTabs.newTab().setText("Month"));
        binding.rangeTabs.addTab(binding.rangeTabs.newTab().setText("Year"));
        binding.rangeTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedRange = tab.getPosition();
                if (selectedTab == TAB_ACTIVITY) {
                    reload();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                selectedRange = tab.getPosition();
                if (selectedTab == TAB_ACTIVITY) {
                    reload();
                }
            }
        });

        binding.buttonAddPrimary.setOnClickListener(v -> {
            if (selectedTab == TAB_ACTIVITY) {
                Intent intent = new Intent(requireContext(), ActivitySessionFormActivity.class);
                intent.putExtra(ActivitySessionFormActivity.EXTRA_PET_ID, petId);
                formLauncher.launch(intent);
            } else {
                Intent intent = new Intent(requireContext(), WeightEntryFormActivity.class);
                intent.putExtra(WeightEntryFormActivity.EXTRA_PET_ID, petId);
                formLauncher.launch(intent);
            }
        });

        reload();
        return binding.getRoot();
    }

    private void reload() {
        if (selectedTab == TAB_ACTIVITY) {
            showActivitySection();
        } else {
            showWeightSection();
        }
    }

    private void showActivitySection() {
        binding.sectionTitle.setText("Activity");
        binding.buttonAddPrimary.setText("Add activity");
        binding.rangeTabs.setVisibility(View.VISIBLE);
        binding.activityChart.setVisibility(View.VISIBLE);
        binding.weightChart.setVisibility(View.GONE);

        List<ActivitySession> sessions = repository.getActivitySessions(petId);
        adapter.submitList(new ArrayList<>(sessions));
        binding.sectionEmpty.setText("No activity entries yet");
        binding.sectionEmpty.setVisibility(sessions.isEmpty() ? View.VISIBLE : View.GONE);

        List<ActivityBarChartView.BarPoint> points = buildChartPoints(sessions);
        binding.activityChart.setPoints(points);

        int totalMinutes = 0;
        int sessionCount = 0;
        for (ActivityBarChartView.BarPoint point : points) {
            totalMinutes += point.totalMinutes;
            sessionCount += point.sessionCount;
        }

        Pet pet = repository.getPet(petId);
        int progress = pet == null ? 0 : repository.getWeeklyActivityProgressPercent(petId, pet.weeklyActivityGoalMinutes);
        String rangeLabel = selectedRange == RANGE_WEEK ? "week" : selectedRange == RANGE_MONTH ? "month" : "year";
        binding.sectionSubtitle.setText(
                "Walk summary for " + rangeLabel + ": " + totalMinutes + " min • " + sessionCount + " sessions • Goal progress " + progress + "%"
        );
    }

    private void showWeightSection() {
        binding.sectionTitle.setText("Weight");
        binding.sectionSubtitle.setText("Weight history and healthy-range markers");
        binding.buttonAddPrimary.setText("Add weight");
        binding.rangeTabs.setVisibility(View.GONE);
        binding.activityChart.setVisibility(View.GONE);
        binding.weightChart.setVisibility(View.VISIBLE);

        List<WeightEntry> weights = repository.getWeightEntries(petId);
        adapter.submitList(new ArrayList<>(weights));
        binding.sectionEmpty.setText("No weight entries yet");
        binding.sectionEmpty.setVisibility(weights.isEmpty() ? View.VISIBLE : View.GONE);
        binding.weightChart.setEntries(weights);
    }

    private List<ActivityBarChartView.BarPoint> buildChartPoints(List<ActivitySession> sessions) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);
        List<ActivityBarChartView.BarPoint> result = new ArrayList<>();

        if (selectedRange == RANGE_WEEK) {
            for (int offset = 6; offset >= 0; offset--) {
                LocalDate date = today.minusDays(offset);
                int totalMinutes = 0;
                int sessionCount = 0;
                for (ActivitySession session : sessions) {
                    if (!"Walk".equalsIgnoreCase(session.activityType)) continue;
                    LocalDate sessionDate = Instant.ofEpochMilli(session.sessionDateEpochMillis).atZone(zoneId).toLocalDate();
                    if (sessionDate.equals(date)) {
                        totalMinutes += session.durationMinutes;
                        sessionCount++;
                    }
                }
                result.add(new ActivityBarChartView.BarPoint(
                        date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + date.getDayOfMonth(),
                        totalMinutes,
                        sessionCount
                ));
            }
            return result;
        }

        if (selectedRange == RANGE_MONTH) {
            LocalDate firstDay = today.withDayOfMonth(1);
            int length = today.lengthOfMonth();
            for (int day = 1; day <= length; day++) {
                LocalDate date = firstDay.withDayOfMonth(day);
                int totalMinutes = 0;
                int sessionCount = 0;
                for (ActivitySession session : sessions) {
                    if (!"Walk".equalsIgnoreCase(session.activityType)) continue;
                    LocalDate sessionDate = Instant.ofEpochMilli(session.sessionDateEpochMillis).atZone(zoneId).toLocalDate();
                    if (sessionDate.equals(date)) {
                        totalMinutes += session.durationMinutes;
                        sessionCount++;
                    }
                }
                result.add(new ActivityBarChartView.BarPoint(String.valueOf(day), totalMinutes, sessionCount));
            }
            return result;
        }

        int currentYear = today.getYear();
        for (int monthValue = 1; monthValue <= 12; monthValue++) {
            int totalMinutes = 0;
            int sessionCount = 0;
            for (ActivitySession session : sessions) {
                if (!"Walk".equalsIgnoreCase(session.activityType)) continue;
                LocalDate sessionDate = Instant.ofEpochMilli(session.sessionDateEpochMillis).atZone(zoneId).toLocalDate();
                if (sessionDate.getYear() == currentYear && sessionDate.getMonthValue() == monthValue) {
                    totalMinutes += session.durationMinutes;
                    sessionCount++;
                }
            }
            String label = Month.of(monthValue).getDisplayName(TextStyle.SHORT, Locale.getDefault());
            result.add(new ActivityBarChartView.BarPoint(label, totalMinutes, sessionCount));
        }
        return result;
    }
}
