package com.example.petcare.ui.dashboard;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.ActivitySession;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.data.entities.Vaccination;
import com.example.petcare.databinding.ItemDashboardPetCardBinding;
import com.example.petcare.util.AgeUtils;
import com.example.petcare.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class DashboardPetAdapter extends RecyclerView.Adapter<DashboardPetAdapter.ViewHolder> {
    public interface OnPetClickListener {
        void onPetClick(Pet pet);
    }

    private final List<Pet> items = new ArrayList<>();
    private final PetRepository repository;
    private final OnPetClickListener onPetClickListener;

    public DashboardPetAdapter(PetRepository repository, OnPetClickListener onPetClickListener) {
        this.repository = repository;
        this.onPetClickListener = onPetClickListener;
    }

    public void submitList(List<Pet> pets) {
        items.clear();
        if (pets != null) {
            items.addAll(pets);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDashboardPetCardBinding binding = ItemDashboardPetCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pet pet = items.get(position);
        holder.binding.petName.setText(pet.name);
        holder.binding.petAgeBreed.setText(AgeUtils.compactAgeWithBreed(pet));
        holder.binding.petSpecies.setText((pet.species == null ? "Pet" : pet.species) + " • " + safe(pet.breed));

        int reminders = repository.getPendingReminderCount(pet.id);
        holder.binding.petReminders.setText(reminders + (reminders == 1 ? " pending reminder" : " pending reminders"));

        ActivitySession latestActivity = repository.getLatestActivitySession(pet.id);
        if (latestActivity == null) {
            holder.binding.petLastActivity.setText("Last activity: no activity yet");
        } else {
            holder.binding.petLastActivity.setText(
                    "Last activity: " + latestActivity.activityType + " • " +
                            latestActivity.durationMinutes + " min • " +
                            FormatUtils.date(latestActivity.sessionDateEpochMillis)
            );
        }

        Vaccination nextVaccination = repository.getNextVaccinationDue(pet.id);
        if (nextVaccination == null || nextVaccination.nextDueAt == null) {
            holder.binding.petNextVaccination.setText("Next vaccination: not scheduled");
        } else {
            holder.binding.petNextVaccination.setText(
                    "Next vaccination: " + nextVaccination.vaccineName + " • " +
                            FormatUtils.date(nextVaccination.nextDueAt)
            );
        }

        int progress = repository.getWeeklyActivityProgressPercent(pet.id, pet.weeklyActivityGoalMinutes);
        holder.binding.petGoalProgress.setProgress(progress);

        if (!TextUtils.isEmpty(pet.photoUri)) {
            holder.binding.petThumbnail.setImageURI(Uri.parse(pet.photoUri));
        } else {
            holder.binding.petThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> onPetClickListener.onPetClick(pet));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown breed" : value.trim();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemDashboardPetCardBinding binding;

        ViewHolder(ItemDashboardPetCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
