package com.example.petcare.ui.settings;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.databinding.ActivityExternalImportBinding;
import com.example.petcare.util.ExternalDataImporter;
import com.example.petcare.util.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExternalImportActivity extends AppCompatActivity {
    private ActivityExternalImportBinding binding;
    private PetRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Pet> pets = new ArrayList<>();
    private ExternalDataImporter.ParsedData parsedData;
    private ExternalDataImporter.ImportPreview preview;
    private Uri sourceUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityExternalImportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new PetRepository(this);

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.buttonImport.setOnClickListener(v -> confirmImport());

        sourceUri = getIntent().getData();
        String fileName = displayName(sourceUri);
        binding.textFileName.setText(fileName);
        setupPetSpinner();
        if (sourceUri == null) showError("No XLSX file was selected");
        else loadWorkbook(fileName);
    }

    private void setupPetSpinner() {
        pets.clear();
        pets.addAll(repository.getActivePets());
        List<String> labels = new ArrayList<>();
        if (pets.isEmpty()) labels.add("No pets available");
        else for (Pet pet : pets) labels.add(pet.name);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPet.setAdapter(adapter);
        binding.spinnerPet.setEnabled(!pets.isEmpty());

        long selectedPetId = repository.getSelectedPetId();
        for (int index = 0; index < pets.size(); index++) {
            if (pets.get(index).id == selectedPetId) {
                binding.spinnerPet.setSelection(index, false);
                break;
            }
        }
        binding.spinnerPet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parsedData != null) updatePreview();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadWorkbook(String fileName) {
        setLoading(true);
        executor.execute(() -> {
            try {
                ExternalDataImporter.ParsedData result = ExternalDataImporter.parse(
                        getApplicationContext(), sourceUri, fileName);
                runOnUiThread(() -> {
                    parsedData = result;
                    setLoading(false);
                    updatePreview();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(error.getMessage() == null ? "Could not read XLSX" : error.getMessage());
                });
            }
        });
    }

    private void updatePreview() {
        binding.textError.setVisibility(View.GONE);
        if (parsedData == null) return;
        if (pets.isEmpty()) {
            preview = null;
            binding.previewContent.setVisibility(View.VISIBLE);
            binding.textTotal.setText("Rows found: " + parsedData.totalRows);
            binding.textImportable.setText("Importable: unavailable");
            binding.textDuplicates.setText("Duplicates: unavailable");
            binding.textSkipped.setText("Skipped: " + parsedData.skippedRows);
            binding.textSkipReasons.setText(formatReasons(parsedData.skipReasons));
            binding.buttonImport.setEnabled(false);
            showError("Create a pet before importing external data");
            return;
        }

        Pet pet = pets.get(binding.spinnerPet.getSelectedItemPosition());
        preview = ExternalDataImporter.preview(repository.getDb(), parsedData, pet.id);
        binding.previewContent.setVisibility(View.VISIBLE);
        binding.textTotal.setText("Rows found: " + preview.totalRows);
        binding.textImportable.setText("Ready to import: " + preview.importableRows);
        binding.textDuplicates.setText("Duplicates: " + preview.duplicateRows);
        binding.textSkipped.setText("Skipped: " + preview.skippedRows);
        binding.textSkipReasons.setText(formatReasons(preview.skipReasons));
        binding.buttonImport.setEnabled(preview.importableRows > 0);
    }

    private void confirmImport() {
        if (parsedData == null || preview == null || pets.isEmpty() || preview.importableRows <= 0) return;
        Pet pet = pets.get(binding.spinnerPet.getSelectedItemPosition());
        new MaterialAlertDialogBuilder(this)
                .setTitle("Import external data")
                .setMessage("Import " + preview.importableRows + " rows into " + pet.name + "?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Import", (dialog, which) -> runImport(pet))
                .show();
    }

    private void runImport(Pet pet) {
        setLoading(true);
        binding.buttonImport.setEnabled(false);
        executor.execute(() -> {
            try {
                ExternalDataImporter.ImportResult result = ExternalDataImporter.importData(
                        repository.getDb(), parsedData, pet.id);
                runOnUiThread(() -> {
                    setLoading(false);
                    updatePreview();
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Import complete")
                            .setMessage("Imported: " + result.importedRows
                                    + "\nDuplicates: " + result.duplicateRows
                                    + "\nSkipped: " + result.skippedRows)
                            .setPositiveButton("OK", null)
                            .show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    updatePreview();
                    showError("Import failed and no rows were saved: "
                            + (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
                });
            }
        });
    }

    private String displayName(Uri uri) {
        if (uri == null) return "No file selected";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.trim().isEmpty()) return value.trim();
                }
            }
        } catch (Exception ignored) { }
        String last = uri.getLastPathSegment();
        return last == null || last.trim().isEmpty() ? "Selected workbook" : last;
    }

    private String formatReasons(Map<String, Integer> reasons) {
        if (reasons == null || reasons.isEmpty()) return "None";
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Integer> entry : reasons.entrySet()) {
            if (text.length() > 0) text.append('\n');
            text.append(entry.getValue()).append(" x ").append(entry.getKey());
        }
        return text.toString();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.spinnerPet.setEnabled(!loading && !pets.isEmpty());
        if (loading) binding.buttonImport.setEnabled(false);
    }

    private void showError(String message) {
        binding.textError.setText(message);
        binding.textError.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
