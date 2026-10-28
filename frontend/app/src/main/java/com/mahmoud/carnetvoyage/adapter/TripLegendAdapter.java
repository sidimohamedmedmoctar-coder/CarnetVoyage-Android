package com.mahmoud.carnetvoyage.adapter;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mahmoud.carnetvoyage.R;
import com.mahmoud.carnetvoyage.model.Memory;
import com.mahmoud.carnetvoyage.model.Trip;
import com.mahmoud.carnetvoyage.util.MapColorUtils;

import java.util.List;

public class TripLegendAdapter extends RecyclerView.Adapter<TripLegendAdapter.LegendViewHolder> {

    public interface OnTripLegendClickListener {
        void onTripLegendClick(TripEntry entry);
    }

    /** Entrée agrégée : un voyage + la liste de ses souvenirs géolocalisés */
    public static class TripEntry {
        public final Trip trip;
        public final List<Memory> memories;

        public TripEntry(Trip trip, List<Memory> memories) {
            this.trip = trip;
            this.memories = memories;
        }
    }

    private final List<TripEntry> entries;
    private final OnTripLegendClickListener listener;

    public TripLegendAdapter(List<TripEntry> entries, OnTripLegendClickListener listener) {
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LegendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_legend_trip, parent, false);
        return new LegendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LegendViewHolder holder, int position) {
        TripEntry entry = entries.get(position);
        Trip trip = entry.trip;

        // Cercle teinté avec la couleur du voyage
        int color = MapColorUtils.getColorForTripId(trip.getId());
        holder.ivTripColor.setColorFilter(color, PorterDuff.Mode.SRC_IN);

        // Titre
        holder.tvTripTitle.setText(trip.getTitle() != null ? trip.getTitle() : "");

        // Méta : "X souvenir(s)" + 🤝 si partagé
        int count = entry.memories.size();
        String meta = count + (count <= 1 ? " souvenir" : " souvenirs");
        if (!trip.isOwner()) meta += "  🤝";
        holder.tvTripMeta.setText(meta);

        holder.itemView.setOnClickListener(v -> listener.onTripLegendClick(entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class LegendViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivTripColor;
        final TextView tvTripTitle;
        final TextView tvTripMeta;

        LegendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTripColor  = itemView.findViewById(R.id.ivTripColor);
            tvTripTitle  = itemView.findViewById(R.id.tvTripTitle);
            tvTripMeta   = itemView.findViewById(R.id.tvTripMeta);
        }
    }
}
