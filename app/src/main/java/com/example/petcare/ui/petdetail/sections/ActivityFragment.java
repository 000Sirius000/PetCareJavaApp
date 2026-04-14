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
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.ActivitySessionFormActivity;
import com.example.petcare.ui.forms.WeightEntryFormActivity;
import com.example.petcare.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class ActivityFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
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
                    return ((ActivitySession) item).activityType + " • " + ((ActivitySession) item).durationMinutes + " min";
                }
                if (item instanceof WeightEntry) {
                    return "Weight • " + ((WeightEntry) item).weightValue + " " + ((WeightEntry) item).unit;
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
        binding.buttonAddActivity.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ActivitySessionFormActivity.class);
            intent.putExtra(ActivitySessionFormActivity.EXTRA_PET_ID, petId);
            formLauncher.launch(intent);
        });
        binding.buttonAddWeight.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), WeightEntryFormActivity.class);
            intent.putExtra(WeightEntryFormActivity.EXTRA_PET_ID, petId);
            formLauncher.launch(intent);
        });
        reload();
        return binding.getRoot();
    }

    private void reload() {
        List<Object> items = new ArrayList<>();
        items.addAll(repository.getActivitySessions(petId));
        items.addAll(repository.getWeightEntries(petId));
        items.sort((left, right) -> Long.compare(getTime(right), getTime(left)));
        adapter.submitList(items);
        binding.sectionEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.weightChart.setEntries(repository.getWeightEntries(petId));

        Pet pet = repository.getPet(petId);
        if (pet != null) {
            int progress = repository.getWeeklyActivityProgressPercent(petId, pet.weeklyActivityGoalMinutes);
            binding.sectionSubtitle.setText("Weekly goal progress: " + progress + "%");
        }
    }

    private long getTime(Object item) {
        if (item instanceof ActivitySession) return ((ActivitySession) item).sessionDateEpochMillis;
        if (item instanceof WeightEntry) return ((WeightEntry) item).measuredAt;
        return 0L;
    }
}
