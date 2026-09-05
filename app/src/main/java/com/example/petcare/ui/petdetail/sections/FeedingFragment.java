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
import com.example.petcare.data.entities.FeedingLog;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.databinding.FragmentFeedingSectionBinding;
import com.example.petcare.ui.common.ChartPeriod;
import com.example.petcare.ui.common.ChartStats;
import com.example.petcare.ui.common.ChartSwipeTouchListener;
import com.example.petcare.ui.common.FilterRange;
import com.example.petcare.ui.forms.FeedingScheduleFormActivity;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;

import java.util.List;
import java.util.Locale;

public class FeedingFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentFeedingSectionBinding binding;
    private PetRepository repository;
    private FilterRange range = FilterRange.WEEK;
    private ChartPeriod period;

    private final ActivityResultLauncher<Intent> formLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reload());

    public static FeedingFragment newInstance(long petId) {
        FeedingFragment fragment = new FeedingFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PET_ID, petId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFeedingSectionBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        binding.buttonAddFeeding.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FeedingScheduleFormActivity.class);
            intent.putExtra(FeedingScheduleFormActivity.EXTRA_PET_ID, petId);
            formLauncher.launch(intent);
        });
        binding.buttonOpenFeedingDetails.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FeedingDetailsActivity.class);
            intent.putExtra(FeedingDetailsActivity.EXTRA_PET_ID, petId);
            startActivity(intent);
        });
        binding.buttonWeek.setOnClickListener(v -> setRange(FilterRange.WEEK));
        binding.buttonMonth.setOnClickListener(v -> setRange(FilterRange.MONTH));
        binding.buttonYear.setOnClickListener(v -> setRange(FilterRange.YEAR));
        binding.buttonPreviousPeriod.setOnClickListener(v -> movePeriod(-1));
        binding.buttonNextPeriod.setOnClickListener(v -> movePeriod(1));
        ChartSwipeTouchListener.attach(binding.feedingChart, () -> movePeriod(-1), () -> movePeriod(1));

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
        List<FeedingSchedule> schedules = repository.getFeedingSchedules(petId);
        List<FeedingLog> logs = repository.getFeedingLogs(petId);
        ensurePeriod(logs, schedules);
        binding.feedingChart.setData(logs, schedules, period);
        binding.feedingStats.setText(feedingStats(logs, schedules));
        binding.sectionSubtitle.setText("Kilograms consumed by food type");
        binding.periodLabel.setText(period.label);
        updateFilterButtons();
        updatePeriodNavigation(logs, schedules);
    }

    private String feedingStats(List<FeedingLog> logs, List<FeedingSchedule> schedules) {
        double[] bucketTotals = new double[period.buckets.size()];
        double total = 0d;
        for (FeedingLog log : logs) {
            int bucket = period.bucketIndex(log.completedAt);
            if (bucket < 0) continue;
            double value = FormatUtils.parseLeadingNumber(log.portion);
            if (value <= 0d) continue;
            total += value;
            bucketTotals[bucket] += value;
        }
        for (FeedingSchedule schedule : schedules) {
            long timestamp = schedule.createdAtEpochMillis > 0L
                    ? schedule.createdAtEpochMillis
                    : period.endMillis;
            int bucket = period.bucketIndex(timestamp);
            if (bucket < 0) continue;
            double value = FormatUtils.parseLeadingNumber(schedule.portion);
            if (value <= 0d) continue;
            total += value;
            bucketTotals[bucket] += value;
        }
        double avg = ChartStats.averagePositive(bucketTotals);
        return String.format(Locale.getDefault(), "Total %s kg - Avg %s/%s", FormatUtils.kilogramsFromGrams(total), FormatUtils.kilogramsFromGrams(avg), avgUnit());
    }

    private String avgUnit() {
        return range == FilterRange.YEAR ? "mo" : "day";
    }

    private void ensurePeriod(List<FeedingLog> logs, List<FeedingSchedule> schedules) {
        if (period != null) return;
        long latest = 0L;
        boolean hasUndatedSchedule = false;
        for (FeedingLog log : logs) latest = Math.max(latest, log.completedAt);
        for (FeedingSchedule schedule : schedules) {
            if (schedule.createdAtEpochMillis > 0L) latest = Math.max(latest, schedule.createdAtEpochMillis);
            else hasUndatedSchedule = true;
        }
        if (hasUndatedSchedule) latest = Math.max(latest, System.currentTimeMillis());
        period = ChartPeriod.of(range, latest > 0L ? latest : System.currentTimeMillis());
    }

    private void updatePeriodNavigation(List<FeedingLog> logs, List<FeedingSchedule> schedules) {
        long earliest = Long.MAX_VALUE;
        long latest = 0L;
        boolean hasUndatedSchedule = false;
        for (FeedingLog log : logs) {
            if (log.completedAt <= 0L) continue;
            earliest = Math.min(earliest, log.completedAt);
            latest = Math.max(latest, log.completedAt);
        }
        for (FeedingSchedule schedule : schedules) {
            if (schedule.createdAtEpochMillis > 0L) {
                earliest = Math.min(earliest, schedule.createdAtEpochMillis);
                latest = Math.max(latest, schedule.createdAtEpochMillis);
            } else {
                hasUndatedSchedule = true;
            }
        }
        if (hasUndatedSchedule) {
            long now = System.currentTimeMillis();
            earliest = Math.min(earliest, now);
            latest = Math.max(latest, now);
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
