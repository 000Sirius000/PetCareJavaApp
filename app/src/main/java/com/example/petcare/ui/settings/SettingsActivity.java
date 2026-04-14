package com.example.petcare.ui.settings;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.databinding.ActivitySettingsBinding;
import com.example.petcare.util.JsonBackupManager;
import com.example.petcare.util.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS = "petcare_prefs";
    private ActivitySettingsBinding binding;

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri == null) return;
                try {
                    savePrefs();
                    JsonBackupManager.exportAllToUri(this, uri);
                    toast("Backup exported");
                } catch (Exception e) {
                    toast("Export failed: " + e.getMessage());
                }
            });

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    int imported = JsonBackupManager.importFromUri(this, uri);
                    toast("Imported pets: " + imported);
                } catch (Exception e) {
                    toast("Import failed: " + e.getMessage());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        binding.inputGracePeriod.setText(String.valueOf(prefs.getInt("grace_hours", 2)));
        binding.inputVaxLead.setText(String.valueOf(prefs.getInt("vax_days", 30)));

        ArrayAdapter<CharSequence> themeAdapter = ArrayAdapter.createFromResource(this, com.example.petcare.R.array.theme_options, android.R.layout.simple_spinner_item);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTheme.setAdapter(themeAdapter);
        String mode = ThemeUtils.getSavedTheme(this);
        binding.spinnerTheme.setSelection("light".equals(mode) ? 1 : "dark".equals(mode) ? 2 : 0);

        binding.buttonExportJson.setOnClickListener(v -> exportLauncher.launch("petcare_backup.json"));
        binding.buttonImportJson.setOnClickListener(v -> confirmImport());

        binding.inputGracePeriod.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) savePrefs();
        });
        binding.inputVaxLead.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) savePrefs();
        });
        binding.spinnerTheme.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selected = position == 1 ? "light" : position == 2 ? "dark" : "system";
                ThemeUtils.saveAndApplyTheme(SettingsActivity.this, selected);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    private void savePrefs() {
        try {
            int grace = Integer.parseInt(binding.inputGracePeriod.getText().toString());
            int vax = Integer.parseInt(binding.inputVaxLead.getText().toString());
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putInt("grace_hours", grace)
                    .putInt("vax_days", vax)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private void confirmImport() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(com.example.petcare.R.string.warning)
                .setMessage(com.example.petcare.R.string.confirm_import_backup)
                .setNegativeButton(com.example.petcare.R.string.cancel, null)
                .setPositiveButton("Import", (dialog, which) -> importLauncher.launch(new String[]{"application/json", "*/*"}))
                .show();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }
}
