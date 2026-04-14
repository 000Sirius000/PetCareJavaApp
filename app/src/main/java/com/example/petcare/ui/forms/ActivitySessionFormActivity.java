package com.example.petcare.ui.forms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.databinding.ActivityActivitySessionFormBinding;
import com.example.petcare.ui.common.FormUiUtils;
import com.example.petcare.util.FormatUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ActivitySessionFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_ACTIVITY_ID = "extra_activity_id";

    private ActivityActivitySessionFormBinding binding;
    private PetRepository repository;
    private ActivitySession editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityActivitySessionFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new PetRepository(this);

        ArrayAdapter<CharSequence> activityAdapter = ArrayAdapter.createFromResource(this, R.array.activity_types, android.R.layout.simple_spinner_item);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputActivityType.setAdapter(activityAdapter);

        ArrayAdapter<String> distanceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"km", "miles"});
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputDistanceUnit.setAdapter(distanceAdapter);

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        long id = getIntent().getLongExtra(EXTRA_ACTIVITY_ID, 0L);
        if (id > 0) {
            editing = repository.getDb().activitySessionDao().getById(id);
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
        binding.toolbar.setTitle("Edit activity");
        selectSpinnerValue(binding.inputActivityType, editing.activityType);
        selectSpinnerValue(binding.inputDistanceUnit, editing.distanceUnit);
        binding.inputDuration.setText(String.valueOf(editing.durationMinutes));
        binding.inputDistance.setText(editing.distance == null ? "" : String.valueOf(editing.distance));
        binding.inputDate.setTag(editing.sessionDateEpochMillis);
        binding.inputDate.setText(FormatUtils.date(editing.sessionDateEpochMillis));
        binding.inputNotes.setText(editing.notes);
        binding.buttonDelete.setVisibility(android.view.View.VISIBLE);
    }

    private void save() {
        ActivitySession session = editing == null ? new ActivitySession() : editing;
        session.petId = getIntent().getLongExtra(EXTRA_PET_ID, editing == null ? 0L : editing.petId);
        session.activityType = String.valueOf(binding.inputActivityType.getSelectedItem());
        session.durationMinutes = parseInt(text(binding.inputDuration), 0);
        session.distance = text(binding.inputDistance).isEmpty() ? null : Double.parseDouble(text(binding.inputDistance));
        session.distanceUnit = String.valueOf(binding.inputDistanceUnit.getSelectedItem());
        session.sessionDateEpochMillis = readTag();
        session.notes = text(binding.inputNotes);
        if (session.durationMinutes <= 0) {
            toast("Duration must be greater than zero");
            return;
        }
        if (session.id == 0) repository.getDb().activitySessionDao().insert(session);
        else repository.getDb().activitySessionDao().update(session);
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
                    repository.getDb().activitySessionDao().delete(editing);
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

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception e) { return fallback; }
    }

    private String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
