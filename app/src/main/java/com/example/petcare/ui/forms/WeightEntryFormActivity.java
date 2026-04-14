package com.example.petcare.ui.forms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.WeightEntry;
import com.example.petcare.databinding.ActivityWeightEntryFormBinding;
import com.example.petcare.ui.common.FormUiUtils;
import com.example.petcare.util.FormatUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class WeightEntryFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_WEIGHT_ID = "extra_weight_id";

    private ActivityWeightEntryFormBinding binding;
    private PetRepository repository;
    private WeightEntry editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWeightEntryFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new PetRepository(this);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"kg", "lbs"});
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputUnit.setAdapter(unitAdapter);

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        long id = getIntent().getLongExtra(EXTRA_WEIGHT_ID, 0L);
        if (id > 0) {
            editing = repository.getDb().weightEntryDao().getById(id);
            if (editing != null) populate();
        } else {
            binding.inputDate.setTag(System.currentTimeMillis());
            binding.inputDate.setText(FormatUtils.date(System.currentTimeMillis()));
        }

        binding.inputDate.setOnClickListener(v -> FormUiUtils.showDatePicker(this, readTag(), binding.inputDate, null));
        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populate() {
        binding.toolbar.setTitle("Edit weight");
        binding.inputDate.setTag(editing.measuredAt);
        binding.inputDate.setText(FormatUtils.date(editing.measuredAt));
        binding.inputWeight.setText(String.valueOf(editing.weightValue));
        selectSpinnerValue(binding.inputUnit, editing.unit);
        binding.inputHealthyMin.setText(editing.healthyMin == null ? "" : String.valueOf(editing.healthyMin));
        binding.inputHealthyMax.setText(editing.healthyMax == null ? "" : String.valueOf(editing.healthyMax));
        binding.buttonDelete.setVisibility(android.view.View.VISIBLE);
    }

    private void save() {
        WeightEntry entry = editing == null ? new WeightEntry() : editing;
        entry.petId = getIntent().getLongExtra(EXTRA_PET_ID, editing == null ? 0L : editing.petId);
        entry.measuredAt = readTag();
        entry.weightValue = parseDouble(text(binding.inputWeight), 0);
        entry.unit = String.valueOf(binding.inputUnit.getSelectedItem());
        entry.healthyMin = text(binding.inputHealthyMin).isEmpty() ? null : parseDouble(text(binding.inputHealthyMin), 0);
        entry.healthyMax = text(binding.inputHealthyMax).isEmpty() ? null : parseDouble(text(binding.inputHealthyMax), 0);
        if (entry.weightValue <= 0) {
            toast("Weight must be greater than zero");
            return;
        }
        if (entry.id == 0) repository.getDb().weightEntryDao().insert(entry);
        else repository.getDb().weightEntryDao().update(entry);
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
                    repository.getDb().weightEntryDao().delete(editing);
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
        Object tag = binding.inputDate.getTag();
        return tag instanceof Long ? (Long) tag : System.currentTimeMillis();
    }

    private double parseDouble(String value, double fallback) {
        try { return Double.parseDouble(value); } catch (Exception e) { return fallback; }
    }

    private String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
