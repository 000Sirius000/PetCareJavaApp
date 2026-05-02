package com.example.petcare.ui.pets;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.databinding.FragmentPetsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class PetsFragment extends Fragment {
    private FragmentPetsBinding binding;
    private PetAdapter adapter;
    private PetRepository repository;

    private final ActivityResultLauncher<Intent> petFormLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reload());

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        binding = FragmentPetsBinding.inflate(inflater, container, false);
        repository = new PetRepository(requireContext());

        adapter = new PetAdapter(
                pet -> reload(),
                this::editPet,
                repository,
                PetAdapter.Mode.PETS
        );

        binding.petsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.petsRecycler.setAdapter(adapter);
        binding.petsSwipe.setOnRefreshListener(this::reload);

        binding.addPetFab.setOnClickListener(v ->
                petFormLauncher.launch(new Intent(requireContext(), PetFormActivity.class)));

        binding.petsToolbar.getMenu().clear();
        binding.petsToolbar.getMenu().add("Archived pets");
        binding.petsToolbar.setOnMenuItemClickListener(item -> {
            showArchivedPets();
            return true;
        });

        reload();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void editPet(Pet pet) {
        Intent intent = new Intent(requireContext(), PetFormActivity.class);
        intent.putExtra(PetFormActivity.EXTRA_PET_ID, pet.id);
        petFormLauncher.launch(intent);
    }

    private void showArchivedPets() {
        List<Pet> archived = repository.getArchivedPets();
        if (archived.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Archived pets")
                    .setMessage("No archived pets")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        List<String> names = new ArrayList<>();
        for (Pet pet : archived) names.add(pet.name + " • " + pet.species);

        ArrayAdapter<String> archivedAdapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, names);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Archived pets")
                .setAdapter(archivedAdapter, (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), PetFormActivity.class);
                    intent.putExtra(PetFormActivity.EXTRA_PET_ID, archived.get(which).id);
                    petFormLauncher.launch(intent);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void reload() {
        List<Pet> pets = repository.getActivePets();
        repository.getSelectedPetId();
        adapter.submitList(pets);
        binding.petsEmpty.setVisibility(pets.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.petsSwipe.setRefreshing(false);
    }
}
