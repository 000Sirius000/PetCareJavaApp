package com.example.petcare.ui.forms;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.VetVisit;
import com.example.petcare.databinding.ActivityVetVisitFormBinding;
import com.example.petcare.ui.common.FormUiUtils;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.StorageUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class VetVisitFormActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_VISIT_ID = "extra_visit_id";

    private ActivityVetVisitFormBinding binding;
    private PetRepository repository;
    private VetVisit editingVisit;
    private String savedAttachmentUri;

    private final ActivityResultLauncher<String[]> attachmentPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    StorageUtils.persistReadPermission(this, uri);
                    savedAttachmentUri = StorageUtils.copyDocumentToAppStorage(this, uri, "attachments", "vet", 20L * 1024 * 1024);
                    binding.textAttachment.setText("Attachment selected: " + StorageUtils.getDisplayName(this, uri));
                    binding.buttonOpenAttachment.setVisibility(android.view.View.VISIBLE);
                } catch (Exception e) {
                    toast("Attachment import failed: " + e.getMessage());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVetVisitFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new PetRepository(this);

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        long visitId = getIntent().getLongExtra(EXTRA_VISIT_ID, 0L);
        if (visitId > 0) {
            editingVisit = repository.getDb().vetVisitDao().getById(visitId);
            if (editingVisit != null) populate();
        } else {
            binding.inputDate.setTag(System.currentTimeMillis());
            binding.inputDate.setText(FormatUtils.date(System.currentTimeMillis()));
        }

        binding.inputDate.setOnClickListener(v -> FormUiUtils.showDatePicker(this, readLongTag(binding.inputDate), binding.inputDate, null));
        binding.buttonPickAttachment.setOnClickListener(v -> attachmentPicker.launch(new String[]{"image/*", "application/pdf"}));
        binding.buttonOpenAttachment.setOnClickListener(v -> openAttachment());
        binding.buttonSave.setOnClickListener(v -> save());
        binding.buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populate() {
        binding.toolbar.setTitle("Edit vet visit");
        binding.inputDate.setTag(editingVisit.visitDateEpochMillis);
        binding.inputDate.setText(FormatUtils.date(editingVisit.visitDateEpochMillis));
        binding.inputClinic.setText(editingVisit.clinicName);
        binding.inputVet.setText(editingVisit.vetName);
        binding.inputReason.setText(editingVisit.reason);
        binding.inputNotes.setText(editingVisit.diagnosisNotes);
        savedAttachmentUri = editingVisit.attachmentUri;
        if (savedAttachmentUri != null && !savedAttachmentUri.isEmpty()) {
            binding.textAttachment.setText("Attachment saved");
            binding.buttonOpenAttachment.setVisibility(android.view.View.VISIBLE);
        }
        binding.buttonDelete.setVisibility(android.view.View.VISIBLE);
    }

    private void save() {
        VetVisit visit = editingVisit == null ? new VetVisit() : editingVisit;
        visit.petId = getIntent().getLongExtra(EXTRA_PET_ID, editingVisit == null ? 0L : editingVisit.petId);
        visit.visitDateEpochMillis = readLongTag(binding.inputDate);
        visit.clinicName = text(binding.inputClinic);
        visit.vetName = text(binding.inputVet);
        visit.reason = text(binding.inputReason);
        visit.diagnosisNotes = text(binding.inputNotes);
        visit.attachmentUri = savedAttachmentUri;

        if (visit.reason.isEmpty()) {
            toast("Reason is required");
            return;
        }

        if (visit.id == 0) {
            repository.getDb().vetVisitDao().insert(visit);
        } else {
            repository.getDb().vetVisitDao().update(visit);
        }
        setResult(RESULT_OK, new Intent());
        finish();
    }

    private void openAttachment() {
        try {
            if (savedAttachmentUri == null || savedAttachmentUri.isEmpty()) return;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = StorageUtils.toOpenableUri(this, savedAttachmentUri);
            intent.setDataAndType(uri, StorageUtils.getMimeType(this, uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            toast("Could not open attachment");
        }
    }

    private void confirmDelete() {
        if (editingVisit == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.confirm_delete)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    repository.getDb().vetVisitDao().delete(editingVisit);
                    setResult(RESULT_OK);
                    finish();
                })
                .show();
    }

    private long readLongTag(android.widget.EditText editText) {
        Object tag = editText.getTag();
        return tag instanceof Long ? (Long) tag : System.currentTimeMillis();
    }

    private String text(android.widget.EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
