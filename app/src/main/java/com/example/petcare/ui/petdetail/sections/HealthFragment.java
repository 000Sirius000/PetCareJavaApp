package com.example.petcare.ui.petdetail.sections;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.MedicationLog;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.ReproductiveEvent;
import com.example.petcare.data.entities.SymptomEntry;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.data.entities.VetVisit;
import com.example.petcare.databinding.FragmentHealthSectionBinding;
import com.example.petcare.ui.forms.MedicationFormActivity;
import com.example.petcare.ui.forms.ReproductiveEventFormActivity;
import com.example.petcare.ui.forms.SymptomEntryFormActivity;
import com.example.petcare.ui.forms.VaccinationFormActivity;
import com.example.petcare.ui.forms.VetVisitFormActivity;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.HealthPdfExporter;
import com.example.petcare.util.ThemeUtils;

import java.util.List;

public class HealthFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentHealthSectionBinding binding;
    private PetRepository repository;
    private boolean showingReminders = true;

    private final ActivityResultLauncher<Intent> formLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reload());

    private final ActivityResultLauncher<String> pdfExportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"), uri -> {
                if (uri == null) return;
                try {
                    HealthPdfExporter.exportPetHealthRecordToUri(requireContext(), petId, uri);
                    Toast.makeText(requireContext(), "PDF exported", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Failed to export PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

    public static HealthFragment newInstance(long petId) {
        HealthFragment fragment = new HealthFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PET_ID, petId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHealthSectionBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);
        binding.buttonRemindersTab.setOnClickListener(v -> { showingReminders = true; reload(); });
        binding.buttonLogTab.setOnClickListener(v -> { showingReminders = false; reload(); });
        binding.sectionActionButton.setOnClickListener(this::showMenu);
        reload();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        updateTabs();
        binding.healthListContainer.removeAllViews();
        if (showingReminders) renderReminders();
        else renderLog();
    }

    private void updateTabs() {
        styleTab(binding.buttonRemindersTab, showingReminders);
        styleTab(binding.buttonLogTab, !showingReminders);
    }

    private void renderReminders() {
        List<Object> reminders = repository.getUpcomingReminderPreview(petId);
        binding.healthEmpty.setVisibility(reminders.isEmpty() ? View.VISIBLE : View.GONE);
        binding.healthEmpty.setText("No upcoming health reminders");
        for (Object item : reminders) binding.healthListContainer.addView(reminderCard(item));
    }

    private void renderLog() {
        List<Object> completed = repository.getHealthTimeline(petId);
        completed.sort((left, right) -> Long.compare(getItemTime(right), getItemTime(left)));
        binding.healthEmpty.setVisibility(completed.isEmpty() ? View.VISIBLE : View.GONE);
        binding.healthEmpty.setText("No completed health entries yet");
        for (Object item : completed) binding.healthListContainer.addView(logCard(item));
    }

    private View reminderCard(Object item) {
        LinearLayout row = baseCard(true);
        LinearLayout body = bodyColumn();
        TextView type = smallLabel(item instanceof Vaccination ? "Vaccination" : "Medication", true);
        TextView title = titleText(reminderTitle(item));
        TextView meta = metaText(reminderMeta(item));
        body.addView(type); body.addView(title); body.addView(meta);
        TextView chevron = chevron();
        CheckBox checkBox = new CheckBox(requireContext());
        checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(ThemeUtils.getAccentColor(requireContext())));
        checkBox.setOnClickListener(v -> completeReminder(item));
        row.addView(body, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(chevron);
        row.addView(checkBox);
        row.setOnClickListener(v -> openItem(item));
        return row;
    }

    private View logCard(Object item) {
        LinearLayout row = baseCard(false);
        LinearLayout body = bodyColumn();
        body.addView(smallLabel(titleFor(item), false));
        body.addView(titleText(subtitleFor(item)));
        String meta = metaFor(item);
        if (!meta.trim().isEmpty()) body.addView(metaText(meta));
        TextView chevron = chevron();
        CheckBox checkBox = new CheckBox(requireContext());
        checkBox.setChecked(true);
        checkBox.setEnabled(false);
        row.addView(body, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(chevron);
        row.addView(checkBox);
        row.setOnClickListener(v -> openItem(item));
        return row;
    }

    private LinearLayout baseCard(boolean upcoming) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(8), dp(10));
        row.setClickable(true);
        row.setFocusable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(requireContext(), R.color.pet_surface));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(upcoming ? 3 : 1), upcoming ? ThemeUtils.getAccentColor(requireContext()) : ContextCompat.getColor(requireContext(), R.color.pet_border));
        row.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(params);
        return row;
    }

    private LinearLayout bodyColumn() {
        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        return body;
    }

    private TextView smallLabel(String text, boolean accent) {
        TextView view = new TextView(requireContext());
        view.setText(text == null ? "" : text.toUpperCase(java.util.Locale.ROOT));
        view.setTextSize(10);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(accent ? ThemeUtils.getAccentColor(requireContext()) : ContextCompat.getColor(requireContext(), R.color.pet_text_secondary));
        return view;
    }

    private TextView titleText(String text) {
        TextView view = new TextView(requireContext());
        view.setText(text == null || text.trim().isEmpty() ? "—" : text.trim());
        view.setTextSize(15);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(ContextCompat.getColor(requireContext(), R.color.pet_text_primary));
        return view;
    }

    private TextView metaText(String text) {
        TextView view = new TextView(requireContext());
        view.setText(text == null ? "" : text.trim());
        view.setTextSize(13);
        view.setTextColor(ContextCompat.getColor(requireContext(), R.color.pet_text_secondary));
        return view;
    }

    private TextView chevron() {
        TextView view = new TextView(requireContext());
        view.setText("›");
        view.setTextSize(24);
        view.setTextColor(ContextCompat.getColor(requireContext(), R.color.pet_text_secondary));
        view.setPadding(dp(8), 0, dp(8), 0);
        return view;
    }

    private void completeReminder(Object item) {
        repository.completeReminder(item);
        Toast.makeText(requireContext(), "Reminder completed and added to the log", Toast.LENGTH_SHORT).show();
        reload();
    }

    private void openItem(Object item) {
        Intent intent;
        if (item instanceof VetVisit) {
            intent = new Intent(requireContext(), VetVisitFormActivity.class);
            intent.putExtra(VetVisitFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(VetVisitFormActivity.EXTRA_VISIT_ID, ((VetVisit) item).id);
        } else if (item instanceof Vaccination) {
            intent = new Intent(requireContext(), VaccinationFormActivity.class);
            intent.putExtra(VaccinationFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(VaccinationFormActivity.EXTRA_VACCINATION_ID, ((Vaccination) item).id);
        } else if (item instanceof Medication) {
            intent = new Intent(requireContext(), MedicationFormActivity.class);
            intent.putExtra(MedicationFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(MedicationFormActivity.EXTRA_MEDICATION_ID, ((Medication) item).id);
        } else if (item instanceof MedicationLog) {
            MedicationLog log = (MedicationLog) item;
            intent = new Intent(requireContext(), MedicationFormActivity.class);
            intent.putExtra(MedicationFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(MedicationFormActivity.EXTRA_MEDICATION_ID, log.medicationId);
        } else if (item instanceof SymptomEntry) {
            intent = new Intent(requireContext(), SymptomEntryFormActivity.class);
            intent.putExtra(SymptomEntryFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(SymptomEntryFormActivity.EXTRA_SYMPTOM_ID, ((SymptomEntry) item).id);
        } else if (item instanceof ReproductiveEvent) {
            intent = new Intent(requireContext(), ReproductiveEventFormActivity.class);
            intent.putExtra(ReproductiveEventFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(ReproductiveEventFormActivity.EXTRA_EVENT_ID, ((ReproductiveEvent) item).id);
        } else return;
        formLauncher.launch(intent);
    }

    private String titleFor(Object item) {
        if (item instanceof VetVisit) return "Vet visit";
        if (item instanceof Vaccination) return "Vaccination";
        if (item instanceof Medication) return "Medication record";
        if (item instanceof MedicationLog) return ((MedicationLog) item).missed ? "Medication missed" : "Medication completed";
        if (item instanceof SymptomEntry) return "Symptom";
        if (item instanceof ReproductiveEvent) return "Reproductive";
        return "Entry";
    }

    private String subtitleFor(Object item) {
        if (item instanceof VetVisit) {
            VetVisit visit = (VetVisit) item;
            return FormatUtils.joinNonEmpty(" · ", visit.reason, visit.clinicName);
        }
        if (item instanceof Vaccination) return ((Vaccination) item).vaccineName;
        if (item instanceof Medication) {
            Medication medication = (Medication) item;
            return FormatUtils.joinNonEmpty(" · ", medication.medicationName, FormatUtils.joinNonEmpty(" ", medication.dosage, medication.dosageUnit));
        }
        if (item instanceof MedicationLog) {
            MedicationLog log = (MedicationLog) item;
            Medication medication = repository.getDb().medicationDao().getById(log.medicationId);
            String name = medication == null ? "Medication" : medication.medicationName;
            String dose = medication == null ? "" : FormatUtils.joinNonEmpty(" ", medication.dosage, medication.dosageUnit);
            return FormatUtils.joinNonEmpty(" · ", name, dose);
        }
        if (item instanceof SymptomEntry) {
            SymptomEntry entry = (SymptomEntry) item;
            return FormatUtils.joinNonEmpty(" · ", entry.tagsCsv, entry.severity);
        }
        if (item instanceof ReproductiveEvent) return FormatUtils.joinNonEmpty(" · ", ((ReproductiveEvent) item).eventType, ((ReproductiveEvent) item).symptomsObserved);
        return "";
    }

    private String metaFor(Object item) {
        if (item instanceof VetVisit) return FormatUtils.humanDate(((VetVisit) item).visitDateEpochMillis);
        if (item instanceof Vaccination) return "Given: " + FormatUtils.humanDate(((Vaccination) item).administeredAt);
        if (item instanceof Medication) return "Started: " + FormatUtils.humanDate(((Medication) item).startDateEpochMillis);
        if (item instanceof MedicationLog) return "Completed: " + FormatUtils.humanDateTime(((MedicationLog) item).administeredAt);
        if (item instanceof SymptomEntry) return FormatUtils.humanDateTime(((SymptomEntry) item).recordedAt);
        if (item instanceof ReproductiveEvent) return FormatUtils.humanDate(((ReproductiveEvent) item).startDateEpochMillis);
        return "";
    }

    private String reminderTitle(Object item) {
        if (item instanceof Medication) return ((Medication) item).medicationName;
        if (item instanceof Vaccination) return ((Vaccination) item).vaccineName;
        return "Reminder";
    }

    private String reminderMeta(Object item) {
        long time = repository.reminderTime(item);
        String formatted = item instanceof Vaccination ? FormatUtils.humanDate(time) : FormatUtils.humanDateTime(time);
        if (FormatUtils.isNearMidnight(time)) formatted += "  ⚠ midnight time";
        return formatted;
    }

    private long getItemTime(Object item) {
        if (item instanceof VetVisit) return ((VetVisit) item).visitDateEpochMillis;
        if (item instanceof Vaccination) return ((Vaccination) item).administeredAt;
        if (item instanceof Medication) return ((Medication) item).startDateEpochMillis;
        if (item instanceof MedicationLog) return ((MedicationLog) item).administeredAt;
        if (item instanceof SymptomEntry) return ((SymptomEntry) item).recordedAt;
        if (item instanceof ReproductiveEvent) return ((ReproductiveEvent) item).startDateEpochMillis;
        return 0L;
    }

    private void showMenu(View anchor) {
        Pet pet = repository.getPet(petId);
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add("Add vet visit");
        menu.getMenu().add("Add vaccination");
        menu.getMenu().add("Add medication");
        menu.getMenu().add("Add symptom");
        if (pet != null && "female".equalsIgnoreCase(String.valueOf(pet.sex))) menu.getMenu().add("Add reproductive event");
        menu.getMenu().add("Export health PDF");
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            Intent intent = null;
            if ("Add vet visit".equals(title)) intent = new Intent(requireContext(), VetVisitFormActivity.class).putExtra(VetVisitFormActivity.EXTRA_PET_ID, petId);
            else if ("Add vaccination".equals(title)) intent = new Intent(requireContext(), VaccinationFormActivity.class).putExtra(VaccinationFormActivity.EXTRA_PET_ID, petId);
            else if ("Add medication".equals(title)) intent = new Intent(requireContext(), MedicationFormActivity.class).putExtra(MedicationFormActivity.EXTRA_PET_ID, petId);
            else if ("Add symptom".equals(title)) intent = new Intent(requireContext(), SymptomEntryFormActivity.class).putExtra(SymptomEntryFormActivity.EXTRA_PET_ID, petId);
            else if ("Add reproductive event".equals(title)) intent = new Intent(requireContext(), ReproductiveEventFormActivity.class).putExtra(ReproductiveEventFormActivity.EXTRA_PET_ID, petId);
            else if ("Export health PDF".equals(title)) { pdfExportLauncher.launch("pet_health_" + petId + ".pdf"); return true; }
            if (intent != null) formLauncher.launch(intent);
            return true;
        });
        menu.show();
    }

    private void styleTab(Button button, boolean active) {
        int accent = ThemeUtils.getAccentColor(requireContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(2), accent);
        bg.setColor(active ? accent : Color.TRANSPARENT);
        button.setBackground(bg);
        button.setTextColor(active ? ContextCompat.getColor(requireContext(), R.color.black) : accent);
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
