package com.smartshield.wifi.presentation.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smartshield.wifi.R;
import com.smartshield.wifi.data.ScanHistoryEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter displaying historical scan assessment logs from Room Database.
 */
public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.HistoryViewHolder> {

    private List<ScanHistoryEntity> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public void submitList(List<ScanHistoryEntity> newItems) {
        this.items = newItems != null ? new ArrayList<>(newItems) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_log, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ScanHistoryEntity entity = items.get(position);
        holder.bind(entity, dateFormat);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvHistorySsid;
        private final TextView tvHistoryScore;
        private final TextView tvHistoryDate;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistorySsid = itemView.findViewById(R.id.tvHistorySsid);
            tvHistoryScore = itemView.findViewById(R.id.tvHistoryScore);
            tvHistoryDate = itemView.findViewById(R.id.tvHistoryDate);
        }

        public void bind(ScanHistoryEntity entity, SimpleDateFormat dateFormat) {
            tvHistorySsid.setText(entity.getSsid());
            String dateStr = dateFormat.format(new Date(entity.getTimestamp()));
            tvHistoryDate.setText(String.format("%s  •  %s", dateStr, entity.getBssid()));

            String risk = entity.getRiskLevel();
            int score = entity.getTotalScore();
            if ("SAFE".equals(risk)) {
                tvHistoryScore.setText(score + "/100 • SAFE");
                tvHistoryScore.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.risk_safe));
            } else if ("MEDIUM_RISK".equals(risk)) {
                tvHistoryScore.setText(score + "/100 • MEDIUM");
                tvHistoryScore.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.risk_medium));
            } else {
                tvHistoryScore.setText(score + "/100 • HIGH RISK");
                tvHistoryScore.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.risk_high));
            }
        }
    }
}
