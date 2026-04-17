package com.example.petcare.ui.petdetail.sections;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.FeedingLog;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.data.entities.MedicationLog;
import com.example.petcare.databinding.FragmentRemindersSectionBinding;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.FeedingScheduleFormActivity;
import com.example.petcare.ui.forms.MedicationFormActivity;
import com.example.petcare.util.FormatUtils;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class FeedingFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";
    private static final int TAB_FEEDING = 0;
    private static final int TAB_MEDICATIONS = 1;

    private long petId;
    private int selectedTab = TAB_FEEDING;
    private FragmentRemindersSectionBinding binding;
    private PetRepository repository;
    private SimpleRowAdapter adapter;

    private final ActivityResultLauncher<Intent> formLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reload());

    public static FeedingFragment newInstance(long petId) {
        FeedingFragment fragment = new FeedingFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PET_ID, petId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRemindersSectionBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        adapter = new SimpleRowAdapter(new SimpleRowAdapter.RowMapper<Object>() {
            @Override
            public String title(Object item) {
                if (item instanceof FeedingSchedule) {
                    return "Schedule • " + ((FeedingSchedule) item).mealName;
                }
                if (item instanceof FeedingLog) {
                    return "Fed • " + ((FeedingLog) item).mealName;
                }
                if (item instanceof Medication) {
                    return "Medication • " + ((Medication) item).medicationName;
                }
                if (item instanceof MedicationLog) {
                    return (((MedicationLog) item).missed ? "Missed dose" : "Dose given");
                }
                return "";
            }

            @Override
            public String subtitle(Object item) {
                if (item instanceof FeedingSchedule) {
                    FeedingSchedule s = (FeedingSchedule) item;
                    return String.format(java.util.Locale.getDefault(), "%02d:%02d • %s • %s %s",
                            s.hourOfDay, s.minute, s.foodType, s.portion, s.portionUnit);
                }
                if (item instanceof FeedingLog) {
                    FeedingLog log = (FeedingLog) item;
                    return log.portion;
                }
                if (item instanceof Medication) {
                    Medication m = (Medication) item;
                    return m.dosage + " " + m.dosageUnit + " • " + m.frequencyType;
                }
                if (item instanceof MedicationLog) {
                    MedicationLog log = (MedicationLog) item;
                    return "Marked by " + FormatUtils.nullable(log.markedBy);
                }
                return "";
            }

            @Override
            public String meta(Object item) {
                if (item instanceof FeedingSchedule) {
                    return "Tap to edit the schedule";
                }
                if (item instanceof FeedingLog) {
                    return "Completed at " + FormatUtils.dateTime(((FeedingLog) item).completedAt);
                }
                if (item instanceof Medication) {
                    Medication m = (Medication) item;
                    SharedPreferences prefs = requireContext().getSharedPreferences("petcare_prefs", 0);
                    int graceHours = prefs.getInt("grace_hours", 2);
                    long missedThreshold = System.currentTimeMillis() - graceHours * 60L * 60 * 1000;
                    String warning = m.nextReminderAt < missedThreshold ? " • MISSED" : "";
                    return "Next dose: " + FormatUtils.dateTime(m.nextReminderAt) + warning;
                }
                if (item instanceof MedicationLog) {
                    MedicationLog log = (MedicationLog) item;
                    return FormatUtils.dateTime(log.administeredAt);
                }
                return "";
            }
        });

        adapter.setOnRowClickListener(item -> {
            if (item instanceof FeedingSchedule) {
                Intent intent = new Intent(requireContext(), FeedingScheduleFormActivity.class);
                intent.putExtra(FeedingScheduleFormActivity.EXTRA_PET_ID, petId);
                intent.putExtra(FeedingScheduleFormActivity.EXTRA_SCHEDULE_ID, ((FeedingSchedule) item).id);
                formLauncher.launch(intent);
            } else if (item instanceof Medication) {
                Intent intent = new Intent(requireContext(), MedicationFormActivity.class);
                intent.putExtra(MedicationFormActivity.EXTRA_PET_ID, petId);
                intent.putExtra(MedicationFormActivity.EXTRA_MEDICATION_ID, ((Medication) item).id);
                formLauncher.launch(intent);
            }
        });

        binding.sectionRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.sectionRecycler.setAdapter(adapter);

        binding.sectionTabs.addTab(binding.sectionTabs.newTab().setText("Feeding"));
        binding.sectionTabs.addTab(binding.sectionTabs.newTab().setText("Medications"));
        binding.sectionTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                reload();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                reload();
            }
        });

        binding.sectionActionButton.setOnClickListener(v -> openCreateForm());
        reload();
        return binding.getRoot();
    }

    private void openCreateForm() {
        Intent intent;
        if (selectedTab == TAB_FEEDING) {
            intent = new Intent(requireContext(), FeedingScheduleFormActivity.class);
            intent.putExtra(FeedingScheduleFormActivity.EXTRA_PET_ID, petId);
        } else {
            intent = new Intent(requireContext(), MedicationFormActivity.class);
            intent.putExtra(MedicationFormActivity.EXTRA_PET_ID, petId);
        }
        formLauncher.launch(intent);
    }

    private void reload() {
        if (selectedTab == TAB_FEEDING) {
            binding.sectionTitle.setText("Feeding");
            binding.sectionSubtitle.setText("Schedules and completed feeding log");
            binding.sectionActionButton.setText("Add feeding schedule");

            List<Object> items = new ArrayList<>();
            items.addAll(repository.getFeedingSchedules(petId));
            items.addAll(repository.getFeedingLogs(petId));
            adapter.submitList(items);
            binding.sectionEmpty.setText("No feeding schedules or logs yet");
            binding.sectionEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            binding.sectionTitle.setText("Medications");
            binding.sectionSubtitle.setText("Medication plans and dose log");
            binding.sectionActionButton.setText("Add medication");

            List<Object> items = new ArrayList<>();
            items.addAll(repository.getMedications(petId));
            items.addAll(repository.getMedicationLogs(petId));
            adapter.submitList(items);
            binding.sectionEmpty.setText("No medication entries or logs yet");
            binding.sectionEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}
