package com.example.petcare.ui.forms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.databinding.ActivityVaccinationFormBinding;
import com.example.petcare.ui.common.FormUiUtils;
import com.example.petcare.util.FormatUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class VaccinationFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_VACCINATION_ID = "extra_vaccination_id";

    private ActivityVaccinationFormBinding binding;
    private PetRepository repository;
    private Vaccination editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVaccinationFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new PetRepository(this);

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        long id = getIntent().getLongExtra(EXTRA_VACCINATION_ID, 0L);
        if (id > 0) {
            editing = repository.getDb().vaccinationDao().getById(id);
            if (editing != null) populate();
        } else {
            binding.inputAdministered.setTag(System.currentTimeMillis());
            binding.inputAdministered.setText(FormatUtils.date(System.currentTimeMillis()));
        }

        binding.inputAdministered.setOnClickListener(v -> FormUiUtils.showDatePicker(this, readTag(binding.inputAdministered), binding.inputAdministered, null));
        binding.inputNextDue.setOnClickListener(v -> FormUiUtils.showDatePicker(this, readTag(binding.inputNextDue), binding.inputNextDue, null));
        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populate() {
        binding.toolbar.setTitle("Edit vaccination");
        binding.inputName.setText(editing.vaccineName);
        binding.inputAdministered.setTag(editing.administeredAt);
        binding.inputAdministered.setText(FormatUtils.date(editing.administeredAt));
        if (editing.nextDueAt != null) {
            binding.inputNextDue.setTag(editing.nextDueAt);
            binding.inputNextDue.setText(FormatUtils.date(editing.nextDueAt));
        }
        binding.inputBatch.setText(editing.batchNumber);
        binding.buttonDelete.setVisibility(android.view.View.VISIBLE);
    }

    private void save() {
        Vaccination vaccination = editing == null ? new Vaccination() : editing;
        vaccination.petId = getIntent().getLongExtra(EXTRA_PET_ID, editing == null ? 0L : editing.petId);
        vaccination.vaccineName = text(binding.inputName);
        vaccination.administeredAt = readTag(binding.inputAdministered);
        vaccination.nextDueAt = binding.inputNextDue.getText() == null || binding.inputNextDue.getText().toString().trim().isEmpty() ? null : readTag(binding.inputNextDue);
        vaccination.batchNumber = text(binding.inputBatch);
        if (vaccination.vaccineName.isEmpty()) {
            toast("Vaccine name is required");
            return;
        }
        if (vaccination.id == 0) repository.getDb().vaccinationDao().insert(vaccination);
        else repository.getDb().vaccinationDao().update(vaccination);
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
                    repository.getDb().vaccinationDao().delete(editing);
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
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
