package com.example.petcare.ui.petdetail.sections;

import android.content.Intent;
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
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.databinding.FragmentFeedingSectionBinding;
import com.example.petcare.ui.common.FilterRange;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.FeedingScheduleFormActivity;

import java.util.List;
import java.util.Locale;

public class FeedingFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";
    private long petId;
    private FragmentFeedingSectionBinding binding;
    private PetRepository repository;
    private SimpleRowAdapter adapter;
    private FilterRange range = FilterRange.WEEK;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFeedingSectionBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        adapter = new SimpleRowAdapter(new SimpleRowAdapter.RowMapper<FeedingSchedule>() {
            @Override
            public String title(FeedingSchedule item) { return item.mealName; }
            @Override
            public String subtitle(FeedingSchedule item) {
                return String.format(Locale.getDefault(), "%02d:%02d • %s", item.hourOfDay, item.minute, item.foodType);
            }
            @Override
            public String meta(FeedingSchedule item) { return "Portion: " + item.portion + " " + item.portionUnit; }
        });
        adapter.setOnRowClickListener(item -> {
            Intent intent = new Intent(requireContext(), FeedingScheduleFormActivity.class);
            intent.putExtra(FeedingScheduleFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(FeedingScheduleFormActivity.EXTRA_SCHEDULE_ID, ((FeedingSchedule) item).id);
            formLauncher.launch(intent);
        });

        binding.sectionRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.sectionRecycler.setAdapter(adapter);
        binding.buttonAddFeeding.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FeedingScheduleFormActivity.class);
            intent.putExtra(FeedingScheduleFormActivity.EXTRA_PET_ID, petId);
            formLauncher.launch(intent);
        });
        binding.buttonWeek.setOnClickListener(v -> setRange(FilterRange.WEEK));
        binding.buttonMonth.setOnClickListener(v -> setRange(FilterRange.MONTH));
        binding.buttonYear.setOnClickListener(v -> setRange(FilterRange.YEAR));
        reload();
        return binding.getRoot();
    }

    private void setRange(FilterRange newRange) {
        range = newRange;
        reload();
    }

    private void reload() {
        List<FeedingSchedule> items = repository.getFeedingSchedules(petId);
        adapter.submitList(items);
        binding.sectionEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.feedingChart.setData(repository.getFeedingLogs(petId), range);
        binding.sectionSubtitle.setText("Schedules and meal proportions • " + range.name());
    }
}
