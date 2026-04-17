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

public class PetDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "extra_pet_id";

    private ActivityPetDetailBinding binding;
    private long petId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPetDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        petId = getIntent().getLongExtra(EXTRA_PET_ID, 0L);
        Pet pet = new PetRepository(this).getPet(petId);
        binding.petDetailToolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.petDetailToolbar.setNavigationOnClickListener(v -> finish());
        binding.petDetailToolbar.setTitle(pet == null ? "Pet detail" : pet.name);

        if (savedInstanceState == null) {
            showFragment(HealthFragment.newInstance(petId));
        }

        binding.petDetailBottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_health) {
                showFragment(HealthFragment.newInstance(petId));
                return true;
            }
            if (id == R.id.nav_feeding) {
                showFragment(FeedingFragment.newInstance(petId));
                return true;
            }
            if (id == R.id.nav_activity) {
                showFragment(ActivityFragment.newInstance(petId));
                return true;
            }
            return false;
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.pet_detail_fragment_container, fragment).commit();
    }
}
