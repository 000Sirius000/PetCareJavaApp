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
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.WeightEntry;
import com.example.petcare.databinding.FragmentWeightSectionBinding;
import com.example.petcare.ui.common.ChartPeriod;
import com.example.petcare.ui.common.ChartSwipeTouchListener;
import com.example.petcare.ui.common.FilterRange;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.WeightEntryFormActivity;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeightFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentWeightSectionBinding binding;
    private PetRepository repository;
    private SimpleRowAdapter adapter;
    private FilterRange range = FilterRange.MONTH;
    private ChartPeriod period;

    private final ActivityResultLauncher<Intent> formLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reload());

    public static WeightFragment newInstance(long petId) {
        WeightFragment fragment = new WeightFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PET_ID, petId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWeightSectionBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        adapter = new SimpleRowAdapter(new SimpleRowAdapter.RowMapper<WeightEntry>() {
            @Override
            public String title(WeightEntry item) {
                String unit = item.unit == null || item.unit.trim().isEmpty() ? "kg" : item.unit;
                return String.format(Locale.getDefault(), "%.1f %s", item.weightValue, unit);
            }

            @Override
            public String subtitle(WeightEntry item) {
                return FormatUtils.dateTime(item.measuredAt);
            }

            @Override
            public String meta(WeightEntry item) {
                return "";
            }
        });

        adapter.setOnRowClickListener(item -> {
            Intent intent = new Intent(requireContext(), WeightEntryFormActivity.class);
            intent.putExtra(WeightEntryFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(WeightEntryFormActivity.EXTRA_WEIGHT_ID, ((WeightEntry) item).id);
            formLauncher.launch(intent);
        });

        binding.weightRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.weightRecycler.setAdapter(adapter);

        binding.buttonAddWeight.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), WeightEntryFormActivity.class);
            intent.putExtra(WeightEntryFormActivity.EXTRA_PET_ID, petId);
            formLauncher.launch(intent);
        });
        binding.buttonWeek.setOnClickListener(v -> setRange(FilterRange.WEEK));
        binding.buttonMonth.setOnClickListener(v -> setRange(FilterRange.MONTH));
        binding.buttonYear.setOnClickListener(v -> setRange(FilterRange.YEAR));
        binding.buttonPreviousPeriod.setOnClickListener(v -> movePeriod(-1));
        binding.buttonNextPeriod.setOnClickListener(v -> movePeriod(1));
        ChartSwipeTouchListener.attach(binding.weightChart, () -> movePeriod(-1), () -> movePeriod(1));

        reload();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setRange(FilterRange newRange) {
        if (range == newRange) return;
        range = newRange;
        period = null;
        reload();
    }

    private void movePeriod(int amount) {
        if (binding == null || period == null) return;
        if (amount < 0 && !binding.buttonPreviousPeriod.isEnabled()) return;
        if (amount > 0 && !binding.buttonNextPeriod.isEnabled()) return;
        period = period.shift(amount);
        reload();
    }

    private void reload() {
        if (binding == null || repository == null) return;
        List<WeightEntry> entries = repository.getWeightEntries(petId);
        ensurePeriod(entries);
        Pet pet = repository.getPet(petId);
        if (pet != null) {
            for (WeightEntry entry : entries) {
                entry.healthyMin = pet.minHealthyWeight;
                entry.healthyMax = pet.maxHealthyWeight;
            }
        }
        List<WeightEntry> periodEntries = new ArrayList<>();
        for (WeightEntry entry : entries) {
            if (period.contains(entry.measuredAt)) periodEntries.add(entry);
        }
        binding.periodLabel.setText(period.label);
        binding.weightChart.setEntries(periodEntries, period);
        adapter.submitList(entries);
        binding.weightEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        updateFilterButtons();
        updatePeriodNavigation(entries);
    }

    private void ensurePeriod(List<WeightEntry> entries) {
        if (period != null) return;
        long latest = 0L;
        for (WeightEntry entry : entries) latest = Math.max(latest, entry.measuredAt);
        period = ChartPeriod.of(range, latest > 0L ? latest : System.currentTimeMillis());
    }

    private void updatePeriodNavigation(List<WeightEntry> entries) {
        long earliest = Long.MAX_VALUE;
        long latest = 0L;
        for (WeightEntry entry : entries) {
            if (entry.measuredAt <= 0L) continue;
            earliest = Math.min(earliest, entry.measuredAt);
            latest = Math.max(latest, entry.measuredAt);
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
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(20));
        background.setStroke(dp(2), accent);
        background.setColor(active ? accent : Color.TRANSPARENT);
        button.setBackground(background);
        button.setTextColor(active ? ContextCompat.getColor(requireContext(), R.color.black) : accent);
        button.setSelected(active);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
