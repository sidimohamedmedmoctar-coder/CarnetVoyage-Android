package com.mahmoud.carnetvoyage.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mahmoud.carnetvoyage.R;
import com.mahmoud.carnetvoyage.model.Memory;
import com.mahmoud.carnetvoyage.util.Constants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder> {

    public interface OnMemoryClickListener {
        void onMemoryClick(Memory memory);
        void onMemoryLongClick(Memory memory);
    }

    private final List<Memory> memories;
    private final OnMemoryClickListener listener;
    private final boolean canModify;

    public MemoryAdapter(List<Memory> memories, OnMemoryClickListener listener, boolean canModify) {
        this.memories = memories;
        this.listener = listener;
        this.canModify = canModify;
    }

    @NonNull
    @Override
    public MemoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_memory, parent, false);
        return new MemoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
        holder.bind(memories.get(position));
    }

    @Override
    public int getItemCount() {
        return memories.size();
    }

    class MemoryViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivPhoto;
        private final TextView tvTitle;
        private final TextView tvDate;
        private final TextView tvDescription;
        private final TextView tvCoords;

        MemoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCoords = itemView.findViewById(R.id.tvCoords);
        }

        void bind(Memory memory) {
            tvTitle.setText(memory.getTitle());

            // Photo : construire l'URL complète en évitant le double slash
            String photoUrl = memory.getPhotoUrl();
            if (photoUrl != null && !photoUrl.isEmpty()) {
                String base = Constants.BASE_URL.endsWith("/")
                        ? Constants.BASE_URL.substring(0, Constants.BASE_URL.length() - 1)
                        : Constants.BASE_URL;
                String fullUrl = photoUrl.startsWith("/") ? base + photoUrl : base + "/" + photoUrl;
                ivPhoto.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(fullUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(ivPhoto);
            } else {
                ivPhoto.setVisibility(View.GONE);
            }

            // Date visitée (format : "mercredi 14 mai 2026 à 15h30")
            if (memory.getVisitedAt() != null && !memory.getVisitedAt().isEmpty()) {
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(memory.getVisitedAt());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                            "EEEE d MMMM yyyy 'à' HH'h'mm", Locale.FRENCH);
                    tvDate.setText("🕒 " + formatter.format(dateTime));
                } catch (Exception e) {
                    tvDate.setText("🕒 " + memory.getVisitedAt());
                }
                tvDate.setVisibility(View.VISIBLE);
            } else {
                tvDate.setVisibility(View.GONE);
            }

            // Description (optionnelle)
            if (memory.getDescription() != null && !memory.getDescription().isEmpty()) {
                tvDescription.setText(memory.getDescription());
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Coordonnées GPS (4 décimales)
            if (memory.getLatitude() != null && memory.getLongitude() != null) {
                tvCoords.setText(String.format(Locale.US, "📍 %.4f, %.4f",
                        memory.getLatitude(), memory.getLongitude()));
                tvCoords.setVisibility(View.VISIBLE);
            } else {
                tvCoords.setVisibility(View.GONE);
            }

            // Listeners
            itemView.setOnClickListener(v -> listener.onMemoryClick(memory));

            if (canModify) {
                itemView.setOnLongClickListener(v -> {
                    listener.onMemoryLongClick(memory);
                    return true;
                });
            } else {
                itemView.setOnLongClickListener(null);
            }
        }
    }
}
