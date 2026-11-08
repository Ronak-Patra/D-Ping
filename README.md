# D Ping — Android Network Troubleshooting Toolkit

[![Platform](https://img.shields.io/badge/Platform-Android%208.0+-green.svg)](https://android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-Android%2015%20(API%2035)-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-blue.svg)](https://developer.android.com/jetpack/compose)
[![Storage](https://img.shields.io/badge/Storage-Room%20%2B%20SQLite%20(Local--First)-orange.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**D Ping** is a high-performance, mobile-first network diagnostic toolkit engineered for network administrators, IT engineers, DevOps practitioners, and security analysts. Built natively with Jetpack Compose Material 3 in a dark-obsidian cyber-cyan/teal aesthetic.

---

## ⚡ Core Features

### 1. Ping Diagnostic Engine & Live Latency Chart
* **Dual Display Modes**: Toggle seamlessly between CMD Console output and real-time SVG-like bezier latency waveforms.
* **Continuous & Precision Ping**: Real-time ICMP execution via system process measuring min/avg/max latency, jitter, and packet loss.
* **Quick Presets**: ⚡ Quick Test (5 pkts), 📊 Standard Test (20 pkts), and ♾️ Continuous Test with background execution support.

### 2. Multi-Provider Internet Speed Test
* **Multi-Provider CDN Clusters**: Test real-world throughput with dynamic server selection.
* **Live Telemetry**: Real-time download/upload speed gauges, round-trip latency, jitter calculation, and live waveform canvas.
* **Prominent Metrics**: Simultaneous high-visibility display of Download, Upload, Ping, and Jitter.

### 3. Subnet Scanner & IP Range Inspector
* **Subnet Occupancy Mapping**: High-concurrency CIDR scanner mapping IPv4 subnets (e.g., `/24`, `/26`, `/28`).
* **Visual Status Filter**: Segmented chips to view **Occupied**, **Available (Free)**, or **All** IP addresses.
* **Device Telemetry**: Resolves hostnames, MAC addresses, hardware vendors, and device types (Computers, Phones, Routers, Printers, IoT).

### 4. LAN Device Discovery
* **Active Host Discovery**: Discovers all active devices connected to the local Wi-Fi or Ethernet network.
* **Category Filtering**: Quickly filter detected nodes by Routers/Gateways, Workstations, Mobile Devices, Printers, or Smart Home IoT hardware.

### 5. My IP & Geolocation Telemetry
* **Dual Network Inspection**: Local Network (LAN) IPv4/IPv6, default gateway, subnet mask, and DNS servers.
* **Public WAN Intelligence**: WAN IPv4, Country, ISP, Autonomous System (ASN), and reverse DNS host.
* **Native Maps Integration**: Deep link integration launching Google Maps or default system map app via `geo:` URI with satellite coordinates.

### 6. Network Tools Hub
* **DNS Lookup**: Query `A`, `AAAA`, `MX`, `TXT`, `NS`, and `CNAME` records via DoH (DNS-over-HTTPS).
* **Port Scanner**: High-speed TCP port scanner for common and custom ports (HTTP 80, HTTPS 443, SSH 22, SMB 445, etc.).
* **Subnet Calculator**: 100% offline CIDR prefix, wildcard mask, network address, broadcast address, and usable host count calculator.
* **Network Interfaces**: Inspect `wlan0`, `eth0`, `rmnet`, and `tun` (VPN) interfaces with MTU and IP assignments.
* **HTTP / HTTPS Checker**: Latency, HTTP status codes, and header analysis.
* **SSL / TLS Certificate Inspector**: Validate certificate expiration dates, subject, issuer, and signature algorithms.

### 7. Full 23-Language Localization (I18N)
Instant reactive translation across all screens without app restart:
* **14 Indian Languages**: Hindi (हिन्दी), Bengali (বাংলা), Punjabi (ਪੰਜਾਬੀ), Marathi (मराठी), Gujarati (ગુજરાતી), Tamil (தமிழ்), Telugu (తెలుగు), Kannada (ಕನ್ನಡ), Malayalam (മലയാളം), Odia (ଓଡ଼ିଆ), Assamese (অসমীয়া), Urdu (اردو), Nepali (नेपाली), Sanskrit (संस्कृतम्).
* **9 Global Languages**: English, Spanish (Español), Chinese (中文), German (Deutsch), French (Français), Russian (Русский), Hebrew (עברית), Turkish (Türkçe), Korean (한국어).

### 8. Diagnostic Vault & CSV Export
* **Persistent History**: Local-first SQLite Room database storing diagnostic sessions.
* **Export Standards**: One-tap RFC 4180 CSV export via Android Sharesheet or file storage.

---

## 🏗️ Architecture & Tech Stack

```
com.dping.app
├── data/              # Room Database, DAOs, Entities, Models
├── di/                # Dependency container & preferences
├── network/           # Ping process, Traceroute, SpeedTest, Discovery, DNS
├── ui/
│   ├── components/    # Cyberpunk design system (Cards, Inputs, Buttons)
│   ├── i18n/          # AppStrings, IndianLanguages, GlobalLanguages
│   ├── screens/       # Jetpack Compose UI Screens
│   ├── theme/         # Color palettes, Typography, M3 Themes
│   └── viewmodels/    # AAC StateFlow ViewModels
└── utils/             # LocaleHelper, ExportManager, SubnetUtils
```

* **UI Layer**: 100% Declarative Jetpack Compose with Material 3.
* **State Management**: Unidirectional Data Flow (UDF) powered by Kotlin `StateFlow`.
* **Concurrency**: Structured Kotlin Coroutines with IO Dispatchers.
* **Storage**: Jetpack Room with SQLite + Jetpack DataStore Preferences.
* **Toolchain**: Kotlin 2.0.21, AGP 8.8, Java 17 LTS, Target SDK 35 (Android 15).

---

## 🛠️ Building & Installation

### Prerequisites
* Android Studio Ladybug or newer.
* Java Development Kit (JDK) 17 LTS.
* Android SDK Platform 35.

### Command Line Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Ronak-Patra/D-Ping.git
   cd D-Ping
   ```

2. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

3. **Build Signed Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```
   The signed APK will be output to `app/build/outputs/apk/release/app-release.apk`.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — see the LICENSE file for details.
