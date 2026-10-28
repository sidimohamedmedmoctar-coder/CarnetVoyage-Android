package com.mahmoud.carnetvoyage.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mahmoud.carnetvoyage.R;
import com.mahmoud.carnetvoyage.model.Trip;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
        void onTripLongClick(Trip trip);
    }

    private final List<Trip> trips;
    private final OnTripClickListener listener;

    public TripAdapter(List<Trip> trips, OnTripClickListener listener) {
        this.trips = trips;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = trips.get(position);

        holder.tvTitle.setText(trip.getTitle());

        // Description (peut être vide)
        if (trip.getDescription() != null && !trip.getDescription().isEmpty()) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(trip.getDescription());
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Dates
        holder.tvDates.setText(formatDates(trip.getStartDate(), trip.getEndDate()));

        // Badge "Partagé" si ce n'est pas notre voyage
        holder.tvShared.setVisibility(trip.isOwner() ? View.GONE : View.VISIBLE);
        if (!trip.isOwner()) {
            holder.tvShared.setText("🤝 par " + trip.getOwnerUsername());
        }

        // Clic
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTripClick(trip);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onTripLongClick(trip);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    // Formate "2026-07-15" + "2026-07-25" → "📅 15 juil. - 25 juil. 2026"
    private String formatDates(String start, String end) {
        if (start == null && end == null) return "📅 Aucune date";
        try {
            DateTimeFormatter input = DateTimeFormatter.ISO_LOCAL_DATE;
            DateTimeFormatter output = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);

            String result = "📅 ";
            if (start != null) {
                result += LocalDate.parse(start, input).format(output);
            }
            if (end != null) {
                result += " → " + LocalDate.parse(end, input).format(output);
            }
            return result;
        } catch (Exception e) {
            return "📅 " + (start != null ? start : "") +
                    (end != null ? " → " + end : "");
        }
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDates, tvShared;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvShared = itemView.findViewById(R.id.tvShared);
        }
    }
}