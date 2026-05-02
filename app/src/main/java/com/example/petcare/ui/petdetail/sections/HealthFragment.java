package com.example.petcare.ui.petdetail.sections;

import android.content.Intent;
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
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.ReproductiveEvent;
import com.example.petcare.data.entities.SymptomEntry;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.data.entities.VetVisit;
import com.example.petcare.databinding.FragmentHealthSectionBinding;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.MedicationFormActivity;
import com.example.petcare.ui.forms.ReproductiveEventFormActivity;
import com.example.petcare.ui.forms.SymptomEntryFormActivity;
import com.example.petcare.ui.forms.VaccinationFormActivity;
import com.example.petcare.ui.forms.VetVisitFormActivity;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.HealthPdfExporter;

import java.util.List;

public class HealthFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentHealthSectionBinding binding;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHealthSectionBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        adapter = new SimpleRowAdapter(new SimpleRowAdapter.RowMapper<Object>() {
            @Override
            public String title(Object item) {
                if (item instanceof VetVisit) return "Vet visit";
                if (item instanceof Vaccination) return "Vaccination";
                if (item instanceof Medication) return "Medication";
                if (item instanceof SymptomEntry) return "Symptom";
                if (item instanceof ReproductiveEvent) return "Reproductive: " + ((ReproductiveEvent) item).eventType;
                return "";
            }

            @Override
            public String subtitle(Object item) {
                if (item instanceof VetVisit) {
                    VetVisit visit = (VetVisit) item;
                    return visit.reason + " • " + visit.clinicName;
                }
                if (item instanceof Vaccination) {
                    return ((Vaccination) item).vaccineName;
                }
                if (item instanceof Medication) {
                    Medication medication = (Medication) item;
                    return medication.medicationName + " • " + medication.dosage + " " + medication.dosageUnit;
                }
                if (item instanceof SymptomEntry) {
                    SymptomEntry entry = (SymptomEntry) item;
                    return entry.tagsCsv + " • " + entry.severity;
                }
                if (item instanceof ReproductiveEvent) {
                    ReproductiveEvent entry = (ReproductiveEvent) item;
                    return FormatUtils.nullable(entry.symptomsObserved);
                }
                return "";
            }

            @Override
            public String meta(Object item) {
                if (item instanceof VetVisit) return FormatUtils.date(((VetVisit) item).visitDateEpochMillis);
                if (item instanceof Vaccination) return "Given: " + FormatUtils.date(((Vaccination) item).administeredAt);
                if (item instanceof Medication) return "Next dose: " + FormatUtils.dateTime(((Medication) item).nextReminderAt);
                if (item instanceof SymptomEntry) return FormatUtils.dateTime(((SymptomEntry) item).recordedAt);
                if (item instanceof ReproductiveEvent) return FormatUtils.date(((ReproductiveEvent) item).startDateEpochMillis);
                return "";
            }
        });

        adapter.setOnRowClickListener(item -> {
            Intent intent;
            if (item instanceof VetVisit) {
                intent = new Intent(requireContext(), VetVisitFormActivity.class);
                intent.putExtra(VetVisitFormActivity.EXTRA_PET_ID, petId);
                intent.putExtra(VetVisitFormActivity.EXTRA_VISIT_ID, ((VetVisit) item).id);
            } else if (item instanceof Vaccination) {
                intent = new Intent(requireContext(), VaccinationFormActivity.class);
                intent.putExtra(VaccinationFormActivity.EXTRA_PET_ID, petId);
                intent.putExtra(VaccinationFormActivity.EXTRA_VACCINATION_ID, ((Vaccination) item).id);
            } else if (item instanceof Medication) {
                intent = new Intent(requireContext(), MedicationFormActivity.class);
                intent.putExtra(MedicationFormActivity.EXTRA_PET_ID, petId);
                intent.putExtra(MedicationFormActivity.EXTRA_MEDICATION_ID, ((Medication) item).id);
            } else if (item instanceof SymptomEntry) {
                intent = new Intent(requireContext(), SymptomEntryFormActivity.class);
                intent.putExtra(SymptomEntryFormActivity.EXTRA_PET_ID, petId);
                intent.putExtra(SymptomEntryFormActivity.EXTRA_SYMPTOM_ID, ((SymptomEntry) item).id);
            } else if (item instanceof ReproductiveEvent) {
                intent = new Intent(requireContext(), ReproductiveEventFormActivity.class);
                intent.putExtra(ReproductiveEventFormActivity.EXTRA_PET_ID, petId);
                intent.putExtra(ReproductiveEventFormActivity.EXTRA_EVENT_ID, ((ReproductiveEvent) item).id);
            } else {
                return;
            }
            formLauncher.launch(intent);
        });

        binding.sectionRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.sectionRecycler.setAdapter(adapter);
        binding.sectionActionButton.setOnClickListener(this::showMenu);
        reload();
        return binding.getRoot();
    }

    private void reload() {
        List<Object> items = repository.getHealthTimeline(petId);
        items.sort((left, right) -> Long.compare(getItemTime(right), getItemTime(left)));
        adapter.submitList(items);
        binding.sectionEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private long getItemTime(Object item) {
        if (item instanceof VetVisit) return ((VetVisit) item).visitDateEpochMillis;
        if (item instanceof Vaccination) return ((Vaccination) item).administeredAt;
        if (item instanceof Medication) return ((Medication) item).startDateEpochMillis;
        if (item instanceof SymptomEntry) return ((SymptomEntry) item).recordedAt;
        if (item instanceof ReproductiveEvent) return ((ReproductiveEvent) item).startDateEpochMillis;
        return 0L;
    }

    private void showMenu(View anchor) {
        Pet pet = repository.getPet(petId);
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add("Add vet visit");
        menu.getMenu().add("Add vaccination");
        menu.getMenu().add("Add medication");
        menu.getMenu().add("Add symptom");
        if (pet != null && "female".equalsIgnoreCase(String.valueOf(pet.sex))) {
            menu.getMenu().add("Add reproductive event");
        }
        menu.getMenu().add("Export health PDF");
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            Intent intent = null;
            if ("Add vet visit".equals(title)) {
                intent = new Intent(requireContext(), VetVisitFormActivity.class);
                intent.putExtra(VetVisitFormActivity.EXTRA_PET_ID, petId);
            } else if ("Add vaccination".equals(title)) {
                intent = new Intent(requireContext(), VaccinationFormActivity.class);
                intent.putExtra(VaccinationFormActivity.EXTRA_PET_ID, petId);
            } else if ("Add medication".equals(title)) {
                intent = new Intent(requireContext(), MedicationFormActivity.class);
                intent.putExtra(MedicationFormActivity.EXTRA_PET_ID, petId);
            } else if ("Add symptom".equals(title)) {
                intent = new Intent(requireContext(), SymptomEntryFormActivity.class);
                intent.putExtra(SymptomEntryFormActivity.EXTRA_PET_ID, petId);
            } else if ("Add reproductive event".equals(title)) {
                intent = new Intent(requireContext(), ReproductiveEventFormActivity.class);
                intent.putExtra(ReproductiveEventFormActivity.EXTRA_PET_ID, petId);
            } else if ("Export health PDF".equals(title)) {
                pdfExportLauncher.launch("pet_health_" + petId + ".pdf");
                return true;
            }
            if (intent != null) formLauncher.launch(intent);
            return true;
        });
        menu.show();
    }
}
