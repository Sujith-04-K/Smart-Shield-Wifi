package com.smartshield.wifi.presentation.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartshield.wifi.R;
import com.smartshield.wifi.data.KnownNetworkEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter displaying user-verified trusted Wi-Fi networks saved in SmartShield Registry.
 */
public class KnownNetworkAdapter extends RecyclerView.Adapter<KnownNetworkAdapter.KnownViewHolder> {

    public interface OnDeleteListener {
        void onDeleteKnownNetwork(String bssid);
    }

    private List<KnownNetworkEntity> items = new ArrayList<>();
    private final OnDeleteListener listener;

    public KnownNetworkAdapter(OnDeleteListener listener) {
        this.listener = listener;
    }

    public void submitList(List<KnownNetworkEntity> newItems) {
        this.items = newItems != null ? new ArrayList<>(newItems) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public KnownViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_known_network, parent, false);
        return new KnownViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KnownViewHolder holder, int position) {
        KnownNetworkEntity entity = items.get(position);
        holder.bind(entity, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class KnownViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvKnownSsid;
        private final TextView tvKnownBssid;
        private final ImageButton btnRemoveKnown;

        public KnownViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKnownSsid = itemView.findViewById(R.id.tvKnownSsid);
            tvKnownBssid = itemView.findViewById(R.id.tvKnownBssid);
            btnRemoveKnown = itemView.findViewById(R.id.btnRemoveKnown);
        }

        public void bind(KnownNetworkEntity entity, OnDeleteListener listener) {
            tvKnownSsid.setText(entity.getSsid());
            tvKnownBssid.setText("BSSID: " + entity.getBssid());

            btnRemoveKnown.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteKnownNetwork(entity.getBssid());
                }
            });
        }
    }
}
