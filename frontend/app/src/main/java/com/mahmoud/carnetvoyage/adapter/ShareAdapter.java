package com.mahmoud.carnetvoyage.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.mahmoud.carnetvoyage.R;
import com.mahmoud.carnetvoyage.model.ShareResponse;

import java.util.List;

public class ShareAdapter extends RecyclerView.Adapter<ShareAdapter.ShareViewHolder> {

    public interface OnUnshareClickListener {
        void onUnshare(ShareResponse share);
    }

    private final List<ShareResponse> shares;
    private final OnUnshareClickListener listener;

    public ShareAdapter(List<ShareResponse> shares, OnUnshareClickListener listener) {
        this.shares = shares;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShareViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_share, parent, false);
        return new ShareViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShareViewHolder holder, int position) {
        ShareResponse share = shares.get(position);
        holder.tvUsername.setText(share.getSharedWithUsername());
        holder.tvEmail.setText(share.getSharedWithEmail());
        holder.btnUnshare.setOnClickListener(v -> listener.onUnshare(share));
    }

    @Override
    public int getItemCount() {
        return shares.size();
    }

    static class ShareViewHolder extends RecyclerView.ViewHolder {
        final TextView tvUsername;
        final TextView tvEmail;
        final MaterialButton btnUnshare;

        ShareViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnUnshare = itemView.findViewById(R.id.btnUnshare);
        }
    }
}
