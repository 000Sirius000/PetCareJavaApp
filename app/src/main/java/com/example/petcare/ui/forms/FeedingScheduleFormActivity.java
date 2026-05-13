package com.example.petcare.ui.forms;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.databinding.ActivityFeedingScheduleFormBinding;
import com.example.petcare.reminders.ReminderScheduler;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;

public class FeedingScheduleFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_SCHEDULE_ID = "extra_schedule_id";

    private static final String[] FOOD_TYPES = {"Natural", "Dry food", "Wet food (canned)"};

    private ActivityFeedingScheduleFormBinding binding;
    private PetRepository repository;
    private FeedingSchedule editing;

    /**
     * Works the same way as manual Activity entry:
     * one field opens DatePicker first, then TimePicker, and stores one combined timestamp.
     */
    private long selectedDateTime = System.currentTimeMillis();

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

        binding.inputDateTime.setKeyListener(null);
        binding.inputDateTime.setFocusable(false);
        binding.inputDateTime.setClickable(true);
        binding.inputDateTime.setOnClickListener(v -> showDatePicker());

        long id = getIntent().getLongExtra(EXTRA_SCHEDULE_ID, 0L);
        if (id > 0L) {
            editing = repository.getDb().feedingScheduleDao().getById(id);
            if (editing != null) {
                populate();
            } else {
                updateDateTimeText();
            }
        } else {
            binding.inputFoodType.setText("Dry food");
            updateDateTimeText();
        }

        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populate() {
        binding.toolbar.setTitle("Edit feeding");
        binding.inputMealName.setText(editing.mealName);
        binding.inputFoodType.setText(normalizeFoodType(editing.foodType));
        binding.inputPortion.setText(editing.portion);

        selectedDateTime = editing.createdAtEpochMillis > 0L
                ? editing.createdAtEpochMillis
                : combineStoredDateAndTime(editing.hourOfDay, editing.minute);

        updateDateTimeText();
        binding.buttonDelete.setVisibility(android.view.View.VISIBLE);
    }

    private long combineStoredDateAndTime(int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void chooseFoodType() {
        int checked = 1;
        String current = text(binding.inputFoodType);
        for (int i = 0; i < FOOD_TYPES.length; i++) {
            if (FOOD_TYPES[i].equalsIgnoreCase(current)) {
                checked = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Food type")
                .setSingleChoiceItems(FOOD_TYPES, checked, (dialog, which) -> {
                    binding.inputFoodType.setText(FOOD_TYPES[which]);
                    dialog.dismiss();
                })
                .show();
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedDateTime);

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.setTimeInMillis(selectedDateTime);
                    picked.set(Calendar.YEAR, year);
                    picked.set(Calendar.MONTH, month);
                    picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDateTime = picked.getTimeInMillis();
                    showTimePicker();
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedDateTime);

        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.setTimeInMillis(selectedDateTime);
                    picked.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    picked.set(Calendar.MINUTE, minute);
                    picked.set(Calendar.SECOND, 0);
                    picked.set(Calendar.MILLISECOND, 0);
                    selectedDateTime = picked.getTimeInMillis();
                    updateDateTimeText();
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void updateDateTimeText() {
        binding.inputDateTime.setText(FormatUtils.dateTime(selectedDateTime));
    }

    private void save() {
        FeedingSchedule schedule = editing == null ? new FeedingSchedule() : editing;
        schedule.petId = getIntent().getLongExtra(EXTRA_PET_ID, editing == null ? 0L : editing.petId);
        schedule.mealName = text(binding.inputMealName);
        schedule.foodType = normalizeFoodType(text(binding.inputFoodType));
        schedule.portion = text(binding.inputPortion);
        schedule.portionUnit = "g";

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedDateTime);
        schedule.hourOfDay = c.get(Calendar.HOUR_OF_DAY);
        schedule.minute = c.get(Calendar.MINUTE);
        schedule.createdAtEpochMillis = selectedDateTime;

        if (schedule.mealName.isEmpty()) {
            toast("Meal name is required");
            return;
        }
        if (schedule.portion.isEmpty()) {
            toast("Portion is required");
            return;
        }

        if (schedule.id == 0L) {
            schedule.id = repository.getDb().feedingScheduleDao().insert(schedule);
        } else {
            ReminderScheduler.cancelFeeding(this, schedule.id);
            repository.getDb().feedingScheduleDao().update(schedule);
        }

        ReminderScheduler.cancelFeeding(this, schedule.id);
        setResult(RESULT_OK, new Intent());
        finish();
    }

    private String normalizeFoodType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Dry food";
        }

        for (String type : FOOD_TYPES) {
            if (type.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }

        return "Dry food";
    }

    private void confirmDelete() {
        if (editing == null) {
            return;
        }

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

    private String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
