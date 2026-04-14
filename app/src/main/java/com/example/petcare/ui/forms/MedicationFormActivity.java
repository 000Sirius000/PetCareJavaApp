package com.example.petcare.ui.forms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.VetVisit;
import com.example.petcare.databinding.ActivityMedicationFormBinding;
import com.example.petcare.reminders.ReminderScheduler;
import com.example.petcare.ui.common.FormUiUtils;
import com.example.petcare.util.FormatUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class MedicationFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_MEDICATION_ID = "extra_medication_id";

    private ActivityMedicationFormBinding binding;
    private PetRepository repository;
    private Medication editing;
    private List<VetVisit> visits = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMedicationFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new PetRepository(this);

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        long petId = getIntent().getLongExtra(EXTRA_PET_ID, 0L);
        visits = repository.getVetVisits(petId);
        setupSpinners();

        long id = getIntent().getLongExtra(EXTRA_MEDICATION_ID, 0L);
        if (id > 0) {
            editing = repository.getDb().medicationDao().getById(id);
            if (editing != null) populate();
        } else {
            binding.inputStartDate.setTag(System.currentTimeMillis());
            binding.inputStartDate.setText(FormatUtils.date(System.currentTimeMillis()));
            binding.inputIntervalDays.setText("1");
        }

        binding.inputStartDate.setOnClickListener(v -> FormUiUtils.showDatePicker(this, readTag(binding.inputStartDate), binding.inputStartDate, null));
        binding.inputEndDate.setOnClickListener(v -> FormUiUtils.showDatePicker(this, readTag(binding.inputEndDate), binding.inputEndDate, null));
        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setupSpinners() {
        ArrayAdapter<String> frequencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Once daily", "Twice daily", "Every N days", "Custom"});
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputFrequency.setAdapter(frequencyAdapter);

        List<String> visitTitles = new ArrayList<>();
        visitTitles.add("No linked visit");
        for (VetVisit visit : visits) {
            visitTitles.add(FormatUtils.date(visit.visitDateEpochMillis) + " • " + visit.reason);
        }
        ArrayAdapter<String> visitsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, visitTitles);
        visitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputLinkedVisit.setAdapter(visitsAdapter);
    }

    private void populate() {
        binding.toolbar.setTitle("Edit medication");
        binding.inputName.setText(editing.medicationName);
        binding.inputDosage.setText(editing.dosage);
        binding.inputDosageUnit.setText(editing.dosageUnit);
        binding.inputIntervalDays.setText(String.valueOf(Math.max(1, editing.frequencyIntervalDays)));
        binding.inputStartDate.setTag(editing.startDateEpochMillis);
        binding.inputStartDate.setText(FormatUtils.date(editing.startDateEpochMillis));
        if (editing.endDateEpochMillis != null) {
            binding.inputEndDate.setTag(editing.endDateEpochMillis);
            binding.inputEndDate.setText(FormatUtils.date(editing.endDateEpochMillis));
        }
        binding.checkArchived.setChecked(editing.archived);
        selectSpinnerValue(binding.inputFrequency, editing.frequencyType);
        if (editing.linkedVisitId != 0) {
            for (int i = 0; i < visits.size(); i++) {
                if (visits.get(i).id == editing.linkedVisitId) {
                    binding.inputLinkedVisit.setSelection(i + 1);
                    break;
                }
            }
        }
        binding.buttonDelete.setVisibility(android.view.View.VISIBLE);
    }

    private void save() {
        Medication medication = editing == null ? new Medication() : editing;
        medication.petId = getIntent().getLongExtra(EXTRA_PET_ID, editing == null ? 0L : editing.petId);
        medication.medicationName = text(binding.inputName);
        medication.dosage = text(binding.inputDosage);
        medication.dosageUnit = text(binding.inputDosageUnit);
        medication.frequencyType = String.valueOf(binding.inputFrequency.getSelectedItem());
        medication.frequencyIntervalDays = parseInt(text(binding.inputIntervalDays), 1);
        medication.startDateEpochMillis = readTag(binding.inputStartDate);
        medication.endDateEpochMillis = binding.inputEndDate.getText() == null || binding.inputEndDate.getText().toString().trim().isEmpty() ? null : readTag(binding.inputEndDate);
        medication.archived = binding.checkArchived.isChecked();
        medication.linkedVisitId = binding.inputLinkedVisit.getSelectedItemPosition() <= 0 ? 0 : visits.get(binding.inputLinkedVisit.getSelectedItemPosition() - 1).id;
        medication.nextReminderAt = medication.startDateEpochMillis + (Math.max(1, medication.frequencyIntervalDays) * 24L * 60 * 60 * 1000);

        if (medication.medicationName.isEmpty()) {
            toast("Medication name is required");
            return;
        }

        if (medication.id == 0) {
            medication.id = repository.getDb().medicationDao().insert(medication);
        } else {
            repository.getDb().medicationDao().update(medication);
        }
        if (!medication.archived) {
            ReminderScheduler.scheduleMedication(this, medication);
        }
        setResult(RESULT_OK, new Intent());
        finish();
    }

    private void confirmDelete() {
        if (editing == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.confirm_delete)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    repository.getDb().medicationDao().delete(editing);
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    private void selectSpinnerValue(android.widget.Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equals(String.valueOf(spinner.getItemAtPosition(i)))) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception e) { return fallback; }
    }
    private long readTag(android.widget.EditText field) {
        Object tag = field.getTag();
        return tag instanceof Long ? (Long) tag : System.currentTimeMillis();
    }
    private String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
