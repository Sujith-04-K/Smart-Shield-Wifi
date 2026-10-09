/**
 * SmartShield Wi-Fi — Desktop Web Dashboard Engine
 * Implements Multi-Criteria Trust Assessment (MCDA), Levenshtein Evil Twin Detector,
 * XAI Audit Trail, and Channel Spectrum Analysis.
 */

const state = {
    networks: [],
    knownBssids: new Set(['A4:6C:2A:1B:3E:10', '28:D1:27:D2:24:C7']),
    selectedBssid: null,
    isScanning: false
};

const ROGUE_OUI_PREFIXES = ['00:13:37', '00:c0:ca', 'de:ad:be'];

const INITIAL_NETWORKS = [
    {
        ssid: "Swargalokam-2.4G_plus",
        bssid: "28:D1:27:D2:24:C7",
        securityType: "WPA2",
        capabilities: "[WPA2-PSK-CCMP][ESS]",
        rssi: -43,
        channel: 9,
        vendorName: "Cisco Systems"
    },
    {
        ssid: "Swargalokam-2.4G",
        bssid: "28:D1:27:D2:24:C6",
        securityType: "WPA2",
        capabilities: "[WPA2-PSK-CCMP][ESS]",
        rssi: -57,
        channel: 9,
        vendorName: "Aruba Networks"
    },
    {
        ssid: "Swargalokam-5G",
        bssid: "8C:13:E2:52:18:0B",
        securityType: "WPA2",
        capabilities: "[WPA2-PSK-CCMP][ESS]",
        rssi: -80,
        channel: 149,
        vendorName: "Aruba Networks"
    },
    {
        ssid: "Rahul-T",
        bssid: "8C:C7:C3:52:F2:EF",
        securityType: "WPA2",
        capabilities: "[WPA2-PSK-CCMP][ESS]",
        rssi: -86,
        channel: 1,
        vendorName: "TP-Link"
    }
];

// Levenshtein Distance
function levenshteinDistance(a, b) {
    if (!a || !b) return (a || b || "").length;
    const matrix = Array.from({ length: a.length + 1 }, () => Array(b.length + 1).fill(0));
    for (let i = 0; i <= a.length; i++) matrix[i][0] = i;
    for (let j = 0; j <= b.length; j++) matrix[0][j] = j;

    for (let i = 1; i <= a.length; i++) {
        for (let j = 1; j <= b.length; j++) {
            const cost = a[i - 1].toLowerCase() === b[j - 1].toLowerCase() ? 0 : 1;
            matrix[i][j] = Math.min(
                matrix[i - 1][j] + 1,
                matrix[i][j - 1] + 1,
                matrix[i - 1][j - 1] + cost
            );
        }
    }
    return matrix[a.length][b.length];
}

function areSsidsSimilar(ssid1, ssid2) {
    if (!ssid1 || !ssid2) return false;
    if (ssid1.toLowerCase() === ssid2.toLowerCase()) return true;
    return levenshteinDistance(ssid1, ssid2) <= 2;
}

function evaluateSimilarityScore(target, neighbors) {
    if (!target || !neighbors) return 30;
    let foundSimilarSsid = false;

    for (const neighbor of neighbors) {
        if (target.bssid.toLowerCase() === neighbor.bssid.toLowerCase()) continue;

        if (areSsidsSimilar(target.ssid, neighbor.ssid)) {
            const isTargetOpen = target.securityType.toUpperCase() === "OPEN";
            const isNeighborOpen = neighbor.securityType.toUpperCase() === "OPEN";

            const isTargetSecure = !isTargetOpen && target.securityType.toUpperCase() !== "UNKNOWN";
            const isNeighborSecure = !isNeighborOpen && neighbor.securityType.toUpperCase() !== "UNKNOWN";

            if ((isTargetOpen && isNeighborSecure) || (isTargetSecure && isNeighborOpen)) {
                return 0; // CRITICAL EVIL TWIN ATTACK
            }
            if (target.ssid.toLowerCase() !== neighbor.ssid.toLowerCase()) {
                foundSimilarSsid = true;
            }
        }
    }
    return foundSimilarSsid ? 15 : 30;
}

// Multi-Criteria Trust Engine
function evaluateTrustAssessment(target, neighbors, isKnown) {
    const reasons = [];

    // 1. Security Protocol (40%)
    let securityScore = 0;
    const secType = (target.securityType || "UNKNOWN").toUpperCase();
    const caps = (target.capabilities || "").toUpperCase();

    if (secType.includes("WPA3")) {
        securityScore = 40;
        reasons.push({ text: "✓ WPA3 Security Protocol (+40/40): Latest SAE wireless encryption.", type: "good" });
    } else if (secType.includes("WPA2")) {
        securityScore = 30;
        reasons.push({ text: "✓ WPA2 Security Protocol (+30/40): Standard AES encryption enabled.", type: "good" });
    } else if (secType.includes("WPA")) {
        securityScore = 15;
        reasons.push({ text: "! WPA Legacy Security (+15/40): Older TKIP cipher; upgrade recommended.", type: "warn" });
    } else if (secType === "OPEN") {
        securityScore = 0;
        reasons.push({ text: "✗ OPEN Network - Unencrypted (+0/40): Traffic eavesdropping risk.", type: "danger" });
    } else {
        securityScore = 5;
        reasons.push({ text: "! Unknown Security Cipher (+5/40).", type: "warn" });
    }

    if (secType !== "OPEN" && caps.includes("TKIP") && !caps.includes("CCMP")) {
        securityScore = Math.max(0, securityScore - 10);
        reasons.push({ text: "✗ Insecure TKIP Cipher (-10/40): Cryptographically weak cipher.", type: "danger" });
    }

    // 2. Similarity & Evil Twin (30%)
    const similarityScore = evaluateSimilarityScore(target, neighbors);
    const isEvilTwin = (similarityScore === 0);

    if (similarityScore === 30) {
        reasons.push({ text: "✓ Unique SSID (+30/30): Single physical AP broadcast.", type: "good" });
    } else if (similarityScore === 15) {
        reasons.push({ text: `✓ Multi-AP Enterprise Network (+15/30): Roaming APs broadcast '${target.ssid}'.`, type: "good" });
    } else {
        reasons.push({ text: "✗ CRITICAL ALERT: Evil Twin Attack (+0/30)! Mismatched security on matching SSIDs.", type: "danger" });
    }

    // 3. Known Registry (20%)
    let knownScore = 20;
    const isRogueHardware = ROGUE_OUI_PREFIXES.some(p => target.bssid.toLowerCase().startsWith(p));

    if (isRogueHardware) {
        knownScore = Math.max(0, knownScore - 10);
        reasons.push({ text: `✗ Rogue Hardware Indicator (-10/20): BSSID OUI (${target.vendorName}) matches pentest gear!`, type: "danger" });
    } else if (isKnown) {
        reasons.push({ text: `✓ Saved in Verified Registry (+20/20): Trusted network (${target.vendorName}).`, type: "good" });
    } else {
        reasons.push({ text: `✓ Clean Baseline Profile (+20/20): (${target.vendorName}).`, type: "good" });
    }

    // 4. Signal RSSI (10%)
    let signalScore = 3;
    const rssi = target.rssi;
    if (rssi > -65) {
        signalScore = 10;
        reasons.push({ text: `✓ Strong Signal Quality (+10/10): Excellent RSSI (${rssi} dBm).`, type: "good" });
    } else if (rssi >= -80) {
        signalScore = 7;
        reasons.push({ text: `✓ Moderate Signal Quality (+7/10): Good RSSI (${rssi} dBm).`, type: "good" });
    } else {
        signalScore = 3;
        reasons.push({ text: `✗ Weak Signal Quality (+3/10): Low RSSI (${rssi} dBm).`, type: "danger" });
    }

    const totalScore = securityScore + similarityScore + knownScore + signalScore;
    return { target, securityScore, similarityScore, knownScore, signalScore, totalScore, isEvilTwin, reasons };
}

// Render Top Metrics
function renderMetrics() {
    document.getElementById('metric-total-nets').innerText = state.networks.length;
    let safeCount = 0;
    let evilCount = 0;
    let knownCount = 0;

    state.networks.forEach(net => {
        const isK = state.knownBssids.has(net.bssid);
        if (isK) knownCount++;
        const audit = evaluateTrustAssessment(net, state.networks, isK);
        if (audit.totalScore >= 80) safeCount++;
        if (audit.isEvilTwin) evilCount++;
    });

    document.getElementById('metric-safe-nets').innerText = safeCount;
    document.getElementById('metric-evil-nets').innerText = evilCount;
    document.getElementById('metric-known-nets').innerText = knownCount;
}

// Render Network List Box
function renderNetworkList() {
    const listEl = document.getElementById('network-list-box');
    const searchVal = document.getElementById('search-input').value.toLowerCase();
    const filterSec = document.getElementById('filter-security').value;

    listEl.innerHTML = '';

    const filtered = state.networks.filter(net => {
        const matchSearch = net.ssid.toLowerCase().includes(searchVal) || net.bssid.toLowerCase().includes(searchVal);
        const matchSec = filterSec === 'ALL' || net.securityType.toUpperCase() === filterSec;
        return matchSearch && matchSec;
    });

    document.getElementById('scan-badge-count').innerText = `${filtered.length} Networks`;

    if (filtered.length === 0) {
        listEl.innerHTML = '<div style="padding: 30px; text-align: center; color: var(--text-muted);">No matching Wi-Fi networks discovered.</div>';
        return;
    }

    filtered.forEach(net => {
        const isKnown = state.knownBssids.has(net.bssid);
        const audit = evaluateTrustAssessment(net, state.networks, isKnown);
        const isSelected = state.selectedBssid === net.bssid;

        let scoreClass = 'safe';
        if (audit.totalScore < 50) scoreClass = 'danger';
        else if (audit.totalScore < 80) scoreClass = 'caution';

        const item = document.createElement('div');
        item.className = `net-item-card ${isSelected ? 'selected' : ''}`;
        item.onclick = () => selectNetwork(net.bssid);

        item.innerHTML = `
            <div>
                <div class="net-title-row">
                    <i class="fa-solid ${net.securityType === 'OPEN' ? 'fa-unlock' : 'fa-lock'}" style="color: ${net.securityType === 'OPEN' ? 'var(--accent-red)' : 'var(--accent-cyan)'}"></i>
                    ${net.ssid}
                    ${audit.isEvilTwin ? '<span class="evil-twin-flag"><i class="fa-solid fa-triangle-exclamation"></i> EVIL TWIN</span>' : ''}
                </div>
                <div class="net-sub-row">
                    <span class="mono-text">${net.bssid}</span>
                    <span>• Ch ${net.channel}</span>
                    <span>• ${net.rssi} dBm</span>
                    <span>• ${net.securityType}</span>
                </div>
            </div>
            <div class="score-pill ${scoreClass}">${audit.totalScore}</div>
        `;
        listEl.appendChild(item);
    });
}

function selectNetwork(bssid) {
    state.selectedBssid = bssid;
    renderNetworkList();
    renderAuditDetail();
}

// Render Audit Detail Panel
function renderAuditDetail() {
    const noSelView = document.getElementById('no-selection-state');
    const detailView = document.getElementById('audit-detail-content');
    const pillEl = document.getElementById('selected-ssid-pill');

    if (!state.selectedBssid) {
        noSelView.classList.remove('hidden');
        detailView.classList.add('hidden');
        pillEl.innerText = 'Select Network';
        return;
    }

    const targetNet = state.networks.find(n => n.bssid === state.selectedBssid);
    if (!targetNet) return;

    noSelView.classList.add('hidden');
    detailView.classList.remove('hidden');
    pillEl.innerText = targetNet.ssid;

    const isKnown = state.knownBssids.has(targetNet.bssid);
    const audit = evaluateTrustAssessment(targetNet, state.networks, isKnown);

    document.getElementById('trust-num').innerText = audit.totalScore;
    document.getElementById('target-ssid-title').innerText = targetNet.ssid;
    document.getElementById('target-bssid-text').innerText = `BSSID: ${targetNet.bssid}`;
    document.getElementById('target-sec-pill').innerHTML = `<i class="fa-solid fa-lock"></i> ${targetNet.securityType}`;
    document.getElementById('target-rssi-pill').innerHTML = `<i class="fa-solid fa-signal"></i> ${targetNet.rssi} dBm`;
    document.getElementById('target-vendor-pill').innerHTML = `<i class="fa-solid fa-microchip"></i> ${targetNet.vendorName}`;

    const verdictTag = document.getElementById('verdict-tag');
    const gaugeCircle = document.getElementById('gauge-circle');

    let color = 'var(--accent-green)';
    let verdict = 'SAFE';

    if (audit.totalScore < 50) { color = 'var(--accent-red)'; verdict = 'DANGEROUS'; }
    else if (audit.totalScore < 80) { color = 'var(--accent-amber)'; verdict = 'CAUTION'; }

    verdictTag.innerText = verdict;
    verdictTag.className = `verdict-pill ${verdict.toLowerCase()}`;

    const deg = (audit.totalScore / 100) * 360;
    gaugeCircle.style.background = `conic-gradient(${color} 0deg ${deg}deg, rgba(255, 255, 255, 0.1) ${deg}deg 360deg)`;
    gaugeCircle.style.boxShadow = `0 0 25px ${color}`;

    // Bars
    document.getElementById('score-sec').innerText = `${audit.securityScore}/40`;
    document.getElementById('fill-sec').style.width = `${(audit.securityScore / 40) * 100}%`;

    document.getElementById('score-sim').innerText = `${audit.similarityScore}/30`;
    document.getElementById('fill-sim').style.width = `${(audit.similarityScore / 30) * 100}%`;

    document.getElementById('score-known').innerText = `${audit.knownScore}/20`;
    document.getElementById('fill-known').style.width = `${(audit.knownScore / 20) * 100}%`;

    document.getElementById('score-signal').innerText = `${audit.signalScore}/10`;
    document.getElementById('fill-signal').style.width = `${(audit.signalScore / 10) * 100}%`;

    // Reasons List
    const reasonsContainer = document.getElementById('xai-reasons-list');
    reasonsContainer.innerHTML = '';
    audit.reasons.forEach(r => {
        const item = document.createElement('div');
        item.className = `reason-card ${r.type}`;
        item.innerText = r.text;
        reasonsContainer.appendChild(item);
    });

    const toggleBtn = document.getElementById('btn-toggle-trust');
    if (isKnown) {
        toggleBtn.innerHTML = '<i class="fa-solid fa-bookmark"></i> Remove from Registry';
        toggleBtn.className = 'btn outline-btn warning';
    } else {
        toggleBtn.innerHTML = '<i class="fa-solid fa-bookmark"></i> Save to Registry';
        toggleBtn.className = 'btn outline-btn';
    }
}

// Render Channel Bars List
function renderChannelBars() {
    const listEl = document.getElementById('channel-bars-list');
    listEl.innerHTML = '';

    state.networks.forEach(net => {
        const row = document.createElement('div');
        row.className = 'ch-bar-row';
        row.innerHTML = `
            <span><b>${net.ssid}</b> (Ch ${net.channel})</span>
            <span class="mono-text">${net.rssi} dBm</span>
        `;
        listEl.appendChild(row);
    });
}

// Spectrum Scan Trigger
function triggerSpectrumScan() {
    if (state.isScanning) return;
    state.isScanning = true;

    const scanBtn = document.getElementById('btn-scan-spectrum');
    const fillBar = document.getElementById('scan-progress-fill');
    const icon = scanBtn.querySelector('.spinner-icon');

    scanBtn.disabled = true;
    icon.classList.add('fa-spin');
    fillBar.style.width = '0%';

    let p = 0;
    const interval = setInterval(() => {
        p += 25;
        fillBar.style.width = `${p}%`;

        if (p >= 100) {
            clearInterval(interval);
            state.isScanning = false;
            scanBtn.disabled = false;
            icon.classList.remove('fa-spin');

            state.networks.forEach(net => {
                net.rssi = Math.min(-35, Math.max(-95, net.rssi + Math.floor(Math.random() * 7) - 3));
            });

            renderMetrics();
            renderNetworkList();
            renderAuditDetail();
            renderChannelBars();
        }
    }, 150);
}

// Setup Lab Simulators
function setupSimulations() {
    document.getElementById('sim-campus').onclick = () => {
        state.networks = [...INITIAL_NETWORKS];
        state.selectedBssid = state.networks[0].bssid;
        renderMetrics();
        renderNetworkList();
        renderAuditDetail();
        renderChannelBars();
    };

    document.getElementById('sim-eviltwin').onclick = () => {
        state.networks = [
            { ssid: "Airport_Free_WiFi", bssid: "00:11:22:33:44:55", securityType: "WPA2", capabilities: "[WPA2-PSK-CCMP]", rssi: -60, channel: 6, vendorName: "Airport Tech" },
            { ssid: "Airport_Free_WiFi", bssid: "00:13:37:DE:AD:BF", securityType: "OPEN", capabilities: "[ESS]", rssi: -40, channel: 6, vendorName: "Rogue SoftAP (Alfa)" }
        ];
        state.selectedBssid = "00:13:37:DE:AD:BF";
        renderMetrics();
        renderNetworkList();
        renderAuditDetail();
        renderChannelBars();
    };

    document.getElementById('sim-open').onclick = () => {
        state.networks = [
            { ssid: "Unencrypted_Public_AP", bssid: "DE:AD:BE:EF:00:01", securityType: "OPEN", capabilities: "[ESS]", rssi: -50, channel: 1, vendorName: "Unknown Vendor" }
        ];
        state.selectedBssid = "DE:AD:BE:EF:00:01";
        renderMetrics();
        renderNetworkList();
        renderAuditDetail();
        renderChannelBars();
    };
}

// Setup QR Tool
function setupQrTool() {
    const parseBtn = document.getElementById('btn-parse-qr');
    const input = document.getElementById('qr-input-text');
    const outBox = document.getElementById('qr-output-box');

    const generateQr = () => {
        const val = input.value.trim();
        let ssid = "Unknown", sec = "OPEN", pass = "None";

        const sMatch = val.match(/S:([^;]+);/);
        const tMatch = val.match(/T:([^;]+);/);
        const pMatch = val.match(/P:([^;]+);/);

        if (sMatch) ssid = sMatch[1];
        if (tMatch) sec = tMatch[1];
        if (pMatch) pass = pMatch[1];

        outBox.innerHTML = `
            <div style="font-size:0.85rem;">
                <div><b>Parsed SSID:</b> ${ssid}</div>
                <div><b>Security Standard:</b> ${sec}</div>
                <div><b>Passphrase:</b> ${pass}</div>
            </div>
        `;
    };

    parseBtn.onclick = generateQr;
    document.getElementById('btn-generate-qr').onclick = () => {
        const target = state.networks.find(n => n.bssid === state.selectedBssid);
        if (target) {
            input.value = `WIFI:S:${target.ssid};T:${target.securityType};P:SmartPass2026;;`;
            generateQr();
            document.querySelector('.web-bottom-grid').scrollIntoView({ behavior: 'smooth' });
        }
    };
    generateQr();
}

// Custom Modal Controls
function setupModal() {
    const modal = document.getElementById('custom-modal');
    document.getElementById('open-modal-btn').onclick = () => modal.classList.remove('hidden');
    document.getElementById('close-modal-btn').onclick = () => modal.classList.add('hidden');
    document.getElementById('cancel-modal-btn').onclick = () => modal.classList.add('hidden');

    document.getElementById('custom-ap-form').onsubmit = (e) => {
        e.preventDefault();
        const ssid = document.getElementById('new-ssid').value.trim();
        const bssid = document.getElementById('new-bssid').value.trim();
        const securityType = document.getElementById('new-security').value;
        const rssi = parseInt(document.getElementById('new-rssi').value, 10);

        const newAp = {
            ssid,
            bssid,
            securityType,
            capabilities: securityType === 'OPEN' ? '[ESS]' : `[${securityType}-PSK-CCMP][ESS]`,
            rssi,
            channel: 6,
            vendorName: "Custom User AP"
        };

        state.networks.push(newAp);
        state.selectedBssid = bssid;
        modal.classList.add('hidden');

        renderMetrics();
        renderNetworkList();
        renderAuditDetail();
        renderChannelBars();
    };
}

// Global Initialization
window.addEventListener('DOMContentLoaded', () => {
    state.networks = [...INITIAL_NETWORKS];
    state.selectedBssid = state.networks[0].bssid;

    document.getElementById('btn-scan-spectrum').onclick = triggerSpectrumScan;
    document.getElementById('search-input').oninput = renderNetworkList;
    document.getElementById('filter-security').onchange = renderNetworkList;

    document.getElementById('btn-toggle-trust').onclick = () => {
        if (!state.selectedBssid) return;
        if (state.knownBssids.has(state.selectedBssid)) {
            state.knownBssids.delete(state.selectedBssid);
        } else {
            state.knownBssids.add(state.selectedBssid);
        }
        renderMetrics();
        renderNetworkList();
        renderAuditDetail();
    };

    setupSimulations();
    setupQrTool();
    setupModal();

    renderMetrics();
    renderNetworkList();
    renderAuditDetail();
    renderChannelBars();
});
