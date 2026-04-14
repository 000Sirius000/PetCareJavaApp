package com.example.petcare.ui.pets;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.databinding.ActivityPetFormBinding;
import com.example.petcare.util.StorageUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PetFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";

    private ActivityPetFormBinding binding;
    private PetRepository repository;
    private Pet editingPet;
    private Uri pendingCameraUri;
    private String savedPhotoUri;

    private final ActivityResultLauncher<String[]> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    StorageUtils.persistReadPermission(this, uri);
                    savedPhotoUri = StorageUtils.copyImageToAppStorage(this, uri, "pet");
                    binding.imagePetPhoto.setImageURI(Uri.parse(savedPhotoUri));
                } catch (Exception e) {
                    toast("Photo import failed: " + e.getMessage());
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (!success || pendingCameraUri == null) return;
                try {
                    savedPhotoUri = StorageUtils.copyImageToAppStorage(this, pendingCameraUri, "pet_camera");
                    binding.imagePetPhoto.setImageURI(Uri.parse(savedPhotoUri));
                } catch (Exception e) {
                    toast("Camera photo failed: " + e.getMessage());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPetFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new PetRepository(this);
        setupSpinners();

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        long petId = getIntent().getLongExtra(EXTRA_PET_ID, 0L);
        if (petId > 0) {
            editingPet = repository.getPet(petId);
            if (editingPet != null) {
                populate();
            }
        }

        binding.buttonPickPhoto.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        binding.buttonTakePhoto.setOnClickListener(v -> capturePhoto());
        binding.buttonRemovePhoto.setOnClickListener(v -> {
            savedPhotoUri = null;
            binding.imagePetPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
        });
        binding.buttonSavePet.setOnClickListener(v -> savePet());
        binding.buttonArchiveRecover.setOnClickListener(v -> handleArchiveRecover());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> speciesAdapter = ArrayAdapter.createFromResource(
                this, R.array.species_options, android.R.layout.simple_spinner_item);
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputSpecies.setAdapter(speciesAdapter);

        ArrayAdapter<CharSequence> sexAdapter = ArrayAdapter.createFromResource(
                this, R.array.sex_options, android.R.layout.simple_spinner_item);
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputSex.setAdapter(sexAdapter);
    }

    private void populate() {
        binding.toolbar.setTitle("Edit pet");
        binding.inputName.setText(editingPet.name);
        binding.inputBreed.setText(editingPet.breed);
        binding.inputAge.setText(editingPet.birthInfo);
        binding.inputGoalMinutes.setText(String.valueOf(editingPet.weeklyActivityGoalMinutes));
        selectSpinnerValue(binding.inputSpecies, editingPet.species);
        selectSpinnerValue(binding.inputSex, editingPet.sex);

        savedPhotoUri = editingPet.photoUri;
        if (savedPhotoUri != null && !savedPhotoUri.isEmpty()) {
            binding.imagePetPhoto.setImageURI(Uri.parse(savedPhotoUri));
        }

        binding.buttonArchiveRecover.setVisibility(android.view.View.VISIBLE);
        binding.buttonDelete.setVisibility(android.view.View.VISIBLE);
        binding.buttonArchiveRecover.setText(editingPet.archived ? R.string.recover : R.string.archive);
    }

    private void capturePhoto() {
        try {
            pendingCameraUri = StorageUtils.createCameraImageUri(this, "pet_camera");
            cameraLauncher.launch(pendingCameraUri);
        } catch (Exception e) {
            toast("Could not launch camera: " + e.getMessage());
        }
    }

    private void savePet() {
        String name = safe(binding.inputName.getText() == null ? null : binding.inputName.getText().toString());
        if (name.isEmpty()) {
            binding.layoutName.setError("Please enter the pet name");
            return;
        }
        binding.layoutName.setError(null);

        Pet pet = editingPet == null ? new Pet() : editingPet;
        pet.name = name;
        pet.species = binding.inputSpecies.getSelectedItem().toString();
        pet.breed = safe(binding.inputBreed.getText() == null ? null : binding.inputBreed.getText().toString());
        pet.birthInfo = safe(binding.inputAge.getText() == null ? null : binding.inputAge.getText().toString());
        pet.sex = binding.inputSex.getSelectedItem().toString();
        pet.photoUri = savedPhotoUri;
        pet.weeklyActivityGoalMinutes = parseInt(binding.inputGoalMinutes.getText() == null ? null : binding.inputGoalMinutes.getText().toString(), 180);
        repository.savePet(pet);
        setResult(RESULT_OK, new Intent());
        toast("Pet saved");
        finish();
    }

    private void handleArchiveRecover() {
        if (editingPet == null) return;
        if (editingPet.archived) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.confirm_recover_pet)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.recover, (dialog, which) -> {
                        repository.recoverPet(editingPet.id);
                        setResult(RESULT_OK);
                        finish();
                    })
                    .show();
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.confirm_archive_pet)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.archive, (dialog, which) -> {
                        repository.archivePet(editingPet.id);
                        setResult(RESULT_OK);
                        finish();
                    })
                    .show();
        }
    }

    private void confirmDelete() {
        if (editingPet == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.confirm_delete)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.deletePet(editingPet);
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    private void selectSpinnerValue(android.widget.Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equals(String.valueOf(spinner.getItemAtPosition(i)))) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(safe(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
