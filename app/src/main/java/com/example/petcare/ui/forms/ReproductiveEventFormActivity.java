package com.example.petcare.ui.forms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.ReproductiveEvent;
import com.example.petcare.databinding.ActivityReproductiveEventFormBinding;
import com.example.petcare.ui.common.FormUiUtils;
import com.example.petcare.util.FormatUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ReproductiveEventFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private ActivityReproductiveEventFormBinding binding;
    private PetRepository repository;
    private ReproductiveEvent editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReproductiveEventFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new PetRepository(this);

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this, R.array.reproductive_event_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputEventType.setAdapter(typeAdapter);

        binding.inputStartDate.setOnClickListener(v -> FormUiUtils.showDatePicker(this, readTag(binding.inputStartDate), binding.inputStartDate, null));
        binding.inputEstimatedEndDate.setOnClickListener(v -> FormUiUtils.showDatePicker(this, readTag(binding.inputEstimatedEndDate), binding.inputEstimatedEndDate, null));
        binding.inputResolutionDate.setOnClickListener(v -> FormUiUtils.showDatePicker(this, readTag(binding.inputResolutionDate), binding.inputResolutionDate, null));

        long id = getIntent().getLongExtra(EXTRA_EVENT_ID, 0L);
        if (id > 0) {
            editing = repository.getDb().reproductiveEventDao().getById(id);
            if (editing != null) populate();
        } else {
            long now = System.currentTimeMillis();
            binding.inputStartDate.setTag(now);
            binding.inputStartDate.setText(FormatUtils.date(now));
        }

        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populate() {
        binding.toolbar.setTitle("Edit reproductive event");
        selectSpinnerValue(String.valueOf(editing.eventType));
        bindDate(binding.inputStartDate, editing.startDateEpochMillis);
        bindDate(binding.inputEstimatedEndDate, editing.estimatedEndDateEpochMillis);
        bindDate(binding.inputResolutionDate, editing.resolutionDateEpochMillis);
        binding.inputClinic.setText(editing.clinic);
        binding.inputSymptoms.setText(editing.symptomsObserved);
        binding.inputNotes.setText(editing.notes);
        binding.checkboxVetConsulted.setChecked(editing.vetConsulted);
        binding.buttonDelete.setVisibility(android.view.View.VISIBLE);
    }

    private void save() {
        ReproductiveEvent item = editing == null ? new ReproductiveEvent() : editing;
        item.petId = getIntent().getLongExtra(EXTRA_PET_ID, editing == null ? 0L : editing.petId);
        item.eventType = String.valueOf(binding.inputEventType.getSelectedItem());
        item.startDateEpochMillis = readTag(binding.inputStartDate);
        item.estimatedEndDateEpochMillis = optionalTag(binding.inputEstimatedEndDate);
        item.resolutionDateEpochMillis = optionalTag(binding.inputResolutionDate);
        item.clinic = text(binding.inputClinic);
        item.symptomsObserved = text(binding.inputSymptoms);
        item.notes = text(binding.inputNotes);
        item.vetConsulted = binding.checkboxVetConsulted.isChecked();

        if (item.id == 0) repository.getDb().reproductiveEventDao().insert(item);
        else repository.getDb().reproductiveEventDao().update(item);
        setResult(RESULT_OK, new Intent());
        finish();
    }

    private void confirmDelete() {
        if (editing == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.confirm_delete)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.getDb().reproductiveEventDao().delete(editing);
                    setResult(RESULT_OK);
                    finish();
                }).show();
    }

    private void selectSpinnerValue(String value) {
        for (int i = 0; i < binding.inputEventType.getCount(); i++) {
            if (value.equals(String.valueOf(binding.inputEventType.getItemAtPosition(i)))) {
                binding.inputEventType.setSelection(i);
                break;
            }
        }
    }

    private void bindDate(android.widget.EditText field, Long value) {
        if (value == null) return;
        field.setTag(value);
        field.setText(FormatUtils.date(value));
    }

    private long readTag(android.widget.EditText field) {
        Object tag = field.getTag();
        return tag instanceof Long ? (Long) tag : System.currentTimeMillis();
    }

    private Long optionalTag(android.widget.EditText field) {
        Object tag = field.getTag();
        return tag instanceof Long ? (Long) tag : null;
    }

    private String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
