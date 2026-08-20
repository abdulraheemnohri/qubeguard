# 🛡️ QubeGuard

**QubeGuard** is an advanced Android ad, tracker, and malware blocking application with a built-in private browser and a hybrid deterministic/ML blocking engine. It provides **system-wide protection** via a VPN-based DNS proxy and **per-site blocking** in a hardened WebView.

---

## ✨ Features

### 🌐 **Three-Layer Blocking Engine**
1. **Layer 1: Deterministic Rules**
   - Fast, in-memory **Radix Tree** for domain/subdomain matching.
   - **Bloom Filter** for rapid negative lookups.
   - **Regex Engine** for complex URL patterns.
   - Supports **AdBlock Plus, Hosts, and Regex** formats.

2. **Layer 2: DNS Blocking (VPN Service)**
   - Intercepts **UDP port 53 (DNS)** traffic.
   - Blocks requests at the **network level** (covers all apps).
   - Forwards allowed requests to **Cloudflare DNS (1.1.1.1)**.

3. **Layer 3: TFLite AI Classifier**
   - Detects **zero-day trackers, obfuscated ad domains, and phishing sites**.
   - Extracts **lexical, structural, and contextual features** from URLs/domains.
   - Uses a **lightweight Neural Network** for inference.

### 🔒 **Private Browser**
- **Hardened WebView** with:
  - Disabled **WebGL, Geolocation, DOM Storage, Database Storage**.
  - Blocked **third-party cookies**.
  - Generic **User-Agent** to prevent fingerprinting.
- **Per-Qube Isolation**: Each Qube has its own **cookies, cache, and history**.
- **Incognito Mode**: No persistent data.
- **HTTPS Upgrade**: Automatically upgrades `http://` to `https://`.

### 📋 **Blocklist Aggregation Engine**
- Supports **multiple blocklist sources** (e.g., EasyList, EasyPrivacy, Malware Domains, AdGuard Mobile).
- **Automatic updates** via `WorkManager`.
- **Normalization** of all formats into a unified AST.
- **Deduplication** to remove redundant rules.

### 🎯 **Policy Engine & Feedback System**
- **Final block/allow decisions** combining all layers.
- **User feedback** for false positives/negatives.
- **Opt-in telemetry** for improving the ML model.

### 🎨 **UI (Jetpack Compose)**
- **Home Screen**: VPN toggle, browser launcher, settings, blocked request counter.
- **Browser**: URL bar, navigation buttons, Qube selector.
- **Block Page**: Interstitial with **Allow Once / Always / Report False Positive**.
- **Settings**: Blocklist, Qube, ML, and feedback settings.

---

## 📦 Installation

### Prerequisites
- Android **API 24+** (Android 7.0 Nougat and above).
- **Kotlin 1.9.0+**.
- **Android Gradle Plugin 8.3.0+**.
- **Python 3.7+** (for model generation, optional).

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/abdulraheemnohri/qubeguard.git
   cd qubeguard
   ```

2. **Set up the TFLite Model** (Required):
   ```bash
   # Option 1: Generate a dummy model for testing
   pip install tensorflow numpy
   python scripts/generate_model.py
   
   # Option 2: Train a custom model (recommended for production)
   pip install tensorflow numpy pandas scikit-learn
   python scripts/train_model.py --data_path your_dataset.csv
   ```
   > See [Model Setup](#-model-setup) for detailed instructions.

3. Open the project in **Android Studio**.

4. Sync Gradle dependencies.

5. Build and run the app on an **Android device or emulator**.

---

## 🤖 Model Setup

QubeGuard uses a **TensorFlow Lite (TFLite) model** for ML-based blocking (Layer 3). You need to provide a model file before running the app.

### 📥 Option 1: Generate a Dummy Model (Quick Start)
For testing the app structure without real ML functionality:

```bash
# Install dependencies
pip install tensorflow numpy

# Generate a dummy model
python scripts/generate_model.py

# The model will be saved to: app/src/main/assets/qubeguard_model.tflite
```

> ⚠️ **Note:** The dummy model has random weights and will not provide accurate classifications. Use it only for testing the app flow.

---

### 🎓 Option 2: Train a Custom Model (Recommended)
For production use, train a model with real data:

1. **Prepare your dataset** in CSV format:
   ```csv
   url,label
   https://example.com,Legitimate
   https://ads.example.com,Ad
   https://tracker.example.com,Tracker
   https://malware-site.com,Malware
   https://phishing-site.com,Phishing
   https://analytics.example.com,Analytics
   ```

2. **Install dependencies:**
   ```bash
   pip install tensorflow numpy pandas scikit-learn
   ```

3. **Train the model:**
   ```bash
   python scripts/train_model.py --data_path your_dataset.csv
   ```

4. **Copy the model** to your Android project:
   ```bash
   cp app/src/main/assets/qubeguard_model.tflite <your-android-project>/app/src/main/assets/
   ```

---

### 🌐 Option 3: Download a Pre-trained Model

1. **Find a pre-trained TFLite model** for URL/text classification:
   - [TensorFlow Hub](https://tfhub.dev/)
   - [Hugging Face](https://huggingface.co/models)
   - [Kaggle](https://www.kaggle.com/models)

2. **Ensure the model has the correct architecture:**
   - **Input:** 10 float values (features from `FeatureExtractor.kt`)
   - **Output:** 6 float values (probabilities for each class)

3. **Place the model** in `app/src/main/assets/qubeguard_model.tflite`

4. **Or host it online** and update `Constants.MODEL_URL`:
   ```kotlin
   const val MODEL_URL = "https://your-server.com/models/qubeguard_model.tflite"
   ```

---

### 📊 Model Architecture

The model expects:
- **Input:** Array of 10 floats (features extracted from URL/domain)
- **Output:** Array of 6 floats (probabilities for each class)

#### Feature Extraction (from `FeatureExtractor.kt`)
| # | Feature | Description |
|---|---------|-------------|
| 1 | URL Length | Normalized URL length |
| 2 | Subdomain Depth | Number of dots in domain |
| 3 | Shannon Entropy | Measure of randomness in domain |
| 4 | Has Numeric | Whether domain contains numbers |
| 5 | Has Hyphen | Whether domain contains hyphens |
| 6 | TLD Rarity | 1 if rare TLD, 0 if common |
| 7 | Has IP Address | Whether domain is an IP address |
| 8 | Has Shortened URL | Whether URL is from a URL shortener |
| 9 | Has Suspicious Keywords | Whether domain contains suspicious words |
| 10 | Uses HTTP | 1 if HTTP, 0 if HTTPS |

#### Output Classes
1. **Legitimate** - Safe, non-malicious content
2. **Ad** - Advertisement-related content
3. **Tracker** - Tracking scripts/pixels
4. **Malware** - Malicious software sites
5. **Phishing** - Phishing/fraudulent sites
6. **Analytics** - Analytics/telemetry services

---

### 🔧 Model Configuration

Update thresholds in `PolicyEngine.kt`:

```kotlin
private val mlThresholds = mapOf(
    "Ad" to 0.7f,        // Block if confidence > 70%
    "Tracker" to 0.7f,   // Block if confidence > 70%
    "Malware" to 0.85f,  // Block if confidence > 85%
    "Phishing" to 0.8f,   // Block if confidence > 80%
    "Analytics" to 0.75f // Block if confidence > 75%
)
```

---

## 🏗️ Architecture

### **Project Structure**
```
qubeguard/
├── settings.gradle.kts                 # Project-level Gradle settings
├── build.gradle.kts                    # Top-level build configuration
├── app/
│   ├── build.gradle.kts                # App module dependencies
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml      # App permissions & services
│   │   │   ├── java/com/qubeguard/app/
│   │   │   │   ├── QubeGuardApp.kt       # Main Application class
│   │   │   │   ├── data/                # Data layer (Room, Blocklists)
│   │   │   │   │   ├── blocklist/        # Blocklist entities & DAOs
│   │   │   │   │   └── ...
│   │   │   │   ├── ml/                  # ML layer (TFLite, Feature Extractor)
│   │   │   │   │   ├── FeatureExtractor.kt
│   │   │   │   │   ├── TfLiteClassifier.kt
│   │   │   │   │   └── ...
│   │   │   │   ├── vpn/                 # VPN layer (DNS Proxy)
│   │   │   │   │   ├── VpnServiceImplementation.kt
│   │   │   │   │   └── DnsProxy.kt
│   │   │   │   ├── browser/             # Browser layer (WebView, Qubes)
│   │   │   │   │   ├── SecureWebView.kt
│   │   │   │   │   ├── QubeProfile.kt
│   │   │   │   │   └── ...
│   │   │   │   ├── policy/              # Policy layer (Feedback, Decisions)
│   │   │   │   │   ├── PolicyEngine.kt
│   │   │   │   │   └── ...
│   │   │   │   ├── engine/              # Core engine (Blocking, Service)
│   │   │   │   │   ├── BlockingEngine.kt
│   │   │   │   │   └── QubeGuardService.kt
│   │   │   │   ├── ui/                  # UI layer (Activities, Compose)
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── BrowserActivity.kt
│   │   │   │   │   └── ...
│   │   │   │   └── util/                # Utilities (Constants, Extensions)
│   │   │   │       ├── Constants.kt
│   │   │   │       └── ...
│   │   │   └── res/                    # Resources
│   │   │       └── assets/              # Model file goes here
│   │   │           └── qubeguard_model.tflite
│   │   └── test/                       # Unit tests
│   │       └── java/com/qubeguard/app/
│   │           ├── FeatureExtractorTest.kt
│   │           └── ...
│   └── ...
├── scripts/                            # Model generation scripts
│   ├── generate_model.py               # Generate dummy model
│   ├── train_model.py                  # Train custom model
│   └── README.md                       # Scripts documentation
└── README.md                           # Project documentation
```

### **Key Components**
| Component | Description | File |
|-----------|-------------|------|
| `BlockingEngine` | Combines Layer 1, 2, and 3 for final blocking decisions | `engine/BlockingEngine.kt` |
| `DeterministicBlocker` | Layer 1: Fast deterministic blocking using Radix Tree, Bloom Filter, and Regex | `data/blocklist/DeterministicBlocker.kt` |
| `VpnServiceImplementation` | Layer 2: VPN service for DNS interception | `vpn/VpnServiceImplementation.kt` |
| `TfLiteClassifier` | Layer 3: ML-based classification of URLs/domains | `ml/TfLiteClassifier.kt` |
| `SecureWebView` | Hardened WebView with privacy protections | `browser/SecureWebView.kt` |
| `QubeManager` | Manages isolated browser profiles (Qubes) | `browser/QubeManager.kt` |
| `PolicyEngine` | Makes final block/allow decisions | `policy/PolicyEngine.kt` |
| `FeedbackCollector` | Logs user feedback for improving the ML model | `policy/FeedbackCollector.kt` |

---

## 📡 Blocking Flow

```
User Request (e.g., https://ads.example.com/banner)
       ↓
┌─────────────────────────────────────┐
│ Layer 1: Deterministic Blocker        │
│ ┌─────────────────────────────────┐ │
│ │ 1. Check Allowlist                │ │
│ │    - If allowed → ALLOW           │ │
│ │ 2. Check Blocklist               │ │
│ │    - If blocked → BLOCK           │ │
│ │ 3. Check Radix Tree               │ │
│ │    - If matched → BLOCK           │ │
│ │ 4. Check Bloom Filter             │ │
│ │    - If not in filter → ALLOW     │ │
│ │ 5. Check Regex Engine             │ │
│ │    - If matched → BLOCK           │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
       ↓ (If not blocked by Layer 1)
┌─────────────────────────────────────┐
│ Layer 2: DNS/VPN Service             │
│ ┌─────────────────────────────────┐ │
│ │ 1. Intercept DNS (UDP port 53)   │ │
│ │ 2. Check Layer 1 rules            │ │
│ │    - If blocked → Return NXDOMAIN│ │
│ │ 3. Forward to upstream DNS        │ │
│ │    (Cloudflare 1.1.1.1)           │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
       ↓ (If not blocked by Layer 2)
┌─────────────────────────────────────┐
│ Layer 3: TFLite AI Classifier        │
│ ┌─────────────────────────────────┐ │
│ │ 1. Extract features from URL     │ │
│ │ 2. Run TFLite model              │ │
│ │ 3. Get confidence scores         │ │
│ │ 4. Apply thresholds              │ │
│ │    - Ad > 0.7 → BLOCK             │ │
│ │    - Tracker > 0.7 → BLOCK        │ │
│ │    - Malware > 0.85 → BLOCK      │ │
│ │    - Phishing > 0.8 → BLOCK      │ │
│ │    - Analytics > 0.75 → BLOCK    │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
       ↓
┌─────────────────────────────────────┐
│ Policy Engine                        │
│ ┌─────────────────────────────────┐ │
│ │ Combine results from all layers  │ │
│ │ Return final decision:           │ │
│ │ - BLOCK (with reason & layer)   │ │
│ │ - ALLOW                          │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
       ↓
User sees: BLOCKED or ALLOWED
```

---

## 🔧 Configuration

### **Blocklist Sources**
Modify `Constants.DEFAULT_BLOCKLIST_SOURCES` to add/remove default blocklists:

```kotlin
const val DEFAULT_BLOCKLIST_SOURCES = """
    [
        {
            "id": "easylist_ads",
            "name": "EasyList",
            "category": "ads",
            "url": "https://easylist.to/easylist/easylist.txt",
            "format": "adblock_plus",
            "license": "GPLv3",
            "update_interval_hours": 24
        },
        {
            "id": "easyprivacy",
            "name": "EasyPrivacy",
            "category": "privacy",
            "url": "https://easylist.to/easylist/easyprivacy.txt",
            "format": "adblock_plus",
            "license": "GPLv3",
            "update_interval_hours": 24
        }
    ]
"""
```

### **VPN Settings**
Modify `VpnServiceImplementation.kt` to change:
- **Upstream DNS server** (default: Cloudflare `1.1.1.1`)
- **DNS proxy port** (default: `5353`)

```kotlin
private val upstreamDnsServer = "1.1.1.1" // Cloudflare DNS
private val dnsProxyPort = 5353
```

### **Model URL**
Update `Constants.MODEL_URL` to point to your model:

```kotlin
const val MODEL_URL = "https://your-server.com/models/qubeguard_model.tflite"
```

Or place the model file in `app/src/main/assets/qubeguard_model.tflite`.

---

## 🧪 Testing

Run unit tests:
```bash
./gradlew test
```

### **Test Coverage**
| Class | Test File | Description |
|-------|-----------|-------------|
| `FeatureExtractor` | `FeatureExtractorTest.kt` | Tests feature extraction from URLs |
| `RadixTree` | `RadixTreeTest.kt` | Tests domain/subdomain matching |
| `BloomFilter` | `BloomFilterTest.kt` | Tests fast negative lookups |
| `PolicyEngine` | `PolicyEngineTest.kt` | Tests blocking decisions |
| `Extensions` | `ExtensionsTest.kt` | Tests Kotlin extensions |

---

## 📂 Dataset Preparation

To train a custom model, you need a dataset of labeled URLs. Here are some sources:

### **Public Datasets**
1. **PhishTank** - [https://www.phishtank.com/](https://www.phishtank.com/)
   - Phishing URL dataset with labels

2. **OpenPhish** - [https://openphish.com/](https://openphish.com/)
   - Real-time phishing URL feed

3. **Malware Domains** - [http://www.malwaredomains.com/](http://www.malwaredomains.com/)
   - List of malware-related domains

4. **Kaggle Datasets**
   - [Phishing URL Dataset](https://www.kaggle.com/datasets/sid321axn/malware-url-dataset)
   - [URL Classification Dataset](https://www.kaggle.com/datasets/)

### **Creating Your Dataset**

Create a CSV file with two columns:
```csv
url,label
https://example.com,Legitimate
https://ads.example.com,Ad
https://google-analytics.com,Analytics
https://malicious-site.com,Malware
https://fake-bank.com,Phishing
https://facebook-pixel.com,Tracker
```

**Label Options:**
- `Legitimate` - Safe, non-malicious
- `Ad` - Advertisement
- `Tracker` - Tracking script/pixel
- `Malware` - Malicious software
- `Phishing` - Phishing site
- `Analytics` - Analytics service

---

## 📜 License

This project is licensed under the **GPLv3 License**. See [LICENSE](LICENSE) for details.

---

## 🤝 Contributing

Contributions are welcome! Please:
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/your-feature`).
3. Commit your changes (`git commit -m "feat: add your feature"`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a **Pull Request**.

---

## 📬 Contact

For questions or feedback:
- Open an **Issue** on GitHub
- Contact the maintainer: [abdulraheemnohri](https://github.com/abdulraheemnohri)

---

## 🙏 Acknowledgments

- **EasyList** - [https://easylist.to/](https://easylist.to/)
- **AdGuard** - [https://adguard.com/](https://adguard.com/)
- **TensorFlow Lite** - [https://www.tensorflow.org/lite](https://www.tensorflow.org/lite)
- **Android VPN API** - [https://developer.android.com/reference/android/net/VpnService](https://developer.android.com/reference/android/net/VpnService)

---

**QubeGuard** – Your privacy, your control. 🛡️

---

## 📚 Additional Resources

- [TensorFlow Lite Model Maker](https://www.tensorflow.org/lite/guide/model_maker)
- [TensorFlow Hub](https://tfhub.dev/)
- [Android VPN Service Guide](https://developer.android.com/guide/topics/connectivity/vpn)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Dagger Hilt Guide](https://developer.android.com/training/dependency-injection/hilt-android)
