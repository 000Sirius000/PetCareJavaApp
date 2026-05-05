package com.example.petcare.ui.petdetail;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.databinding.ActivityPetDetailBinding;
import com.example.petcare.ui.petdetail.sections.ActivityFragment;
import com.example.petcare.ui.petdetail.sections.FeedingFragment;
import com.example.petcare.ui.petdetail.sections.HealthFragment;
import com.example.petcare.ui.petdetail.sections.WeightFragment;
import com.example.petcare.util.ThemeUtils;

public class PetDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";
    public static final String EXTRA_INITIAL_TAB = "extra_initial_tab";

    public static final String TAB_ACTIVITY = "activity";
    public static final String TAB_FEEDING = "feeding";
    public static final String TAB_HEALTH = "health";
    public static final String TAB_WEIGHT = "weight";

    private ActivityPetDetailBinding binding;
    private long petId;
    private String themeAtCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityPetDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        themeAtCreate = ThemeUtils.getSavedTheme(this);

        petId = getIntent().getLongExtra(EXTRA_PET_ID, 0L);
        Pet pet = new PetRepository(this).getPet(petId);
        binding.petDetailToolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.petDetailToolbar.setNavigationOnClickListener(v -> finish());
        binding.petDetailToolbar.setTitle(pet == null ? "Pet detail" : pet.name);

        if (savedInstanceState == null) {
            String initial = getIntent().getStringExtra(EXTRA_INITIAL_TAB);
            if (TAB_FEEDING.equals(initial)) {
                binding.petDetailBottomNav.setSelectedItemId(R.id.nav_feeding);
                showFragment(FeedingFragment.newInstance(petId));
            } else if (TAB_HEALTH.equals(initial)) {
                binding.petDetailBottomNav.setSelectedItemId(R.id.nav_health);
                showFragment(HealthFragment.newInstance(petId));
            } else if (TAB_WEIGHT.equals(initial)) {
                binding.petDetailBottomNav.setSelectedItemId(R.id.nav_weight);
                showFragment(WeightFragment.newInstance(petId));
            } else {
                binding.petDetailBottomNav.setSelectedItemId(R.id.nav_activity);
                showFragment(ActivityFragment.newInstance(petId));
            }
        }

        binding.petDetailBottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_activity) {
                showFragment(ActivityFragment.newInstance(petId));
                return true;
            }
            if (id == R.id.nav_feeding) {
                showFragment(FeedingFragment.newInstance(petId));
                return true;
            }
            if (id == R.id.nav_health) {
                showFragment(HealthFragment.newInstance(petId));
                return true;
            }
            if (id == R.id.nav_weight) {
                showFragment(WeightFragment.newInstance(petId));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String savedTheme = ThemeUtils.getSavedTheme(this);
        if (themeAtCreate != null && !savedTheme.equals(themeAtCreate)) {
            recreate();
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.pet_detail_fragment_container, fragment).commit();
    }
}
