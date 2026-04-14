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
import com.example.petcare.data.entities.SymptomEntry;
import com.example.petcare.databinding.FragmentSymptomsSectionBinding;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.SymptomEntryFormActivity;
import com.example.petcare.util.FormatUtils;

import java.util.List;

public class SymptomsFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentSymptomsSectionBinding binding;
    private PetRepository repository;
    private SimpleRowAdapter adapter;

    private final ActivityResultLauncher<Intent> formLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reload());

    public static SymptomsFragment newInstance(long petId) {
        SymptomsFragment fragment = new SymptomsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PET_ID, petId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSymptomsSectionBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        adapter = new SimpleRowAdapter(new SimpleRowAdapter.RowMapper<SymptomEntry>() {
            @Override
            public String title(SymptomEntry item) {
                return item.tagsCsv;
            }

            @Override
            public String subtitle(SymptomEntry item) {
                return "Severity: " + item.severity;
            }

            @Override
            public String meta(SymptomEntry item) {
                return FormatUtils.dateTime(item.recordedAt) + " • " + FormatUtils.nullable(item.notes);
            }
        });

        adapter.setOnRowClickListener(item -> {
            Intent intent = new Intent(requireContext(), SymptomEntryFormActivity.class);
            intent.putExtra(SymptomEntryFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(SymptomEntryFormActivity.EXTRA_SYMPTOM_ID, ((SymptomEntry) item).id);
            formLauncher.launch(intent);
        });

        binding.sectionRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.sectionRecycler.setAdapter(adapter);
        binding.buttonAddSymptom.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SymptomEntryFormActivity.class);
            intent.putExtra(SymptomEntryFormActivity.EXTRA_PET_ID, petId);
            formLauncher.launch(intent);
        });
        reload();
        return binding.getRoot();
    }

    private void reload() {
        List<SymptomEntry> items = repository.getSymptomEntries(petId);
        adapter.submitList(items);
        binding.sectionEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.symptomChart.setEntries(items);
    }
}
