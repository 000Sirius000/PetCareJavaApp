package com.example.petcare.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcare.databinding.ItemSimpleRowBinding;

import java.util.ArrayList;
import java.util.List;

public class SimpleRowAdapter extends RecyclerView.Adapter<SimpleRowAdapter.RowViewHolder> {
    public interface RowMapper<T> {
        String title(T item);
        String subtitle(T item);
        String meta(T item);
    }

    public interface OnRowClickListener {
        void onRowClick(Object item);
    }

    private final RowMapper<Object> mapper;
    private final List<Object> items = new ArrayList<>();
    private OnRowClickListener onRowClickListener;

    @SuppressWarnings("unchecked")
    public <T> SimpleRowAdapter(RowMapper<T> mapper) {
        this.mapper = (RowMapper<Object>) mapper;
    }

    public void setOnRowClickListener(OnRowClickListener onRowClickListener) {
        this.onRowClickListener = onRowClickListener;
    }

    public void submitList(List<?> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSimpleRowBinding binding = ItemSimpleRowBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new RowViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        Object item = items.get(position);
        holder.binding.rowTitle.setText(mapper.title(item));

        String subtitle = mapper.subtitle(item);
        holder.binding.rowSubtitle.setText(subtitle);
        holder.binding.rowSubtitle.setVisibility(isBlank(subtitle) ? View.GONE : View.VISIBLE);

        String meta = mapper.meta(item);
        holder.binding.rowMeta.setText(meta);
        holder.binding.rowMeta.setVisibility(isBlank(meta) ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (onRowClickListener != null) {
                onRowClickListener.onRowClick(item);
            }
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        final ItemSimpleRowBinding binding;

        RowViewHolder(ItemSimpleRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
