package com.smartshield.wifi.presentation.channel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.smartshield.wifi.R;
import com.smartshield.wifi.domain.WifiNetwork;
import com.smartshield.wifi.presentation.customview.ChannelSpectrumView;
import com.smartshield.wifi.presentation.scanner.ScannerViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment presenting the Wi-Fi Channel Frequency Spectrum, channel overlap,
 * co-channel interference levels, and optimal channel recommendations.
 */
public class ChannelAnalyzerFragment extends Fragment {

    private ScannerViewModel scannerViewModel;
    private ChannelSpectrumView spectrumView;
    private TabLayout tabLayoutBand;
    private TextView tvOptimalChannelDesc;
    private RecyclerView rvChannelStats;
    private ChannelStatsAdapter adapter;

    private List<WifiNetwork> currentNetworks = new ArrayList<>();
    private boolean is24Ghz = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_channel_analyzer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spectrumView = view.findViewById(R.id.spectrumView);
        tabLayoutBand = view.findViewById(R.id.tabLayoutBand);
        tvOptimalChannelDesc = view.findViewById(R.id.tvOptimalChannelDesc);
        rvChannelStats = view.findViewById(R.id.rvChannelStats);

        rvChannelStats.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ChannelStatsAdapter();
        rvChannelStats.setAdapter(adapter);

        tabLayoutBand.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                is24Ghz = (tab.getPosition() == 0);
                updateSpectrum();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        scannerViewModel = new ViewModelProvider(requireActivity()).get(ScannerViewModel.class);
        scannerViewModel.getScanResults().observe(getViewLifecycleOwner(), assessments -> {
            if (assessments != null) {
                List<WifiNetwork> nets = new ArrayList<>();
                for (com.smartshield.wifi.domain.TrustAssessment ta : assessments) {
                    nets.add(ta.getNetwork());
                }
                currentNetworks = nets;
                updateSpectrum();
            }
        });

        scannerViewModel.getDiscoveredNetworks().observe(getViewLifecycleOwner(), networks -> {
            if (networks != null && !networks.isEmpty()) {
                currentNetworks = networks;
                updateSpectrum();
            }
        });
    }

    private void updateSpectrum() {
        spectrumView.setNetworks(currentNetworks, is24Ghz);

        // Analyze channel counts
        Map<Integer, Integer> channelCounts = new HashMap<>();
        for (WifiNetwork net : currentNetworks) {
            if (is24Ghz && "2.4 GHz".equals(net.getBandString())) {
                int ch = net.getChannelNumber();
                channelCounts.put(ch, channelCounts.getOrDefault(ch, 0) + 1);
            } else if (!is24Ghz && !"2.4 GHz".equals(net.getBandString())) {
                int ch = net.getChannelNumber();
                channelCounts.put(ch, channelCounts.getOrDefault(ch, 0) + 1);
            }
        }

        // Recommend best channel
        if (is24Ghz) {
            int[] primaryChannels = {1, 6, 11};
            int bestCh = 6;
            int minCount = Integer.MAX_VALUE;
            for (int ch : primaryChannels) {
                int count = channelCounts.getOrDefault(ch, 0);
                if (count < minCount) {
                    minCount = count;
                    bestCh = ch;
                }
            }
            tvOptimalChannelDesc.setText("Recommended: Channel " + bestCh + " (Non-overlapping with " + minCount + " active APs)");
        } else {
            tvOptimalChannelDesc.setText("5 GHz Band active: Lower congestion & zero co-channel overlap");
        }

        // Build adapter list items
        List<ChannelStatItem> items = new ArrayList<>();
        if (is24Ghz) {
            for (int ch = 1; ch <= 14; ch++) {
                int count = channelCounts.getOrDefault(ch, 0);
                if (count > 0 || ch == 1 || ch == 6 || ch == 11) {
                    items.add(new ChannelStatItem(ch, count, is24Ghz));
                }
            }
        } else {
            int[] fiveGhzChannels = {36, 40, 44, 48, 149, 153, 157, 161, 165};
            for (int ch : fiveGhzChannels) {
                int count = channelCounts.getOrDefault(ch, 0);
                items.add(new ChannelStatItem(ch, count, is24Ghz));
            }
        }
        adapter.setItems(items);
    }

    private static class ChannelStatItem {
        final int channel;
        final int apCount;
        final boolean is24Ghz;

        ChannelStatItem(int channel, int apCount, boolean is24Ghz) {
            this.channel = channel;
            this.apCount = apCount;
            this.is24Ghz = is24Ghz;
        }
    }

    private static class ChannelStatsAdapter extends RecyclerView.Adapter<ChannelStatsAdapter.ViewHolder> {

        private final List<ChannelStatItem> items = new ArrayList<>();

        void setItems(List<ChannelStatItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel_info, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChannelStatItem item = items.get(position);
            holder.tvChannelBadge.setText("Ch " + item.channel);
            holder.tvChannelTitle.setText("Channel " + item.channel + (item.is24Ghz ? " (2.4 GHz)" : " (5 GHz)"));
            holder.tvChannelSub.setText(item.apCount + " Access Points detected on this channel");

            if (item.apCount == 0) {
                holder.tvRatingChip.setText("Optimal");
                holder.tvRatingChip.setBackgroundColor(0xFF166534);
            } else if (item.apCount <= 2) {
                holder.tvRatingChip.setText("Moderate");
                holder.tvRatingChip.setBackgroundColor(0xFF854D0E);
            } else {
                holder.tvRatingChip.setText("Congested");
                holder.tvRatingChip.setBackgroundColor(0xFF991B1B);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvChannelBadge, tvChannelTitle, tvChannelSub, tvRatingChip;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvChannelBadge = itemView.findViewById(R.id.tvChannelBadge);
                tvChannelTitle = itemView.findViewById(R.id.tvChannelTitle);
                tvChannelSub = itemView.findViewById(R.id.tvChannelSub);
                tvRatingChip = itemView.findViewById(R.id.tvRatingChip);
            }
        }
    }
}
