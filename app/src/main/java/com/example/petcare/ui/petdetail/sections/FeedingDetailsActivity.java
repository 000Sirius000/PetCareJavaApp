package com.example.petcare.ui.petdetail.sections;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.FeedingSchedule;
import com.example.petcare.ui.forms.FeedingScheduleFormActivity;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.Locale;

public class FeedingDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";

    private long petId;
    private PetRepository repository;
    private LinearLayout container;

    private final ActivityResultLauncher<Intent> formLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> reload());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feeding_details);

        petId = getIntent().getLongExtra(EXTRA_PET_ID, 0L);
        repository = new PetRepository(this);
        container = findViewById(R.id.feeding_details_container);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.button_add_feeding).setOnClickListener(v -> openForm(0L));
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        container.removeAllViews();
        List<FeedingSchedule> items = repository.getFeedingScheduleDetails(petId);
        if (items.isEmpty()) {
            addText("No feeding entries yet", 16, false);
            return;
        }

        for (FeedingSchedule item : items) {
            TextView row = addText(title(item) + "\n" + subtitle(item), 15, false);
            row.setPadding(18, 18, 18, 18);
            row.setBackgroundColor(getResources().getColor(R.color.pet_surface, getTheme()));
            row.setOnClickListener(v -> openForm(item.id));
        }
    }

    private TextView addText(String text, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(getResources().getColor(R.color.pet_text_primary, getTheme()));
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        container.addView(view, params);
        return view;
    }

    private String title(FeedingSchedule item) {
        return nullableMealName(item.mealName) + " - " + portionLabel(item.portion);
    }

    private String subtitle(FeedingSchedule item) {
        return "Food type: " + normalizeFoodType(item.foodType)
                + "\nFeeding date: " + dateTimeLabel(item);
    }

    private String dateTimeLabel(FeedingSchedule item) {
        if (item.createdAtEpochMillis > 0L) {
            return FormatUtils.humanDateTime(item.createdAtEpochMillis);
        }
        return String.format(Locale.getDefault(), "No date - %02d:%02d", item.hourOfDay, item.minute);
    }

    private String portionLabel(String portion) {
        return FormatUtils.number(FormatUtils.parseLeadingNumber(portion)) + " g";
    }

    private String nullableMealName(String mealName) {
        return mealName == null || mealName.trim().isEmpty() ? "Feeding" : mealName.trim();
    }

    private String normalizeFoodType(String type) {
        return type == null || type.trim().isEmpty() ? "Dry food" : type.trim();
    }

    private void openForm(long feedingId) {
        Intent intent = new Intent(this, FeedingScheduleFormActivity.class);
        intent.putExtra(FeedingScheduleFormActivity.EXTRA_PET_ID, petId);
        if (feedingId > 0L) intent.putExtra(FeedingScheduleFormActivity.EXTRA_SCHEDULE_ID, feedingId);
        formLauncher.launch(intent);
    }
}
