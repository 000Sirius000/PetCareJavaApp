package com.example.petcare.ui.petdetail.sections;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.data.entities.Medication;
import com.example.petcare.databinding.FragmentSimpleListBinding;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.FeedingScheduleFormActivity;
import com.example.petcare.ui.forms.MedicationFormActivity;
import com.example.petcare.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class FeedingFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentSimpleListBinding binding;
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
        binding = FragmentSimpleListBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        binding.sectionTitle.setText("Feeding & medication");
        binding.sectionSubtitle.setText("Schedules, active medication and reminder overview");
        binding.sectionActionButton.setText("Add feeding / medication");

        adapter = new SimpleRowAdapter(new SimpleRowAdapter.RowMapper<Object>() {
            @Override
            public String title(Object item) {
                if (item instanceof FeedingSchedule) {
                    return "Meal: " + ((FeedingSchedule) item).mealName;
                }
                if (item instanceof Medication) {
                    return "Medication: " + ((Medication) item).medicationName;
                }
                return "";
            }

            @Override
            public String subtitle(Object item) {
                if (item instanceof FeedingSchedule) {
                    FeedingSchedule s = (FeedingSchedule) item;
                    return String.format(java.util.Locale.getDefault(), "%02d:%02d • %s", s.hourOfDay, s.minute, s.foodType);
                }
                if (item instanceof Medication) {
                    Medication m = (Medication) item;
                    return m.dosage + " " + m.dosageUnit + " • " + m.frequencyType;
                }
                return "";
            }

            @Override
            public String meta(Object item) {
                if (item instanceof FeedingSchedule) {
                    FeedingSchedule s = (FeedingSchedule) item;
                    return "Portion: " + s.portion + " " + s.portionUnit;
                }
                if (item instanceof Medication) {
                    Medication m = (Medication) item;
                    SharedPreferences prefs = requireContext().getSharedPreferences("petcare_prefs", 0);
                    int graceHours = prefs.getInt("grace_hours", 2);
                    long missedThreshold = System.currentTimeMillis() - graceHours * 60L * 60 * 1000;
                    String warning = m.nextReminderAt < missedThreshold ? " • MISSED" : "";
                    return "Next dose: " + FormatUtils.dateTime(m.nextReminderAt) + warning;
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
        binding.sectionActionButton.setOnClickListener(this::showMenu);
        reload();
        return binding.getRoot();
    }

    private void reload() {
        List<Object> items = new ArrayList<>();
        items.addAll(repository.getFeedingSchedules(petId));
        items.addAll(repository.getMedications(petId));
        adapter.submitList(items);
        binding.sectionEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add("Add feeding schedule");
        menu.getMenu().add("Add medication");
        menu.setOnMenuItemClickListener(item -> {
            if ("Add feeding schedule".equals(String.valueOf(item.getTitle()))) {
                Intent intent = new Intent(requireContext(), FeedingScheduleFormActivity.class);
                intent.putExtra(FeedingScheduleFormActivity.EXTRA_PET_ID, petId);
                formLauncher.launch(intent);
            } else {
                Intent intent = new Intent(requireContext(), MedicationFormActivity.class);
                intent.putExtra(MedicationFormActivity.EXTRA_PET_ID, petId);
                formLauncher.launch(intent);
            }
            return true;
        });
        menu.show();
    }
}
