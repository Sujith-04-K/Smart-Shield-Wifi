package com.smartshield.wifi.presentation.history;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.smartshield.wifi.R;
import com.smartshield.wifi.data.KnownNetworkEntity;
import com.smartshield.wifi.data.ScanHistoryEntity;

import java.util.List;

/**
 * Fragment displaying historical Wi-Fi scan assessment logs and the user's
 * verified trusted Wi-Fi access points registry.
 */
public class HistoryFragment extends Fragment implements KnownNetworkAdapter.OnDeleteListener {

    private HistoryViewModel viewModel;
    private HistoryListAdapter historyAdapter;
    private KnownNetworkAdapter knownAdapter;

    private TabLayout tabLayoutHistory;
    private RecyclerView recyclerViewHistory;
    private TextView tvEmptyHistory;
    private FloatingActionButton fabClearLogs;

    private int currentTab = 0; // 0 = Logs, 1 = Registry
    private List<ScanHistoryEntity> currentHistoryList;
    private List<KnownNetworkEntity> currentKnownList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        tabLayoutHistory = view.findViewById(R.id.tabLayoutHistory);
        recyclerViewHistory = view.findViewById(R.id.recyclerViewHistory);
        tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory);
        fabClearLogs = view.findViewById(R.id.fabClearLogs);

        historyAdapter = new HistoryListAdapter();
        knownAdapter = new KnownNetworkAdapter(this);

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewHistory.setAdapter(historyAdapter);

        setupTabs();
        setupObservers();

        FloatingActionButton fabExport = view.findViewById(R.id.fabExport);
        if (fabExport != null) {
            fabExport.setOnClickListener(v -> showExportDialog());
        }

        fabClearLogs.setOnClickListener(v -> {
            if (currentTab == 0) {
                viewModel.clearAllScanHistory();
            }
        });
    }

    private void showExportDialog() {
        if (currentHistoryList == null || currentHistoryList.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "No scan history records available to export.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Export as CSV (.csv)", "Export as JSON (.json)"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Export Scan History Logs")
                .setItems(options, (dialog, which) -> {
                    try {
                        java.io.File file;
                        if (which == 0) {
                            file = com.smartshield.wifi.util.DataExportManager.exportHistoryToCsv(requireContext(), currentHistoryList);
                        } else {
                            file = com.smartshield.wifi.util.DataExportManager.exportHistoryToJson(requireContext(), currentHistoryList);
                        }
                        android.widget.Toast.makeText(getContext(), "Exported successfully: " + file.getName(), android.widget.Toast.LENGTH_LONG).show();

                        // Share File Intent
                        android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                                requireContext(),
                                requireContext().getPackageName() + ".fileprovider",
                                file
                        );
                        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                        shareIntent.setType(which == 0 ? "text/csv" : "application/json");
                        shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                        shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(android.content.Intent.createChooser(shareIntent, "Share Export File"));
                    } catch (Exception e) {
                        android.widget.Toast.makeText(getContext(), "Export failed: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupTabs() {
        tabLayoutHistory.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateUIForCurrentTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupObservers() {
        viewModel.getAllScanHistory().observe(getViewLifecycleOwner(), list -> {
            currentHistoryList = list;
            if (currentTab == 0) {
                updateUIForCurrentTab();
            }
        });

        viewModel.getAllKnownNetworks().observe(getViewLifecycleOwner(), list -> {
            currentKnownList = list;
            if (currentTab == 1) {
                updateUIForCurrentTab();
            }
        });
    }

    private void updateUIForCurrentTab() {
        if (currentTab == 0) {
            recyclerViewHistory.setAdapter(historyAdapter);
            historyAdapter.submitList(currentHistoryList);
            boolean empty = (currentHistoryList == null || currentHistoryList.isEmpty());
            tvEmptyHistory.setVisibility(empty ? View.VISIBLE : View.GONE);
            tvEmptyHistory.setText("No historical scan logs found.");
            fabClearLogs.setVisibility(empty ? View.GONE : View.VISIBLE);
        } else {
            recyclerViewHistory.setAdapter(knownAdapter);
            knownAdapter.submitList(currentKnownList);
            boolean empty = (currentKnownList == null || currentKnownList.isEmpty());
            tvEmptyHistory.setVisibility(empty ? View.VISIBLE : View.GONE);
            tvEmptyHistory.setText("No verified networks in SmartShield registry.\nMark a network as verified in its detail view.");
            fabClearLogs.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDeleteKnownNetwork(String bssid) {
        viewModel.deleteKnownNetwork(bssid);
    }
}
