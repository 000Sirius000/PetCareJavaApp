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

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.databinding.FragmentActivityOnlySectionBinding;
import com.example.petcare.ui.common.FilterRange;
import com.example.petcare.ui.forms.ActivitySessionFormActivity;

import java.util.List;

public class ActivityFragment extends Fragment {
    private static final String ARG_PET_ID = "pet_id";

    private long petId;
    private FragmentActivityOnlySectionBinding binding;
    private PetRepository repository;
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

        binding.buttonAddActivity.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ActivitySessionFormActivity.class);
            intent.putExtra(ActivitySessionFormActivity.EXTRA_PET_ID, petId);
            formLauncher.launch(intent);
        });
        binding.buttonOpenActivityLog.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ActivityLogActivity.class);
            intent.putExtra(ActivityLogActivity.EXTRA_PET_ID, petId);
            startActivity(intent);
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
        binding.activityChart.setData(items, range);
        binding.activityDistanceChart.setData(items, range);
        binding.sectionSubtitle.setText("Time and distance charts • " + range.name());
    }
}
