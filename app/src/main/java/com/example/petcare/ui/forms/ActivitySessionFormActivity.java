package com.example.petcare.ui.forms;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.databinding.ActivityActivitySessionFormBinding;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;

public class ActivitySessionFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_ACTIVITY_ID = "extra_activity_id";

    private ActivityActivitySessionFormBinding binding;
    private PetRepository repository;
    private ActivitySession editing;
    private long selectedDateTime = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityActivitySessionFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new PetRepository(this);

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.activity_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputActivityType.setAdapter(adapter);

        long id = getIntent().getLongExtra(EXTRA_ACTIVITY_ID, 0L);
        if (id > 0L) {
            editing = repository.getDb().activitySessionDao().getById(id);
            if (editing != null) populate();
        } else {
            updateDateTimeText();
        }

        binding.inputDateTime.setOnClickListener(v -> showDatePicker());
        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populate() {
        binding.toolbar.setTitle("Edit activity");
        selectSpinnerValue(editing.activityType);
        binding.inputDuration.setText(String.valueOf(editing.durationMinutes));
        if (editing.distance != null) binding.inputDistance.setText(String.valueOf(editing.distance));
        binding.inputNotes.setText(editing.notes);
        selectedDateTime = editing.sessionDateEpochMillis;
        updateDateTimeText();
        binding.buttonDelete.setVisibility(android.view.View.VISIBLE);
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedDateTime);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.setTimeInMillis(selectedDateTime);
            picked.set(Calendar.YEAR, year);
            picked.set(Calendar.MONTH, month);
            picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            selectedDateTime = picked.getTimeInMillis();
            showTimePicker();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedDateTime);
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            Calendar picked = Calendar.getInstance();
            picked.setTimeInMillis(selectedDateTime);
            picked.set(Calendar.HOUR_OF_DAY, hourOfDay);
            picked.set(Calendar.MINUTE, minute);
            picked.set(Calendar.SECOND, 0);
            picked.set(Calendar.MILLISECOND, 0);
            selectedDateTime = picked.getTimeInMillis();
            updateDateTimeText();
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void updateDateTimeText() {
        binding.inputDateTime.setText(FormatUtils.dateTime(selectedDateTime));
    }

    private void save() {
        ActivitySession item = editing == null ? new ActivitySession() : editing;
        item.petId = getIntent().getLongExtra(EXTRA_PET_ID, editing == null ? 0L : editing.petId);
        item.activityType = String.valueOf(binding.inputActivityType.getSelectedItem());
        item.durationMinutes = Math.max(1, parseInt(text(binding.inputDuration), 1));
        Double distance = parseOptionalDouble(text(binding.inputDistance));
        item.distance = distance;
        item.distanceUnit = distance == null ? null : "km";
        item.sessionDateEpochMillis = selectedDateTime;
        item.notes = text(binding.inputNotes);

        if (item.id == 0L) repository.getDb().activitySessionDao().insert(item);
        else repository.getDb().activitySessionDao().update(item);

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
                    repository.getDb().activitySessionDao().delete(editing);
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    private void selectSpinnerValue(String value) {
        if (value == null) return;
        for (int i = 0; i < binding.inputActivityType.getCount(); i++) {
            if (value.equals(String.valueOf(binding.inputActivityType.getItemAtPosition(i)))) {
                binding.inputActivityType.setSelection(i);
                return;
            }
        }
    }

    private String text(android.widget.TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception e) { return fallback; }
    }

    private Double parseOptionalDouble(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return Double.parseDouble(value.trim().replace(',', '.')); }
        catch (Exception e) {
            Toast.makeText(this, "Distance must be a number in kilometres", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
