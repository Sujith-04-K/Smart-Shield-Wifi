# SmartShield Wi-Fi 🛡️📶

An explainable, rule-based mobile computing security framework designed for Android devices to evaluate Wi-Fi trust in real time before connecting to nearby wireless networks.

---

## 🌟 Key Features

- **Real-Time Trust Assessment Engine**:
  - Computes weighted trust scores (0–100) using multi-criteria decision assessment.
  - Evaluates radio spectrum parameters, encryption protocols (Open, WEP, WPA2, WPA3), and captive portals.
  - Transparent, explainable security recommendations (*"Why should I trust this network?"*).

- **Evil Twin & Spoofing Detection**:
  - Uses Levenshtein distance string similarity to identify SSID spoofing and rogue APs.
  - Flags security capability mismatches and duplicate BSSID anomalies.

- **Spectrum & Channel Analyzer**:
  - Real-time channel interference analysis for both 2.4 GHz and 5 GHz frequency bands.
  - Custom spectrum visualization for optimal channel selection.

- **QR Code Wi-Fi Scanner**:
  - Built-in QR scanner (ZXing) to inspect and securely connect to Wi-Fi networks from QR codes with pre-connection security audits.

- **Audit Reporting & Export**:
  - Export scan logs and network security audits to PDF, CSV, and JSON formats.

- **Multi-Platform Visualizers**:
  - **Android Application**: Native Android app (Java, MVVM, Room DB, Material Design).
  - **Streamlit Analytics Dashboard**: Interactive web dashboard (`streamlit_app.py`) for Wi-Fi threat analytics.
  - **Web Client**: Lightweight browser viewer (`web/`).

---

## 🏗️ Architecture

```
Smart Shield Wifi/
├── app/                        # Android application module
│   └── src/main/java/com/smartshield/wifi/
│       ├── data/               # Room DB (Entities, DAO, Repository)
│       ├── domain/             # Threat & Trust Assessment Engine, Evil Twin Detector
│       └── presentation/       # MVVM UI (Activities, Fragments, Custom Views)
├── web/                        # Web dashboard client
├── streamlit_app.py            # Streamlit security analytics application
├── ARCHITECTURE_DESIGN.md      # Detailed technical architecture report
└── build.gradle.kts            # Project build configuration
```

---

## 🚀 Getting Started

### Android App
1. Open the project in **Android Studio** (Hedgehog or newer recommended).
2. Allow Gradle sync to complete.
3. Connect an Android device (Android 8.0+ / API 26+) with Developer Options and USB Debugging enabled.
4. Grant Location and Nearby Wi-Fi permissions when prompted.
5. Build and run via `Shift + F10` or:
   ```bash
   ./gradlew assembleDebug
   ```

### Streamlit Dashboard
To run the companion threat analytics dashboard:
```bash
pip install streamlit pandas matplotlib seaborn
streamlit run streamlit_app.py
```

---

## 📄 License
This project is developed as an academic and open-source mobile computing security framework.
