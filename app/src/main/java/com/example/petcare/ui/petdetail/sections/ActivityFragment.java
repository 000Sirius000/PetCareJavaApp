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
import com.example.petcare.ui.common.FilterRange;
import com.example.petcare.ui.forms.ActivitySessionFormActivity;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ActivityFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentActivityOnlySectionBinding binding;
    private PetRepository repository;
    private FilterRange range = FilterRange.WEEK;

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
        reload();
    }

    private void reload() {
        List<ActivitySession> items = repository.getActivitySessions(petId);
        binding.activityChart.setData(items, range);
        binding.activityDistanceChart.setData(items, range);
        binding.sectionSubtitle.setText("Time and distance charts • " + range.name());
        binding.activityTimeStats.setText(activityStats(items, false));
        binding.activityDistanceStats.setText(activityStats(items, true));
        updateFilterButtons();
    }

    private String activityStats(List<ActivitySession> items, boolean distance) {
        long[] bounds = rangeBounds();
        double total = 0d;
        for (ActivitySession item : items) {
            if (item.sessionDateEpochMillis < bounds[0] || item.sessionDateEpochMillis > bounds[1]) continue;
            if (distance) {
                if (item.distance != null && supportsDistance(item.activityType)) total += Math.max(0d, item.distance);
            } else {
                total += Math.max(0, item.durationMinutes);
            }
        }
        int divisor = averageDivisor();
        if (distance) {
            double avg = divisor <= 0 ? 0d : total / divisor;
            return String.format(Locale.getDefault(), "Total %s km · Avg %s/%s", FormatUtils.number(total), FormatUtils.number(avg), avgUnit());
        }
        int minutes = (int) Math.round(total);
        int avg = divisor <= 0 ? 0 : Math.round(minutes / (float) divisor);
        return String.format(Locale.getDefault(), "Total %d min · Avg %d/%s", minutes, avg, avgUnit());
    }

    private boolean supportsDistance(String type) {
        return "walk".equalsIgnoreCase(type) || "run".equalsIgnoreCase(type);
    }

    private int averageDivisor() {
        if (range == FilterRange.YEAR) return 12;
        if (range == FilterRange.MONTH) return Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        return 7;
    }

    private String avgUnit() {
        return range == FilterRange.YEAR ? "mo" : "day";
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
