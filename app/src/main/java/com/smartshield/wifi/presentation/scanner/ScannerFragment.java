package com.smartshield.wifi.presentation.scanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smartshield.wifi.R;
import com.smartshield.wifi.domain.TrustAssessment;
import com.smartshield.wifi.presentation.detail.NetworkDetailActivity;
import com.smartshield.wifi.scanner.WifiScanReceiver;

/**
 * Fragment displaying discovered Wi-Fi Access Points.
 * Handles runtime permissions, scan throttling cooldown UI, risk tier filter chips,
 * and launches the Explainable Trust Assessment detail screen on tap.
 */
public class ScannerFragment extends Fragment implements WifiListAdapter.OnNetworkClickListener,
        WifiScanReceiver.ScanResultCallback {

    private ScannerViewModel viewModel;
    private WifiListAdapter adapter;

    private TextView tvThrottlingStatus;
    private TextView tvEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewWifi;
    private ChipGroup chipGroupRiskFilter;
    private FloatingActionButton fabScan;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                if (fineGranted) {
                    viewModel.refreshScan();
                } else {
                    Toast.makeText(getContext(),
                            "Location permission is mandatory for Wi-Fi scanning",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ScannerViewModel.class);

        tvThrottlingStatus = view.findViewById(R.id.tvThrottlingStatus);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerViewWifi = view.findViewById(R.id.recyclerViewWifi);
        chipGroupRiskFilter = view.findViewById(R.id.chipGroupRiskFilter);
        fabScan = view.findViewById(R.id.fabScan);

        setupRecyclerView();
        setupFilters();
        setupObservers();

        swipeRefreshLayout.setOnRefreshListener(() -> checkPermissionAndScan());
        fabScan.setOnClickListener(v -> checkPermissionAndScan());

        FloatingActionButton fabQr = view.findViewById(R.id.fabQr);
        if (fabQr != null) {
            fabQr.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.smartshield.wifi.presentation.qr.QrScannerActivity.class);
                startActivity(intent);
            });
        }

        View btnProbeCaptivePortal = view.findViewById(R.id.btnProbeCaptivePortal);
        if (btnProbeCaptivePortal != null) {
            btnProbeCaptivePortal.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Probing connected network for Captive Portal / HTTP Redirects...", Toast.LENGTH_SHORT).show();
                com.smartshield.wifi.util.CaptivePortalProbe.runProbe(result -> {
                    if (isAdded() && getContext() != null) {
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle(result.status.title)
                                .setMessage(result.status.description + "\n\nHTTP Response Code: " + result.responseCode
                                        + (result.redirectUrl != null ? "\nRedirect Location: " + result.redirectUrl : ""))
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
            });
        }

        checkPermissionAndScan();
    }

    private void setupRecyclerView() {
        adapter = new WifiListAdapter(this);
        recyclerViewWifi.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewWifi.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupRiskFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipAll) {
                viewModel.setGroupedBySsid(false);
                viewModel.setRiskFilter("ALL");
            } else if (id == R.id.chipGroupSSID) {
                viewModel.setGroupedBySsid(true);
                viewModel.setRiskFilter("ALL");
            } else if (id == R.id.chipSafe) {
                viewModel.setRiskFilter("SAFE");
            } else if (id == R.id.chipMedium) {
                viewModel.setRiskFilter("MEDIUM_RISK");
            } else if (id == R.id.chipHigh) {
                viewModel.setRiskFilter("HIGH_RISK");
            }
        });
    }

    private void setupObservers() {
        viewModel.getScanResults().observe(getViewLifecycleOwner(), assessments -> {
            adapter.submitList(assessments);
            tvEmptyState.setVisibility(assessments != null && !assessments.isEmpty() ?
                    View.GONE : View.VISIBLE);
        });

        viewModel.getCooldownSeconds().observe(getViewLifecycleOwner(), seconds -> {
            if (seconds != null && seconds > 0) {
                tvThrottlingStatus.setVisibility(View.VISIBLE);
                tvThrottlingStatus.setText(getString(R.string.scan_throttled_msg, seconds));
            } else {
                tvThrottlingStatus.setVisibility(View.GONE);
            }
        });

        viewModel.getIsScanning().observe(getViewLifecycleOwner(), isScanning -> {
            swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(isScanning));
        });
    }

    private boolean isLocationServiceEnabled() {
        android.location.LocationManager lm = (android.location.LocationManager)
                requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        if (lm == null) return false;
        return lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
               lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
    }

    private void checkPermissionAndScan() {
        if (!isLocationServiceEnabled()) {
            Toast.makeText(getContext(),
                    "Please turn ON Location (GPS) in phone settings! Android requires Location to scan Wi-Fi.",
                    Toast.LENGTH_LONG).show();
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            viewModel.refreshScan();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && viewModel.getWifiScanner() != null) {
            viewModel.getWifiScanner().registerScanReceiver(requireContext(), this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (viewModel != null && viewModel.getWifiScanner() != null) {
            viewModel.getWifiScanner().unregisterScanReceiver(requireContext());
        }
    }

    @Override
    public void onScanResultsReady(boolean success) {
        if (viewModel != null) {
            viewModel.onScanBroadcastReceived();
        }
    }

    @Override
    public void onNetworkClicked(TrustAssessment assessment) {
        Intent intent = new Intent(requireContext(), NetworkDetailActivity.class);
        intent.putExtra(NetworkDetailActivity.EXTRA_ASSESSMENT, assessment);
        startActivity(intent);
    }
}
