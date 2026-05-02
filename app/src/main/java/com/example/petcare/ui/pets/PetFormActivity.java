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
import com.example.petcare.ui.common.FormUiUtils;
import com.example.petcare.util.AgeUtils;
import com.example.petcare.util.StorageUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

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

        binding.inputAge.setKeyListener(null);
        binding.inputAge.setFocusable(false);
        binding.inputAge.setClickable(true);
        binding.inputAge.setOnClickListener(v -> openBirthDatePicker());

        long petId = getIntent().getLongExtra(EXTRA_PET_ID, 0L);
        if (petId > 0) {
            editingPet = repository.getPet(petId);
            if (editingPet != null) populate();
        } else {
            binding.textAgePreview.setText("Age will be calculated automatically");
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
        ArrayAdapter<CharSequence> speciesAdapter =
                ArrayAdapter.createFromResource(this, R.array.species_options, android.R.layout.simple_spinner_item);
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputSpecies.setAdapter(speciesAdapter);

        ArrayAdapter<CharSequence> sexAdapter =
                ArrayAdapter.createFromResource(this, R.array.sex_options, android.R.layout.simple_spinner_item);
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputSex.setAdapter(sexAdapter);
    }

    private void populate() {
        binding.toolbar.setTitle("Edit pet");
        binding.inputName.setText(editingPet.name);
        binding.inputBreed.setText(editingPet.breed);
        binding.inputAge.setText(editingPet.birthInfo);
        updateAgePreview(editingPet.birthInfo);
        binding.inputGoalMinutes.setText(String.valueOf(editingPet.weeklyActivityGoalMinutes));
        if (editingPet.minHealthyWeight != null) {
            binding.inputMinHealthyWeight.setText(String.valueOf(editingPet.minHealthyWeight));
        }
        if (editingPet.maxHealthyWeight != null) {
            binding.inputMaxHealthyWeight.setText(String.valueOf(editingPet.maxHealthyWeight));
        }
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

    private void openBirthDatePicker() {
        long initial = System.currentTimeMillis();
        LocalDate parsed = editingPet == null ? null : AgeUtils.parseBirthDate(editingPet);
        if (parsed != null) {
            initial = parsed.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        FormUiUtils.showDatePicker(this, initial, binding.inputAge, () -> {
            String value = binding.inputAge.getText() == null ? "" : binding.inputAge.getText().toString().trim();
            updateAgePreview(value);
        });
    }

    private void updateAgePreview(String birthInfo) {
        if (birthInfo == null || birthInfo.trim().isEmpty()) {
            binding.textAgePreview.setText("Age will be calculated automatically");
            return;
        }
        try {
            LocalDate date = LocalDate.parse(birthInfo.trim());
            binding.textAgePreview.setText("Age: " + AgeUtils.fullAge(date));
        } catch (Exception e) {
            binding.textAgePreview.setText("Use the date picker to choose a valid date");
        }
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

        String birthInfo = safe(binding.inputAge.getText() == null ? null : binding.inputAge.getText().toString());
        if (!birthInfo.isEmpty()) {
            try {
                LocalDate.parse(birthInfo);
            } catch (Exception e) {
                toast("Please choose a valid date of birth");
                return;
            }
        }

        Double minWeight = parseOptionalDouble(binding.inputMinHealthyWeight.getText() == null ? null : binding.inputMinHealthyWeight.getText().toString());
        Double maxWeight = parseOptionalDouble(binding.inputMaxHealthyWeight.getText() == null ? null : binding.inputMaxHealthyWeight.getText().toString());
        if (minWeight != null && maxWeight != null && minWeight > maxWeight) {
            toast("Minimum healthy weight cannot be greater than maximum healthy weight");
            return;
        }

        Pet pet = editingPet == null ? new Pet() : editingPet;
        pet.name = name;
        pet.species = binding.inputSpecies.getSelectedItem().toString();
        pet.breed = safe(binding.inputBreed.getText() == null ? null : binding.inputBreed.getText().toString());
        pet.birthInfo = birthInfo;
        pet.sex = binding.inputSex.getSelectedItem().toString();
        pet.photoUri = savedPhotoUri;
        pet.weeklyActivityGoalMinutes = parseInt(binding.inputGoalMinutes.getText() == null ? null : binding.inputGoalMinutes.getText().toString(), 45);
        pet.minHealthyWeight = minWeight;
        pet.maxHealthyWeight = maxWeight;

        long petId = repository.savePet(pet);
        repository.setSelectedPetId(petId);

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
                    }).show();
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.confirm_archive_pet)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.archive, (dialog, which) -> {
                        repository.archivePet(editingPet.id);
                        setResult(RESULT_OK);
                        finish();
                    }).show();
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
                }).show();
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
        try { return Integer.parseInt(safe(value)); }
        catch (Exception e) { return fallback; }
    }

    private Double parseOptionalDouble(String value) {
        String clean = safe(value).replace(',', '.');
        if (clean.isEmpty()) return null;
        try { return Double.parseDouble(clean); }
        catch (Exception e) {
            toast("Please enter weight as a number");
            throw e;
        }
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }

    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }
}
