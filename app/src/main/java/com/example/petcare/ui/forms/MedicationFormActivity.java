package com.example.petcare.ui.forms;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.VetVisit;
import com.example.petcare.databinding.ActivityMedicationFormBinding;
import com.example.petcare.reminders.MedicationScheduleCalculator;
import com.example.petcare.reminders.ReminderScheduler;
import com.example.petcare.ui.common.FormUiUtils;
import com.example.petcare.util.FormatUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicationFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_MEDICATION_ID = "extra_medication_id";

    private ActivityMedicationFormBinding binding;
    private PetRepository repository;
    private Medication editing;
    private List<VetVisit> visits = new ArrayList<>();
    private int firstReminderMinute = 9 * 60;
    private int secondReminderMinute = 21 * 60;

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
        if (id > 0L) {
            editing = repository.getDb().medicationDao().getById(id);
            if (editing != null) populate();
        } else {
            long today = System.currentTimeMillis();
            binding.inputStartDate.setTag(today);
            binding.inputStartDate.setText(FormatUtils.date(today));
            binding.inputIntervalDays.setText("1");
            binding.switchRemind.setChecked(true);
            applyWeekdayMask(127);
        }

        binding.inputFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long itemId) {
                updateScheduleVisibility();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        binding.switchRemind.setOnCheckedChangeListener((button, checked) -> updateScheduleVisibility());
        binding.buttonTimeOne.setOnClickListener(v -> showTimePicker(false));
        binding.buttonTimeTwo.setOnClickListener(v -> showTimePicker(true));
        binding.inputStartDate.setOnClickListener(v -> FormUiUtils.showDatePicker(
                this, readTag(binding.inputStartDate), binding.inputStartDate, null));
        binding.inputEndDate.setOnClickListener(v -> FormUiUtils.showDatePicker(
                this, readTag(binding.inputEndDate), binding.inputEndDate, null));
        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());

        updateTimeButtons();
        updateScheduleVisibility();
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
        binding.switchRemind.setChecked(editing.reminderEnabled);
        binding.checkArchived.setChecked(editing.archived);
        firstReminderMinute = validMinute(editing.reminderMinuteOfDay1, 9 * 60);
        secondReminderMinute = validMinute(editing.reminderMinuteOfDay2, 21 * 60);
        applyWeekdayMask(editing.reminderWeekdayMask);
        selectSpinnerValue(binding.inputFrequency, editing.frequencyType);
        if (editing.linkedVisitId != 0L) {
            for (int index = 0; index < visits.size(); index++) {
                if (visits.get(index).id == editing.linkedVisitId) {
                    binding.inputLinkedVisit.setSelection(index + 1);
                    break;
                }
            }
        }
        binding.buttonDelete.setVisibility(View.VISIBLE);
    }

    private void updateScheduleVisibility() {
        boolean enabled = binding.switchRemind.isChecked();
        binding.reminderScheduleGroup.setVisibility(enabled ? View.VISIBLE : View.GONE);
        String frequency = selectedFrequency();
        binding.layoutIntervalDays.setVisibility(enabled && "Every N days".equals(frequency) ? View.VISIBLE : View.GONE);
        binding.buttonTimeTwo.setVisibility(enabled && "Twice daily".equals(frequency) ? View.VISIBLE : View.GONE);
        binding.customWeekdaysGroup.setVisibility(enabled && "Custom".equals(frequency) ? View.VISIBLE : View.GONE);
        updateTimeButtons();
    }

    private void showTimePicker(boolean second) {
        int current = second ? secondReminderMinute : firstReminderMinute;
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            int value = hourOfDay * 60 + minute;
            if (second) secondReminderMinute = value;
            else firstReminderMinute = value;
            updateTimeButtons();
        }, current / 60, current % 60, true).show();
    }

    private void updateTimeButtons() {
        boolean twice = "Twice daily".equals(selectedFrequency());
        binding.buttonTimeOne.setText((twice ? "First reminder: " : "Reminder time: ") + timeLabel(firstReminderMinute));
        binding.buttonTimeTwo.setText("Second reminder: " + timeLabel(secondReminderMinute));
    }

    private void save() {
        Medication medication = editing == null ? new Medication() : editing;
        medication.petId = getIntent().getLongExtra(EXTRA_PET_ID, editing == null ? 0L : editing.petId);
        medication.medicationName = text(binding.inputName);
        medication.dosage = text(binding.inputDosage);
        medication.dosageUnit = text(binding.inputDosageUnit);
        medication.frequencyType = selectedFrequency();
        medication.frequencyIntervalDays = "Every N days".equals(medication.frequencyType)
                ? parseInt(text(binding.inputIntervalDays), 0)
                : 1;
        medication.startDateEpochMillis = readTag(binding.inputStartDate);
        medication.endDateEpochMillis = binding.inputEndDate.getText() == null
                || binding.inputEndDate.getText().toString().trim().isEmpty()
                ? null
                : readTag(binding.inputEndDate);
        medication.reminderEnabled = binding.switchRemind.isChecked();
        medication.reminderMinuteOfDay1 = firstReminderMinute;
        medication.reminderMinuteOfDay2 = secondReminderMinute;
        medication.reminderWeekdayMask = weekdayMask();
        medication.archived = binding.checkArchived.isChecked();
        medication.linkedVisitId = binding.inputLinkedVisit.getSelectedItemPosition() <= 0
                ? 0L
                : visits.get(binding.inputLinkedVisit.getSelectedItemPosition() - 1).id;

        if (medication.medicationName.isEmpty()) {
            toast("Medication name is required");
            return;
        }
        if (medication.endDateEpochMillis != null
                && medication.endDateEpochMillis < medication.startDateEpochMillis) {
            toast("End date cannot be before start date");
            return;
        }
        if (medication.reminderEnabled && "Every N days".equals(medication.frequencyType)
                && medication.frequencyIntervalDays < 1) {
            toast("Enter a number of days greater than zero");
            return;
        }
        if (medication.reminderEnabled && "Twice daily".equals(medication.frequencyType)
                && firstReminderMinute == secondReminderMinute) {
            toast("Choose two different reminder times");
            return;
        }
        if (medication.reminderEnabled && "Custom".equals(medication.frequencyType)
                && medication.reminderWeekdayMask == 0) {
            toast("Choose at least one weekday");
            return;
        }

        medication.nextReminderAt = medication.reminderEnabled && !medication.archived
                ? MedicationScheduleCalculator.nextOccurrence(medication, System.currentTimeMillis() - 1L)
                : 0L;

        if (medication.id == 0L) medication.id = repository.getDb().medicationDao().insert(medication);
        else repository.getDb().medicationDao().update(medication);

        if (medication.reminderEnabled && !medication.archived && medication.nextReminderAt > 0L) {
            ReminderScheduler.scheduleMedication(this, medication);
        } else {
            ReminderScheduler.cancelMedication(this, medication.id);
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
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    ReminderScheduler.cancelMedication(this, editing.id);
                    repository.getDb().medicationDao().delete(editing);
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    private String selectedFrequency() {
        Object selected = binding.inputFrequency.getSelectedItem();
        return selected == null ? "Once daily" : String.valueOf(selected);
    }

    private int weekdayMask() {
        CheckBox[] days = weekdayBoxes();
        int mask = 0;
        for (int index = 0; index < days.length; index++) {
            if (days[index].isChecked()) mask |= 1 << index;
        }
        return mask;
    }

    private void applyWeekdayMask(int mask) {
        CheckBox[] days = weekdayBoxes();
        for (int index = 0; index < days.length; index++) {
            days[index].setChecked((mask & (1 << index)) != 0);
        }
    }

    private CheckBox[] weekdayBoxes() {
        return new CheckBox[]{
                binding.dayMonday,
                binding.dayTuesday,
                binding.dayWednesday,
                binding.dayThursday,
                binding.dayFriday,
                binding.daySaturday,
                binding.daySunday
        };
    }

    private void selectSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        for (int index = 0; index < spinner.getCount(); index++) {
            if (value.equals(String.valueOf(spinner.getItemAtPosition(index)))) {
                spinner.setSelection(index);
                return;
            }
        }
    }

    private int validMinute(int value, int fallback) {
        return value >= 0 && value < 24 * 60 ? value : fallback;
    }

    private String timeLabel(int minuteOfDay) {
        return String.format(Locale.getDefault(), "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60);
    }

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (Exception ignored) { return fallback; }
    }

    private long readTag(EditText field) {
        Object tag = field.getTag();
        return tag instanceof Long ? (Long) tag : System.currentTimeMillis();
    }

    private String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
