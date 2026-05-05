package com.example.petcare.ui.forms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.databinding.ActivityFeedingScheduleFormBinding;
import com.example.petcare.reminders.ReminderScheduler;
import com.example.petcare.util.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class FeedingScheduleFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_SCHEDULE_ID = "extra_schedule_id";
    private static final String[] FOOD_TYPES = {"Natural", "Dry food", "Wet food (canned)"};

    private ActivityFeedingScheduleFormBinding binding;
    private PetRepository repository;
    private FeedingSchedule editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityFeedingScheduleFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new PetRepository(this);

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.inputFoodType.setKeyListener(null);
        binding.inputFoodType.setFocusable(false);
        binding.inputFoodType.setClickable(true);
        binding.inputFoodType.setOnClickListener(v -> chooseFoodType());

        long id = getIntent().getLongExtra(EXTRA_SCHEDULE_ID, 0L);
        if (id > 0L) {
            editing = repository.getDb().feedingScheduleDao().getById(id);
            if (editing != null) populate();
        } else {
            binding.inputTime.setTag(new int[]{8, 0});
            binding.inputTime.setText("08:00");
            binding.inputFoodType.setText("Dry food");
        }

        binding.inputTime.setOnClickListener(v -> showTimePicker());
        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populate() {
        binding.toolbar.setTitle("Edit feeding schedule");
        binding.inputMealName.setText(editing.mealName);
        binding.inputFoodType.setText(normalizeFoodType(editing.foodType));
        binding.inputPortion.setText(editing.portion);
        binding.inputTime.setTag(new int[]{editing.hourOfDay, editing.minute});
        binding.inputTime.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", editing.hourOfDay, editing.minute));
        binding.buttonDelete.setVisibility(android.view.View.VISIBLE);
    }

    private void chooseFoodType() {
        int checked = 1;
        String current = text(binding.inputFoodType);
        for (int i = 0; i < FOOD_TYPES.length; i++) {
            if (FOOD_TYPES[i].equalsIgnoreCase(current)) checked = i;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Food type")
                .setSingleChoiceItems(FOOD_TYPES, checked, (dialog, which) -> {
                    binding.inputFoodType.setText(FOOD_TYPES[which]);
                    dialog.dismiss();
                })
                .show();
    }

    private void showTimePicker() {
        int[] time = readTimeTag();
        new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
            binding.inputTime.setTag(new int[]{hourOfDay, minute});
            binding.inputTime.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
        }, time[0], time[1], true).show();
    }

    private void save() {
        FeedingSchedule schedule = editing == null ? new FeedingSchedule() : editing;
        schedule.petId = getIntent().getLongExtra(EXTRA_PET_ID, editing == null ? 0L : editing.petId);
        schedule.mealName = text(binding.inputMealName);
        schedule.foodType = normalizeFoodType(text(binding.inputFoodType));
        schedule.portion = text(binding.inputPortion);
        schedule.portionUnit = "g";
        int[] time = readTimeTag();
        schedule.hourOfDay = time[0];
        schedule.minute = time[1];
        if (schedule.createdAtEpochMillis <= 0L) schedule.createdAtEpochMillis = System.currentTimeMillis();

        if (schedule.mealName.isEmpty()) { toast("Meal name is required"); return; }
        if (schedule.portion.isEmpty()) { toast("Portion is required"); return; }

        if (schedule.id == 0L) schedule.id = repository.getDb().feedingScheduleDao().insert(schedule);
        else {
            ReminderScheduler.cancelFeeding(this, schedule.id);
            repository.getDb().feedingScheduleDao().update(schedule);
        }
        ReminderScheduler.cancelFeeding(this, schedule.id);
        setResult(RESULT_OK, new Intent());
        finish();
    }

    private String normalizeFoodType(String value) {
        if (value == null || value.trim().isEmpty()) return "Dry food";
        for (String type : FOOD_TYPES) if (type.equalsIgnoreCase(value.trim())) return type;
        return "Dry food";
    }

    private void confirmDelete() {
        if (editing == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.confirm_delete)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    ReminderScheduler.cancelFeeding(this, editing.id);
                    repository.getDb().feedingScheduleDao().delete(editing);
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    private int[] readTimeTag() {
        Object tag = binding.inputTime.getTag();
        return tag instanceof int[] ? (int[]) tag : new int[]{8, 0};
    }

    private String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }
}
