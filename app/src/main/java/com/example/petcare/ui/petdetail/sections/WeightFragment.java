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
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.WeightEntry;
import com.example.petcare.databinding.FragmentWeightSectionBinding;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.WeightEntryFormActivity;
import com.example.petcare.util.FormatUtils;

import java.util.List;
import java.util.Locale;

public class WeightFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentWeightSectionBinding binding;
    private PetRepository repository;
    private SimpleRowAdapter adapter;

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
                return "Measured weight entry";
            }

            @Override
            public String meta(WeightEntry item) {
                return FormatUtils.dateTime(item.measuredAt);
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

        reload();
        return binding.getRoot();
    }

    private void reload() {
        List<WeightEntry> entries = repository.getWeightEntries(petId);
        Pet pet = repository.getPet(petId);
        if (pet != null) {
            for (WeightEntry entry : entries) {
                entry.healthyMin = pet.minHealthyWeight;
                entry.healthyMax = pet.maxHealthyWeight;
            }
        }
        binding.weightChart.setEntries(entries);
        adapter.submitList(entries);
        binding.weightEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
