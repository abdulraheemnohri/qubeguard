# 🛡️ QubeGuard

**QubeGuard** is an advanced Android ad, tracker, and malware blocking application with a built-in private browser and a hybrid deterministic/Transformer AI blocking engine. It provides **system-wide protection** via a VPN-based DNS proxy and **per-site blocking** in a hardened WebView.

---

## ✨ Features

### 🌐 **Three-Layer Blocking Engine**
1. **Layer 1: Deterministic Rules**
   - Fast, in-memory **Radix Tree** for domain/subdomain matching (indexed right-to-left for domain hierarchy lookups).
   - **Bloom Filter** for rapid negative lookups.
   - **Regex Engine** for complex URL and wildcard patterns.
   - Supports **AdBlock Plus, Hosts, and Regex** formats.

2. **Layer 2: DNS Blocking (VPN Service)**
   - Intercepts **UDP port 53 (DNS)** traffic.
   - Blocks requests at the **network level** across all applications.
   - Returns valid DNS **NXDOMAIN** responses for blocked queries.
   - Forwards allowed requests to **Cloudflare DNS (1.1.1.1)**.

3. **Layer 3: Optional Local Transformer AI Classifier**
   - On-device **BERT sequence classifier** executed via `onnxruntime-android`.
   - Source model: `r3ddkahili/final-complete-malicious-url-model`.
   - Classifies malicious URLs into **Benign, Defacement, Phishing, Malware**.
   - Entirely local inference — no browsing data is ever sent to remote APIs.

### 🔒 **Private Browser & Qubes**
- **Hardened WebView** with:
  - Disabled **WebGL, Geolocation, DOM Storage, Database Storage**.
  - Blocked **third-party cookies**.
  - Generic **User-Agent** to prevent fingerprinting.
- **Per-Qube Isolation**: Isolated browser profiles with custom cookies, cache, and history.
- **Incognito Qube**: Disposable session profile.

### 📋 **Blocklist Aggregation Engine**
- Supports **multiple blocklist sources** (EasyList, EasyPrivacy, AdGuard, StevenBlack, etc.).
- **Automatic updates** scheduled via `WorkManager`.
- **Deduplication & Normalization** into Room database.

### 🎨 **UI (Jetpack Compose)**
- **Main Screen**: VPN toggle, quick actions, layer status cards.
- **Private Browser**: Navigation bar, Qube profile selector, URL input.
- **Block Page**: Interstitial screen with **Allow Once**, **Allow Always**, **Keep Blocked**, and **Report False Positive**.
- **Settings**: Comprehensive controls for Blocklists, Qubes, Local AI, and Feedback.

---

## 📦 Building & Testing

### Prerequisites
- Android **API 24+** (Android 7.0 Nougat and above).
- **JDK 17+** (or Java 21).
- **Gradle 8.8+**.

### Build Commands
```bash
# Run unit tests
gradle testDebugUnitTest

# Assemble debug APK
gradle assembleDebug
```

---

## 📜 License

This project is licensed under the **GPLv3 License**. See [LICENSE](LICENSE) for details.
