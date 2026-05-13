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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.FeedingLog;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.databinding.FragmentFeedingSectionBinding;
import com.example.petcare.ui.common.FilterRange;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.FeedingScheduleFormActivity;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FeedingFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";
    private long petId;
    private FragmentFeedingSectionBinding binding;
    private PetRepository repository;
    private SimpleRowAdapter adapter;
    private FilterRange range = FilterRange.WEEK;

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

        adapter = new SimpleRowAdapter(new SimpleRowAdapter.RowMapper<FeedingSchedule>() {
            @Override public String title(FeedingSchedule item) { return dotFor(item.foodType) + " " + item.mealName; }
            @Override public String subtitle(FeedingSchedule item) {
                String date = item.createdAtEpochMillis > 0L ? FormatUtils.humanDate(item.createdAtEpochMillis) : "No date";
                return String.format(Locale.getDefault(), "%s · %02d:%02d · %s", date, item.hourOfDay, item.minute, normalizeFoodType(item.foodType));
            }
            @Override public String meta(FeedingSchedule item) { return "Portion: " + FormatUtils.number(FormatUtils.parseLeadingNumber(item.portion)) + " g"; }
        });
        adapter.setOnRowClickListener(item -> {
            Intent intent = new Intent(requireContext(), FeedingScheduleFormActivity.class);
            intent.putExtra(FeedingScheduleFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(FeedingScheduleFormActivity.EXTRA_SCHEDULE_ID, ((FeedingSchedule) item).id);
            formLauncher.launch(intent);
        });

        binding.sectionRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.sectionRecycler.setAdapter(adapter);
        binding.buttonAddFeeding.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FeedingScheduleFormActivity.class);
            intent.putExtra(FeedingScheduleFormActivity.EXTRA_PET_ID, petId);
            formLauncher.launch(intent);
        });
        binding.buttonWeek.setOnClickListener(v -> setRange(FilterRange.WEEK));
        binding.buttonMonth.setOnClickListener(v -> setRange(FilterRange.MONTH));
        binding.buttonYear.setOnClickListener(v -> setRange(FilterRange.YEAR));
        reload();
        return binding.getRoot();
    }

    private void setRange(FilterRange newRange) { range = newRange; reload(); }

    private void reload() {
        List<FeedingSchedule> schedules = repository.getFeedingSchedules(petId);
        List<FeedingLog> logs = repository.getFeedingLogs(petId);
        adapter.submitList(schedules);
        binding.sectionEmpty.setVisibility(schedules.isEmpty() && logs.isEmpty() ? View.VISIBLE : View.GONE);
        binding.feedingChart.setData(logs, schedules, range);
        binding.feedingStats.setText(feedingStats(logs, schedules));
        binding.sectionSubtitle.setText("Grams consumed by food type • " + range.name());
        updateFilterButtons();
    }

    private String feedingStats(List<FeedingLog> logs, List<FeedingSchedule> schedules) {
        long[] bounds = rangeBounds();
        double total = 0d;
        for (FeedingLog log : logs) {
            if (log.completedAt >= bounds[0] && log.completedAt <= bounds[1]) total += FormatUtils.parseLeadingNumber(log.portion);
        }
        for (FeedingSchedule schedule : schedules) {
            long t = schedule.createdAtEpochMillis > 0L ? schedule.createdAtEpochMillis : System.currentTimeMillis();
            if (t >= bounds[0] && t <= bounds[1]) total += FormatUtils.parseLeadingNumber(schedule.portion);
        }
        int divisor = averageDivisor();
        double avg = divisor <= 0 ? 0d : total / divisor;
        return String.format(Locale.getDefault(), "Total %s g · Avg %s/%s", FormatUtils.number(total), FormatUtils.number(avg), avgUnit());
    }

    private long[] rangeBounds() {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        if (range == FilterRange.YEAR) {
            start.clear(); start.set(now.get(Calendar.YEAR), Calendar.JANUARY, 1, 0, 0, 0);
            end.clear(); end.set(now.get(Calendar.YEAR), Calendar.DECEMBER, 31, 23, 59, 59);
        } else if (range == FilterRange.MONTH) {
            start.clear(); start.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1, 0, 0, 0);
            end.clear(); end.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        } else {
            start.add(Calendar.DAY_OF_YEAR, -6);
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59); end.set(Calendar.MILLISECOND, 999);
        }
        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }

    private int averageDivisor() {
        if (range == FilterRange.YEAR) return 12;
        if (range == FilterRange.MONTH) return Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        return 7;
    }

    private String avgUnit() { return range == FilterRange.YEAR ? "mo" : "day"; }

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

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private String normalizeFoodType(String type) {
        if (type == null || type.trim().isEmpty()) return "Dry food";
        return type.trim();
    }

    private String dotFor(String type) {
        String normalized = normalizeFoodType(type);
        if ("Natural".equalsIgnoreCase(normalized)) return "🟩";
        if ("Wet food (canned)".equalsIgnoreCase(normalized) || "Wet food".equalsIgnoreCase(normalized)) return "🟦";
        return "🟨";
    }
}
