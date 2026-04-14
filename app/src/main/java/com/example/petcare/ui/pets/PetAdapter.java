package com.example.petcare.ui.pets;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.databinding.ItemPetCardBinding;

import java.util.ArrayList;
import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {
    public interface OnPetClickListener {
        void onClick(Pet pet);
    }

    public interface OnPetEditListener {
        void onEdit(Pet pet);
    }

    private final OnPetClickListener onClickListener;
    private final OnPetEditListener onEditListener;
    private final PetRepository repository;
    private final List<Pet> pets = new ArrayList<>();

    public PetAdapter(OnPetClickListener onClickListener, OnPetEditListener onEditListener, PetRepository repository) {
        this.onClickListener = onClickListener;
        this.onEditListener = onEditListener;
        this.repository = repository;
    }

    public void submitList(List<Pet> items) {
        pets.clear();
        pets.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPetCardBinding binding = ItemPetCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new PetViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = pets.get(position);
        holder.binding.petName.setText(pet.name);
        holder.binding.petSpecies.setText(pet.species + " • " + safe(pet.breed));
        int reminderCount = repository.getPendingReminderCount(pet.id);
        int progressPercent = repository.getWeeklyActivityProgressPercent(pet.id, pet.weeklyActivityGoalMinutes);
        holder.binding.petStats.setText(reminderCount + " reminders today");
        holder.binding.petGoalProgress.setProgress(progressPercent);
        if (!TextUtils.isEmpty(pet.photoUri)) {
            holder.binding.petThumbnail.setImageURI(Uri.parse(pet.photoUri));
        } else {
            holder.binding.petThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        holder.itemView.setOnClickListener(v -> onClickListener.onClick(pet));
        holder.binding.buttonEditPet.setOnClickListener(v -> onEditListener.onEdit(pet));
    }

    @Override
    public int getItemCount() {
        return pets.size();
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "Unknown breed" : value;
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        final ItemPetCardBinding binding;

        PetViewHolder(ItemPetCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
