# 🛡️ QubeGuard: Advanced Privacy, Pi-hole DNS Sinkhole & Private Browser for Android

**QubeGuard** is a comprehensive, open-source Android privacy shield, Pi-hole style local DNS sinkhole, and hardened Private Browser powered by a hybrid **Deterministic Engine**, **Local VPN Proxy**, and an on-device **Transformer AI Classifier**.

---

## 🌟 Key Highlights & Capabilities

### 🛡️ **3-Layer Hybrid Protection Engine**
1. **Layer 1: Deterministic Engine & Compiled Rules**
   - **Radix Tree Domain Matching:** Indexed TLD-first (right-to-left) so subdomains inherit parent domain rules automatically.
   - **Bloom Filter:** Ultra-fast O(1) negative lookup filter to bypass unblocked domains instantly.
   - **Regex & AdBlock Plus Engine:** Converts glob rules (`||domain.com^`, `*adserver*`) to optimized Java regular expressions.
   - **Custom Sources & Categories:** Manage sources for Ads, Privacy, Security, Social Media, and Annoyances.

2. **Layer 2: Local VPN Service & Pi-hole DNS Proxy**
   - **UDP Port 53 Interception:** System-wide local DNS proxy running directly on-device.
   - **Pi-hole Sinkhole Response Modes:** Configurable blocking response (`NXDOMAIN`, `0.0.0.0 Null IP`, `NODATA`, `REFUSED`).
   - **Custom Local DNS Records:** Create A/AAAA/CNAME record mappings (e.g. `router.lan -> 192.168.1.1`).
   - **Conditional Forwarding:** Route local domain queries (e.g. `*.home.arpa`) to your home router/LAN DNS server.
   - **Configurable Upstream DNS:** Toggle between Cloudflare (1.1.1.1), AdGuard (94.140.14.14), Quad9 (9.9.9.9), Google (8.8.8.8), or custom DNS IPs with DNSSEC toggle.
   - **Per-App Split Tunneling:** Exclude specific installed Android apps from VPN DNS filtering.
   - **Quick Settings Tile:** Toggle system protection on/off directly from Android's Quick Settings drawer.

3. **Layer 3: Local Transformer AI URL Classifier**
   - On-device **BERT sequence classifier** executed locally via `onnxruntime-android`.
   - Source model: `r3ddkahili/final-complete-malicious-url-model`.
   - Classifies domain URLs into **Benign, Defacement, Phishing, Malware**.
   - 100% private and offline — zero browsing data or URLs leave your device.

---

### 🌐 **Chrome-Style Hardened Private Browser**
- **Modern Chrome-Style UI:** Top pill omnibox, lock indicator, active tab counter `[ N ]`, grid tab switcher, and 3-dots overflow menu.
- **HTML5 Full-Screen Video Player:** Supports full-screen playback for YouTube, Vimeo, and HTML5 `<video>` tags.
- **Media Download Detector:** Automatically detects downloadable video/audio URLs (`.mp4`, `.webm`, `.m3u8`, `.m4a`) with an instant download modal.
- **Reader Mode:** Distraction-free article extraction view.
- **Find in Page:** In-page text search tool with match highlighting (`findAllAsync`).
- **Search Engine Selector:** Choose between DuckDuckGo, StartPage, Google, Bing, or Ecosia.
- **Qube Isolation Profiles:** Isolated browser storage, history, and bookmarks per Qube profile (Work, Personal, Finance, Incognito).
- **Cosmetic Ad Hiding:** Injects CSS script rules to hide leftover empty ad banner boxes.
- **Browser Settings:** Custom controls for JavaScript, Popups, Do Not Track headers, Text Zoom level, and Auto-Clear on Exit.

---

### 📊 **Pi-hole Query Analytics & Rule Management**
- **Live DNS Query Monitor:** View real-time DNS queries, timestamp, block status, and reason.
- **Analytics Dashboard:** Visual breakdown of Total Queries, Blocked count, Block percentage, Top Blocked Domains, and Top Permitted Domains.
- **Custom Allowlist & User Rules:** Add domain whitelists and regex rules with live test evaluator.
- **Backup & Restore:** Export or import your entire blocklist configuration, allowlists, local DNS records, and settings in JSON format.

---

## 📱 Android Setup & Installation Guide

### Prerequisites
- Android device running **Android 7.0 (API level 24)** or higher.
- Java Development Kit **JDK 17 or JDK 21**.
- **Android Studio Jellyfish / Ladybug** or Gradle 8.8+.

### 🔨 Building from Source
1. **Clone the Repository:**
   ```bash
   git clone https://github.com/qubeguard/qubeguard-android.git
   cd qubeguard-android
   ```

2. **Run Unit Tests:**
   ```bash
   gradle testDebugUnitTest
   ```

3. **Run Android Lint Verification:**
   ```bash
   gradle :app:lintDebug
   ```

4. **Assemble Debug APK:**
   ```bash
   gradle assembleDebug
   ```
   The generated APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🚀 How to Use QubeGuard

1. **Activate System Protection:**
   - Open **QubeGuard** on your Android device.
   - Tap **Start Protection** on the Home screen to launch the local VPN DNS proxy.
   - Grant the standard Android VPN permission prompt when requested.

2. **Configure Pi-hole DNS Sinkhole & Local DNS:**
   - Go to **Settings -> Pi-hole Sinkhole & Local DNS Settings**.
   - Select your preferred **Sinkhole Response Mode** (`NXDOMAIN`, `0.0.0.0`, `NODATA`, `REFUSED`).
   - Tap **Custom Local DNS Records** to add local hostname mappings (e.g. `nas.home -> 192.168.1.50`).

3. **Monitor Live DNS Queries:**
   - Go to **Settings -> View DNS Logs** or access the **Blocklists** tab.
   - Search queries by domain name, filter by All / Blocked / Allowed, or view Top Blocked Domains.

4. **Use the Private Browser:**
   - Tap the **Browser** tab in the bottom navigation bar.
   - Enter any URL or search query in the omnibox.
   - Use the **[ N ]** tab switcher button to open or close multi-tab sessions.
   - Tap the 3-dots menu for **Reader Mode**, **Find in Page**, **Downloads**, **Qube Switching**, or **Video Playback**.

---

## 📜 License

Distributed under the **GPLv3 License**. See [LICENSE](LICENSE) for full licensing terms.
