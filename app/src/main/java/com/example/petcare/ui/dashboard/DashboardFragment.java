package com.example.petcare.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.databinding.FragmentDashboardBinding;
import com.example.petcare.ui.petdetail.PetDetailActivity;
import com.example.petcare.ui.pets.PetAdapter;
import com.example.petcare.ui.pets.PetFormActivity;

import java.util.List;

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private PetAdapter adapter;
    private PetRepository repository;

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());

        adapter = new PetAdapter(
                pet -> {
                    Intent intent = new Intent(requireContext(), PetDetailActivity.class);
                    intent.putExtra(PetDetailActivity.EXTRA_PET_ID, pet.id);
                    startActivity(intent);
                },
                pet -> {
                    Intent intent = new Intent(requireContext(), PetFormActivity.class);
                    intent.putExtra(PetFormActivity.EXTRA_PET_ID, pet.id);
                    startActivity(intent);
                },
                repository
        );

        binding.dashboardPetList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.dashboardPetList.setAdapter(adapter);
        binding.dashboardSwipe.setOnRefreshListener(this::reload);
        reload();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<Pet> pets = repository.getActivePets();
        adapter.submitList(pets);
        binding.dashboardSwipe.setRefreshing(false);
    }
}
