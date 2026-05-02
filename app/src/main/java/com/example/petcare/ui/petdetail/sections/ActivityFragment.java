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
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.databinding.FragmentActivityOnlySectionBinding;
import com.example.petcare.ui.common.FilterRange;
import com.example.petcare.ui.common.SimpleRowAdapter;
import com.example.petcare.ui.forms.ActivitySessionFormActivity;
import com.example.petcare.util.FormatUtils;

import java.util.List;

public class ActivityFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentActivityOnlySectionBinding binding;
    private PetRepository repository;
    private SimpleRowAdapter adapter;
    private FilterRange range = FilterRange.WEEK;

    private final ActivityResultLauncher<Intent> formLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reload());

    public static ActivityFragment newInstance(long petId) {
        ActivityFragment fragment = new ActivityFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PET_ID, petId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentActivityOnlySectionBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());
        petId = requireArguments().getLong(ARG_PET_ID);

        adapter = new SimpleRowAdapter(new SimpleRowAdapter.RowMapper<ActivitySession>() {
            @Override
            public String title(ActivitySession item) {
                String distance = item.distance == null || item.distanceUnit == null
                        ? ""
                        : " • " + FormatUtils.number(item.distance) + " " + item.distanceUnit;
                return item.activityType + " • " + item.durationMinutes + " min" + distance;
            }

            @Override
            public String subtitle(ActivitySession item) {
                return FormatUtils.nullable(item.notes);
            }

            @Override
            public String meta(ActivitySession item) {
                return FormatUtils.date(item.sessionDateEpochMillis);
            }
        });

        adapter.setOnRowClickListener(item -> {
            Intent intent = new Intent(requireContext(), ActivitySessionFormActivity.class);
            intent.putExtra(ActivitySessionFormActivity.EXTRA_PET_ID, petId);
            intent.putExtra(ActivitySessionFormActivity.EXTRA_ACTIVITY_ID, ((ActivitySession) item).id);
            formLauncher.launch(intent);
        });

        binding.sectionRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.sectionRecycler.setAdapter(adapter);

        binding.buttonAddActivity.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ActivitySessionFormActivity.class);
            intent.putExtra(ActivitySessionFormActivity.EXTRA_PET_ID, petId);
            formLauncher.launch(intent);
        });

        binding.buttonWeek.setOnClickListener(v -> setRange(FilterRange.WEEK));
        binding.buttonMonth.setOnClickListener(v -> setRange(FilterRange.MONTH));
        binding.buttonYear.setOnClickListener(v -> setRange(FilterRange.YEAR));

        reload();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void setRange(FilterRange newRange) {
        range = newRange;
        reload();
    }

    private void reload() {
        List<ActivitySession> items = repository.getActivitySessions(petId);
        adapter.submitList(items);
        binding.sectionEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.activityChart.setData(items, range);
        binding.activityDistanceChart.setData(items, range);
        binding.sectionSubtitle.setText("Time and distance charts • " + range.name());
    }
}
