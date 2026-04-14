package com.example.petcare.ui.petdetail.sections;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.data.entities.VetVisit;
import com.example.petcare.databinding.FragmentSimpleListBinding;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.MedicationFormActivity;
import com.example.petcare.ui.forms.VaccinationFormActivity;
import com.example.petcare.ui.forms.VetVisitFormActivity;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.HealthPdfExporter;

import java.util.ArrayList;
import java.util.List;

public class HealthFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentSimpleListBinding binding;
    private PetRepository repository;
    private SimpleRowAdapter adapter;

    private final ActivityResultLauncher<Intent> formLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reload());

    private final ActivityResultLauncher<String> pdfExportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"), uri -> {
                if (uri == null) return;
                try {
                    HealthPdfExporter.exportPetHealthRecordToUri(requireContext(), petId, uri);
                    Toast.makeText(requireContext(), "PDF exported", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Failed to export PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

    public static HealthFragment newInstance(long petId) {
        HealthFragment fragment = new HealthFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PET_ID, petId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSimpleListBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        binding.sectionTitle.setText("Health timeline");
        binding.sectionSubtitle.setText("Vet visits and vaccinations in reverse chronological order");
        binding.sectionActionButton.setText("Add entry / export PDF");

        adapter = new SimpleRowAdapter(new SimpleRowAdapter.RowMapper<Object>() {
            @Override
            public String title(Object item) {
                if (item instanceof VetVisit) return "Vet visit";
                if (item instanceof Vaccination) return "Vaccination";
                return "";
            }

            @Override
            public String subtitle(Object item) {
                if (item instanceof VetVisit) {
                    VetVisit visit = (VetVisit) item;
                    return visit.reason + " • " + visit.clinicName;
                }
                if (item instanceof Vaccination) {
                    Vaccination vaccination = (Vaccination) item;
                    return vaccination.vaccineName;
                }
                return "";
            }

            @Override
            public String meta(Object item) {
                if (item instanceof VetVisit) {
                    VetVisit visit = (VetVisit) item;
                    return FormatUtils.date(visit.visitDateEpochMillis) + " • " + FormatUtils.nullable(visit.diagnosisNotes);
                }
                if (item instanceof Vaccination) {
                    Vaccination vaccination = (Vaccination) item;
                    return "Given: " + FormatUtils.date(vaccination.administeredAt);
                }
                return "";
            }
        });

        adapter.setOnRowClickListener(item -> {
            if (item instanceof VetVisit) {
                Intent intent = new Intent(requireContext(), VetVisitFormActivity.class);
                intent.putExtra(VetVisitFormActivity.EXTRA_PET_ID, petId);
                intent.putExtra(VetVisitFormActivity.EXTRA_VISIT_ID, ((VetVisit) item).id);
                formLauncher.launch(intent);
            } else if (item instanceof Vaccination) {
                Intent intent = new Intent(requireContext(), VaccinationFormActivity.class);
                intent.putExtra(VaccinationFormActivity.EXTRA_PET_ID, petId);
                intent.putExtra(VaccinationFormActivity.EXTRA_VACCINATION_ID, ((Vaccination) item).id);
                formLauncher.launch(intent);
            }
        });

        binding.sectionRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.sectionRecycler.setAdapter(adapter);
        binding.sectionActionButton.setOnClickListener(this::showMenu);
        reload();
        return binding.getRoot();
    }

    private void reload() {
        List<Object> items = new ArrayList<>();
        items.addAll(repository.getVetVisits(petId));
        items.addAll(repository.getVaccinations(petId));
        items.sort((left, right) -> Long.compare(getItemTime(right), getItemTime(left)));
        adapter.submitList(items);
        binding.sectionEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private long getItemTime(Object item) {
        if (item instanceof VetVisit) return ((VetVisit) item).visitDateEpochMillis;
        if (item instanceof Vaccination) return ((Vaccination) item).administeredAt;
        return 0L;
    }

    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add("Add vet visit");
        menu.getMenu().add("Add vaccination");
        menu.getMenu().add("Add medication");
        menu.getMenu().add("Export health PDF");
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            if ("Add vet visit".equals(title)) {
                Intent intent = new Intent(requireContext(), VetVisitFormActivity.class);
                intent.putExtra(VetVisitFormActivity.EXTRA_PET_ID, petId);
                formLauncher.launch(intent);
            } else if ("Add vaccination".equals(title)) {
                Intent intent = new Intent(requireContext(), VaccinationFormActivity.class);
                intent.putExtra(VaccinationFormActivity.EXTRA_PET_ID, petId);
                formLauncher.launch(intent);
            } else if ("Add medication".equals(title)) {
                Intent intent = new Intent(requireContext(), MedicationFormActivity.class);
                intent.putExtra(MedicationFormActivity.EXTRA_PET_ID, petId);
                formLauncher.launch(intent);
            } else if ("Export health PDF".equals(title)) {
                pdfExportLauncher.launch("pet_health_" + petId + ".pdf");
            }
            return true;
        });
        menu.show();
    }
}
