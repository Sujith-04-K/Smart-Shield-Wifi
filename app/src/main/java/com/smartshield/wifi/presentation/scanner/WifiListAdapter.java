package com.smartshield.wifi.presentation.scanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smartshield.wifi.R;
import com.smartshield.wifi.domain.TrustAssessment;
import com.smartshield.wifi.domain.WifiNetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter displaying nearby scanned Wi-Fi Access Points.
 * Shows SSID, BSSID, encryption protocol, RSSI signal level, and color-coded Trust Score badge.
 */
public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.WifiViewHolder> {

    public interface OnNetworkClickListener {
        void onNetworkClicked(TrustAssessment assessment);
    }

    private List<TrustAssessment> items = new ArrayList<>();
    private final OnNetworkClickListener listener;

    public WifiListAdapter(OnNetworkClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<TrustAssessment> newItems) {
        this.items = newItems != null ? new ArrayList<>(newItems) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WifiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wifi_network, parent, false);
        return new WifiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiViewHolder holder, int position) {
        TrustAssessment item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class WifiViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvSsid;
        private final TextView tvRiskBadge;
        private final TextView tvBssidBand;
        private final TextView tvSecurity;
        private final TextView tvVendor;
        private final TextView tvSignal;

        public WifiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSsid = itemView.findViewById(R.id.tvSsid);
            tvRiskBadge = itemView.findViewById(R.id.tvRiskBadge);
            tvBssidBand = itemView.findViewById(R.id.tvBssidBand);
            tvSecurity = itemView.findViewById(R.id.tvSecurity);
            tvVendor = itemView.findViewById(R.id.tvVendor);
            tvSignal = itemView.findViewById(R.id.tvSignal);
        }

        public void bind(TrustAssessment assessment, OnNetworkClickListener listener) {
            WifiNetwork network = assessment.getNetwork();

            tvSsid.setText(network.getSsid());
            if (assessment.getApCount() > 1) {
                tvBssidBand.setText(String.format("🏢 Enterprise Campus Network (%d Routers Grouped) • Ch %d (%s)", assessment.getApCount(), network.getChannelNumber(), network.getBandString()));
            } else {
                tvBssidBand.setText(String.format("AP MAC: %s  •  Ch %d (%s)", network.getBssid(), network.getChannelNumber(), network.getBandString()));
            }
            tvSecurity.setText(network.getSecurityType());
            tvSignal.setText(String.format("RSSI: %d dBm (%d%%)", network.getRssi(), network.getSignalPercentage()));

            if (tvVendor != null) {
                if (network.isPotentialRogueHardware()) {
                    tvVendor.setText("⚠️ " + network.getVendorName());
                    tvVendor.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.risk_high));
                } else {
                    tvVendor.setText(network.getVendorName());
                }
            }

            // Format Risk Badge
            int score = assessment.getTotalScore();
            String riskLevel = assessment.getRiskLevel();

            if (assessment.isEvilTwinDetected()) {
                tvRiskBadge.setText("EVIL TWIN ALERT • " + score + "/100");
                tvRiskBadge.setBackgroundResource(R.drawable.bg_badge_high);
                tvRiskBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.risk_high));
            } else if ("SAFE".equals(riskLevel)) {
                tvRiskBadge.setText(score + "/100 • SAFE");
                tvRiskBadge.setBackgroundResource(R.drawable.bg_badge_safe);
                tvRiskBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.risk_safe));
            } else if ("MEDIUM_RISK".equals(riskLevel)) {
                tvRiskBadge.setText(score + "/100 • MEDIUM");
                tvRiskBadge.setBackgroundResource(R.drawable.bg_badge_medium);
                tvRiskBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.risk_medium));
            } else {
                tvRiskBadge.setText(score + "/100 • HIGH RISK");
                tvRiskBadge.setBackgroundResource(R.drawable.bg_badge_high);
                tvRiskBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.risk_high));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNetworkClicked(assessment);
                }
            });
        }
    }
}
