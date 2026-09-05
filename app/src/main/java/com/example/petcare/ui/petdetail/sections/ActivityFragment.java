package com.example.petcare.ui.petdetail.sections;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.databinding.FragmentActivityOnlySectionBinding;
import com.example.petcare.ui.common.ChartPeriod;
import com.example.petcare.ui.common.ChartStats;
import com.example.petcare.ui.common.ChartSwipeTouchListener;
import com.example.petcare.ui.common.FilterRange;
import com.example.petcare.ui.forms.ActivitySessionFormActivity;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;

import java.util.List;
import java.util.Locale;

public class ActivityFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentActivityOnlySectionBinding binding;
    private PetRepository repository;
    private FilterRange range = FilterRange.WEEK;
    private ChartPeriod period;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentActivityOnlySectionBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        binding.buttonAddActivity.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ActivitySessionFormActivity.class);
            intent.putExtra(ActivitySessionFormActivity.EXTRA_PET_ID, petId);
            formLauncher.launch(intent);
        });
        binding.buttonOpenActivityLog.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ActivityLogActivity.class);
            intent.putExtra(ActivityLogActivity.EXTRA_PET_ID, petId);
            startActivity(intent);
        });

        binding.buttonWeek.setOnClickListener(v -> setRange(FilterRange.WEEK));
        binding.buttonMonth.setOnClickListener(v -> setRange(FilterRange.MONTH));
        binding.buttonYear.setOnClickListener(v -> setRange(FilterRange.YEAR));
        binding.buttonPreviousPeriod.setOnClickListener(v -> movePeriod(-1));
        binding.buttonNextPeriod.setOnClickListener(v -> movePeriod(1));
        ChartSwipeTouchListener.attach(binding.activityChart, () -> movePeriod(-1), () -> movePeriod(1));
        ChartSwipeTouchListener.attach(binding.activityDistanceChart, () -> movePeriod(-1), () -> movePeriod(1));

        reload();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void setRange(FilterRange newRange) {
        range = newRange;
        period = null;
        reload();
    }

    private void movePeriod(int amount) {
        if (period == null || binding == null) return;
        if (amount < 0 && !binding.buttonPreviousPeriod.isEnabled()) return;
        if (amount > 0 && !binding.buttonNextPeriod.isEnabled()) return;
        period = period.shift(amount);
        reload();
    }

    private void reload() {
        List<ActivitySession> items = repository.getActivitySessions(petId);
        ensurePeriod(items);
        binding.activityChart.setData(items, period);
        binding.activityDistanceChart.setData(items, period);
        binding.sectionSubtitle.setText("Time and distance charts");
        binding.periodLabel.setText(period.label);
        binding.activityTimeStats.setText(activityStats(items, false));
        binding.activityDistanceStats.setText(activityStats(items, true));
        updateFilterButtons();
        updatePeriodNavigation(items);
    }

    private String activityStats(List<ActivitySession> items, boolean distance) {
        double[] bucketTotals = new double[period.buckets.size()];
        boolean[] filled = new boolean[period.buckets.size()];
        double total = 0d;
        for (ActivitySession item : items) {
            int bucket = period.bucketIndex(item.sessionDateEpochMillis);
            if (bucket < 0) continue;
            if (distance) {
                if (item.distance != null && supportsDistance(item.activityType)) {
                    double value = Math.max(0d, item.distance);
                    total += value;
                    bucketTotals[bucket] += value;
                    filled[bucket] = true;
                }
            } else {
                double value = Math.max(0, item.durationMinutes);
                total += value;
                bucketTotals[bucket] += value;
            }
        }
        if (distance) {
            double avg = ChartStats.averageFilled(bucketTotals, filled);
            return String.format(Locale.getDefault(), "Total %s km · Avg %s/%s", FormatUtils.number(total), FormatUtils.number(avg), avgUnit());
        }
        double hours = total / 60d;
        double avg = ChartStats.averagePositive(bucketTotals) / 60d;
        return String.format(Locale.getDefault(), "Total %s h · Avg %s/%s", FormatUtils.number(hours), FormatUtils.number(avg), avgUnit());
    }

    private boolean supportsDistance(String type) {
        return "walk".equalsIgnoreCase(type) || "run".equalsIgnoreCase(type);
    }

    private String avgUnit() {
        return range == FilterRange.YEAR ? "mo" : "day";
    }

    private void ensurePeriod(List<ActivitySession> items) {
        if (period != null) return;
        long latest = 0L;
        for (ActivitySession item : items) {
            latest = Math.max(latest, item.sessionDateEpochMillis);
        }
        period = ChartPeriod.of(range, latest > 0L ? latest : System.currentTimeMillis());
    }

    private void updatePeriodNavigation(List<ActivitySession> items) {
        long earliest = Long.MAX_VALUE;
        long latest = 0L;
        for (ActivitySession item : items) {
            if (item.sessionDateEpochMillis <= 0L) continue;
            earliest = Math.min(earliest, item.sessionDateEpochMillis);
            latest = Math.max(latest, item.sessionDateEpochMillis);
        }
        if (latest <= 0L) {
            binding.buttonPreviousPeriod.setEnabled(false);
            binding.buttonNextPeriod.setEnabled(false);
            return;
        }
        long earliestStart = ChartPeriod.periodStart(range, earliest);
        long latestStart = ChartPeriod.periodStart(range, latest);
        binding.buttonPreviousPeriod.setEnabled(period.startMillis > earliestStart);
        binding.buttonNextPeriod.setEnabled(period.startMillis < latestStart);
    }

    private void updateFilterButtons() {
        styleChip(binding.buttonWeek, range == FilterRange.WEEK);
        styleChip(binding.buttonMonth, range == FilterRange.MONTH);
        styleChip(binding.buttonYear, range == FilterRange.YEAR);
    }

    private void styleChip(Button button, boolean active) {
        int accent = ThemeUtils.getAccentColor(requireContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(2), accent);
        bg.setColor(active ? accent : Color.TRANSPARENT);
        button.setBackground(bg);
        button.setTextColor(active ? ContextCompat.getColor(requireContext(), R.color.black) : accent);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
