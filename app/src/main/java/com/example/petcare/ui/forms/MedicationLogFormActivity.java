package com.example.petcare.ui.forms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.MedicationLog;
import com.example.petcare.databinding.ActivityMedicationLogFormBinding;
import com.example.petcare.ui.common.FormUiUtils;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MedicationLogFormActivity extends AppCompatActivity {
    public static final String EXTRA_LOG_ID = "extra_log_id";

    private ActivityMedicationLogFormBinding binding;
    private PetRepository repository;
    private MedicationLog editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMedicationLogFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new PetRepository(this);

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        long logId = getIntent().getLongExtra(EXTRA_LOG_ID, 0L);
        editing = repository.getDb().medicationLogDao().getById(logId);
        if (editing == null) {
            toast("Medication log not found");
            finish();
            return;
        }

        populate();
        binding.inputCompletedAt.setOnClickListener(v -> FormUiUtils.showDateTimePicker(this, readTag(), binding.inputCompletedAt, null));
        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populate() {
        Medication medication = repository.getDb().medicationDao().getById(editing.medicationId);
        binding.inputName.setText(firstNonEmpty(editing.medicationName, medication == null ? null : medication.medicationName, "Medication"));
        binding.inputDosage.setText(firstNonEmpty(editing.dosage, medicationDose(medication), ""));
        binding.inputCompletedAt.setTag(editing.administeredAt);
        binding.inputCompletedAt.setText(FormatUtils.dateTime(editing.administeredAt));
        binding.checkMissed.setChecked(editing.missed);
    }

    private void save() {
        editing.medicationName = firstNonEmpty(text(binding.inputName), "Medication");
        editing.dosage = text(binding.inputDosage);
        editing.administeredAt = readTag();
        editing.missed = binding.checkMissed.isChecked();
        repository.getDb().medicationLogDao().update(editing);
        setResult(RESULT_OK, new Intent());
        finish();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.confirm_delete)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    repository.getDb().medicationLogDao().delete(editing);
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    private String medicationDose(Medication medication) {
        if (medication == null) return "";
        return FormatUtils.joinNonEmpty(" ", medication.dosage, medication.dosageUnit);
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private long readTag() {
        Object tag = binding.inputCompletedAt.getTag();
        return tag instanceof Long ? (Long) tag : System.currentTimeMillis();
    }

    private String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
