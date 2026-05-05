package com.example.petcare.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.databinding.ActivitySettingsBinding;
import com.example.petcare.util.JsonBackupManager;
import com.example.petcare.util.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS = "petcare_prefs";

    // Save all known keys so the rest of the app can keep using the key it already expects.
    private static final String KEY_PET_ICON = "pet_icon";
    private static final String KEY_PET_ICON_CHOICE = "pet_icon_choice";
    private static final String KEY_PETS_ICON = "pets_icon";
    private static final String KEY_PETS_ICON_PREFERENCE = "pets_icon_preference";

    private ActivitySettingsBinding binding;
    private boolean listenersReady = false;
    private String currentTheme;
    private String currentPetIcon;

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
        ThemeUtils.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        binding.inputGracePeriod.setText(String.valueOf(prefs.getInt("grace_hours", 2)));
        binding.inputVaxLead.setText(String.valueOf(prefs.getInt("vax_days", 30)));

        setupThemeSpinner(prefs);
        setupPetIconSpinner(prefs);

        binding.buttonExportJson.setOnClickListener(v -> exportLauncher.launch("petcare_backup.json"));
        binding.buttonImportJson.setOnClickListener(v -> confirmImport());

        binding.inputGracePeriod.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) savePrefs();
        });
        binding.inputVaxLead.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) savePrefs();
        });

        // Important: only after all setSelection(...) calls.
        // Android Spinner fires onItemSelected during initialization; without this guard
        // the page can spam "Pets icon updated" and feel unclickable.
        listenersReady = true;
    }

    private void setupThemeSpinner(SharedPreferences prefs) {
        ArrayAdapter<CharSequence> themeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.theme_options,
                android.R.layout.simple_spinner_item
        );
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTheme.setAdapter(themeAdapter);

        currentTheme = normalizeThemeValue(ThemeUtils.getSavedTheme(this));
        binding.spinnerTheme.setSelection(findThemePosition(themeAdapter, currentTheme), false);

        binding.spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (!listenersReady) return;

                String selected = normalizeThemeLabel(String.valueOf(parent.getItemAtPosition(position)));
                if (selected.equals(currentTheme)) return;

                currentTheme = selected;
                savePrefs();
                ThemeUtils.saveAndApplyTheme(SettingsActivity.this, selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupPetIconSpinner(SharedPreferences prefs) {
        ArrayAdapter<CharSequence> iconAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.pet_icon_options,
                android.R.layout.simple_spinner_item
        );
        iconAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPetIcon.setAdapter(iconAdapter);

        currentPetIcon = normalizePetIconValue(readPetIconPreference(prefs));
        binding.spinnerPetIcon.setSelection(findPetIconPosition(iconAdapter, currentPetIcon), false);

        binding.spinnerPetIcon.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (!listenersReady) return;

                String selected = normalizePetIconLabel(String.valueOf(parent.getItemAtPosition(position)));
                if (selected.equals(currentPetIcon)) return;

                currentPetIcon = selected;
                savePetIconPreference(selected);
                toast("Pets icon updated");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private String readPetIconPreference(SharedPreferences prefs) {
        String value = prefs.getString(KEY_PET_ICON, null);
        if (value == null) value = prefs.getString(KEY_PET_ICON_CHOICE, null);
        if (value == null) value = prefs.getString(KEY_PETS_ICON, null);
        if (value == null) value = prefs.getString(KEY_PETS_ICON_PREFERENCE, null);
        return value == null ? "dog_cat" : value;
    }

    private void savePetIconPreference(String value) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_PET_ICON, value)
                .putString(KEY_PET_ICON_CHOICE, value)
                .putString(KEY_PETS_ICON, value)
                .putString(KEY_PETS_ICON_PREFERENCE, value)
                .apply();
    }

    private void savePrefs() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();

        Integer grace = parsePositiveInt(binding.inputGracePeriod.getText() == null
                ? null
                : binding.inputGracePeriod.getText().toString());
        Integer vax = parsePositiveInt(binding.inputVaxLead.getText() == null
                ? null
                : binding.inputVaxLead.getText().toString());

        if (grace != null) editor.putInt("grace_hours", grace);
        if (vax != null) editor.putInt("vax_days", vax);

        editor.apply();
    }

    private Integer parsePositiveInt(String value) {
        if (value == null) return null;
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(0, parsed);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int findThemePosition(ArrayAdapter<CharSequence> adapter, String savedValue) {
        String normalized = normalizeThemeValue(savedValue);
        for (int i = 0; i < adapter.getCount(); i++) {
            if (normalized.equals(normalizeThemeLabel(String.valueOf(adapter.getItem(i))))) {
                return i;
            }
        }
        return 0;
    }

    private int findPetIconPosition(ArrayAdapter<CharSequence> adapter, String savedValue) {
        String normalized = normalizePetIconValue(savedValue);
        for (int i = 0; i < adapter.getCount(); i++) {
            if (normalized.equals(normalizePetIconLabel(String.valueOf(adapter.getItem(i))))) {
                return i;
            }
        }
        return 0;
    }

    private String normalizeThemeLabel(String label) {
        String value = label == null ? "" : label.trim().toLowerCase(Locale.ROOT);
        if (value.contains("purple")) return "black_purple";
        if (value.contains("yellow")) return "black_yellow";
        if (value.contains("light")) return "light";
        if (value.contains("dark")) return "dark";
        return "system";
    }

    private String normalizeThemeValue(String value) {
        if (value == null) return "system";
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "black-purple":
            case "black purple":
            case "purple":
            case "black_purple":
                return "black_purple";
            case "black-yellow":
            case "black yellow":
            case "yellow":
            case "black_yellow":
                return "black_yellow";
            case "light":
                return "light";
            case "dark":
                return "dark";
            default:
                return "system";
        }
    }

    private String normalizePetIconLabel(String label) {
        String value = label == null ? "" : label.trim().toLowerCase(Locale.ROOT);
        boolean hasDog = value.contains("dog");
        boolean hasCat = value.contains("cat");
        if (hasDog && hasCat) return "dog_cat";
        if (hasDog) return "dog";
        if (hasCat) return "cat";
        return "dog_cat";
    }

    private String normalizePetIconValue(String value) {
        if (value == null) return "dog_cat";
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "dog":
            case "only_dog":
            case "dog_only":
                return "dog";
            case "cat":
            case "only_cat":
            case "cat_only":
                return "cat";
            default:
                return "dog_cat";
        }
    }

    private void confirmImport() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.confirm_import_backup)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Import", (dialog, which) -> importLauncher.launch(new String[]{"application/json", "*/*"}))
                .show();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }
}
