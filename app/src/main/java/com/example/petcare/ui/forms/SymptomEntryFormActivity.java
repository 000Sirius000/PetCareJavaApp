package com.example.petcare.ui.forms;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.SymptomEntry;
import com.example.petcare.data.entities.SymptomTag;
import com.example.petcare.data.entities.VetVisit;
import com.example.petcare.databinding.ActivitySymptomEntryFormBinding;
import com.example.petcare.ui.common.FormUiUtils;
import com.example.petcare.util.FormatUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SymptomEntryFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_SYMPTOM_ID = "extra_symptom_id";

    private ActivitySymptomEntryFormBinding binding;
    private PetRepository repository;
    private SymptomEntry editing;
    private final Set<String> selectedTags = new LinkedHashSet<>();
    private List<SymptomTag> availableTags = new ArrayList<>();
    private List<VetVisit> vetVisits = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySymptomEntryFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new PetRepository(this);

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        availableTags = repository.getSymptomTags();
        long petId = getIntent().getLongExtra(EXTRA_PET_ID, 0L);
        vetVisits = repository.getVetVisits(petId);

        ArrayAdapter<CharSequence> severityAdapter = ArrayAdapter.createFromResource(this, R.array.severity_options, android.R.layout.simple_spinner_item);
        severityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputSeverity.setAdapter(severityAdapter);

        List<String> visitsAdapterItems = new ArrayList<>();
        visitsAdapterItems.add("No linked vet visit");
        for (VetVisit visit : vetVisits) {
            visitsAdapterItems.add(FormatUtils.date(visit.visitDateEpochMillis) + " • " + visit.reason);
        }
        ArrayAdapter<String> visitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, visitsAdapterItems);
        visitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputLinkedVisit.setAdapter(visitAdapter);

        long id = getIntent().getLongExtra(EXTRA_SYMPTOM_ID, 0L);
        if (id > 0) {
            editing = repository.getDb().symptomEntryDao().getById(id);
            if (editing != null) populate();
        } else {
            binding.inputRecordedAt.setTag(System.currentTimeMillis());
            binding.inputRecordedAt.setText(FormatUtils.dateTime(System.currentTimeMillis()));
        }

        binding.inputRecordedAt.setOnClickListener(v -> FormUiUtils.showDateTimePicker(this, readTag(), binding.inputRecordedAt, null));
        binding.buttonSelectTags.setOnClickListener(v -> selectTags());
        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populate() {
        binding.toolbar.setTitle("Edit symptom");
        binding.inputRecordedAt.setTag(editing.recordedAt);
        binding.inputRecordedAt.setText(FormatUtils.dateTime(editing.recordedAt));
        binding.inputNotes.setText(editing.notes);
        if (editing.tagsCsv != null) {
            for (String item : editing.tagsCsv.split(",")) {
                if (!item.trim().isEmpty()) selectedTags.add(item.trim());
            }
        }
        binding.textTags.setText(TextUtils.join(", ", selectedTags));
        selectSpinnerValue(binding.inputSeverity, editing.severity);
        if (editing.linkedVetVisitId != null) {
            for (int i = 0; i < vetVisits.size(); i++) {
                if (vetVisits.get(i).id == editing.linkedVetVisitId) {
                    binding.inputLinkedVisit.setSelection(i + 1);
                    break;
                }
            }
        }
        binding.buttonDelete.setVisibility(android.view.View.VISIBLE);
    }

    private void selectTags() {
        String[] names = new String[availableTags.size()];
        boolean[] checked = new boolean[availableTags.size()];
        for (int i = 0; i < availableTags.size(); i++) {
            names[i] = availableTags.get(i).name;
            checked[i] = selectedTags.contains(names[i]);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Select symptom tags")
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    if (isChecked) selectedTags.add(names[which]);
                    else selectedTags.remove(names[which]);
                })
                .setPositiveButton("Done", (d, w) -> binding.textTags.setText(selectedTags.isEmpty() ? "No tags selected" : TextUtils.join(", ", selectedTags)))
                .show();
    }

    private void save() {
        String customTag = text(binding.inputCustomTag);
        if (!customTag.isEmpty()) {
            selectedTags.add(customTag);
            boolean exists = false;
            for (SymptomTag tag : availableTags) {
                if (customTag.equalsIgnoreCase(tag.name)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                SymptomTag tag = new SymptomTag();
                tag.bodySystem = "Custom";
                tag.name = customTag;
                tag.customTag = true;
                repository.getDb().symptomTagDao().insert(tag);
            }
        }

        if (selectedTags.isEmpty()) {
            toast("Select at least one symptom tag");
            return;
        }

        SymptomEntry entry = editing == null ? new SymptomEntry() : editing;
        entry.petId = getIntent().getLongExtra(EXTRA_PET_ID, editing == null ? 0L : editing.petId);
        entry.recordedAt = readTag();
        entry.tagsCsv = TextUtils.join(", ", selectedTags);
        entry.severity = String.valueOf(binding.inputSeverity.getSelectedItem());
        entry.notes = text(binding.inputNotes);
        entry.linkedVetVisitId = binding.inputLinkedVisit.getSelectedItemPosition() <= 0 ? null : vetVisits.get(binding.inputLinkedVisit.getSelectedItemPosition() - 1).id;

        if (entry.id == 0) repository.getDb().symptomEntryDao().insert(entry);
        else repository.getDb().symptomEntryDao().update(entry);
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
                    repository.getDb().symptomEntryDao().delete(editing);
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

    private long readTag() {
        Object tag = binding.inputRecordedAt.getTag();
        return tag instanceof Long ? (Long) tag : System.currentTimeMillis();
    }

    private String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
