package com.example.petcare.ui.pets;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcare.R;
import com.example.petcare.data.PetRepository;
import com.example.petcare.data.entities.Pet;
import com.example.petcare.databinding.ItemPetCardBinding;
import com.example.petcare.util.AgeUtils;
import com.example.petcare.util.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {
    public enum Mode { DASHBOARD, PETS }

    public interface OnPetClickListener { void onClick(Pet pet); }
    public interface OnPetEditListener { void onEdit(Pet pet); }

    private final OnPetClickListener onClickListener;
    private final OnPetEditListener onEditListener;
    private final PetRepository repository;
    private final Mode mode;
    private final List<Pet> pets = new ArrayList<>();

    public PetAdapter(OnPetClickListener onClickListener, OnPetEditListener onEditListener, PetRepository repository, Mode mode) {
        this.onClickListener = onClickListener;
        this.onEditListener = onEditListener;
        this.repository = repository;
        this.mode = mode;
    }

    public void submitList(List<Pet> items) {
        pets.clear();
        pets.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPetCardBinding binding = ItemPetCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PetViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = pets.get(position);
        boolean selected = repository.getSelectedPetId() == pet.id;

        holder.binding.petName.setText(pet.name);
        holder.binding.petSpecies.setText(pet.species + " • " + safe(pet.breed));
        holder.binding.petAge.setText(AgeUtils.compactAge(pet));

        if (mode == Mode.DASHBOARD) {
            holder.binding.petStats.setText(repository.getLastWeightSummary(pet));
            holder.binding.petExtra.setVisibility(View.VISIBLE);
            holder.binding.petExtra.setText("Next vaccine: " + repository.getNextVaccinationSummary(pet.id));
        } else {
            holder.binding.petStats.setText(selected ? "Active profile" : "Tap to select");
            holder.binding.petExtra.setVisibility(View.GONE);
        }

        holder.binding.selectedBadge.setVisibility(selected && mode == Mode.PETS ? View.VISIBLE : View.GONE);
        holder.binding.petCardRoot.setStrokeWidth(selected && mode == Mode.PETS ? 4 : 1);
        holder.binding.petCardRoot.setStrokeColor(
                selected && mode == Mode.PETS
                        ? ThemeUtils.getAccentColor(holder.itemView.getContext())
                        : ContextCompat.getColor(holder.itemView.getContext(), R.color.pet_border)
        );

        if (!TextUtils.isEmpty(pet.photoUri)) {
            holder.binding.petThumbnail.setImageURI(Uri.parse(pet.photoUri));
        } else {
            holder.binding.petThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (mode == Mode.PETS) {
                if (repository.getSelectedPetId() == pet.id) {
                    onEditListener.onEdit(pet);
                } else {
                    repository.setSelectedPetId(pet.id);
                    notifyDataSetChanged();
                    onClickListener.onClick(pet);
                }
            } else {
                onClickListener.onClick(pet);
            }
        });
    }

    @Override
    public int getItemCount() { return pets.size(); }

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
