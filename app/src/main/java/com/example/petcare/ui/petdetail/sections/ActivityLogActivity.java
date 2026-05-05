package com.example.petcare.ui.petdetail.sections;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.ui.forms.ActivitySessionFormActivity;
import com.example.petcare.util.FormatUtils;
import com.example.petcare.util.ThemeUtils;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class ActivityLogActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_activity_log);

        petId = getIntent().getLongExtra(EXTRA_PET_ID, 0L);
        repository = new PetRepository(this);
        container = findViewById(R.id.activity_log_container);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.button_add_activity).setOnClickListener(v -> openForm(0L));
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        container.removeAllViews();
        List<ActivitySession> items = repository.getActivitySessions(petId);
        if (items.isEmpty()) {
            addText("No activity entries yet", 16, false);
            return;
        }
        for (ActivitySession item : items) {
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

    private String title(ActivitySession item) {
        String distance = item.distance == null ? "" : " · " + FormatUtils.number(item.distance) + " km";
        return item.activityType + " · " + item.durationMinutes + " min" + distance;
    }

    private String subtitle(ActivitySession item) {
        return "Active date: " + FormatUtils.dateTime(item.sessionDateEpochMillis)
                + "\nNotes: " + FormatUtils.nullable(item.notes);
    }

    private void openForm(long activityId) {
        Intent intent = new Intent(this, ActivitySessionFormActivity.class);
        intent.putExtra(ActivitySessionFormActivity.EXTRA_PET_ID, petId);
        if (activityId > 0L) intent.putExtra(ActivitySessionFormActivity.EXTRA_ACTIVITY_ID, activityId);
        formLauncher.launch(intent);
    }
}
