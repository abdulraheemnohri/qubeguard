# QubeGuard Model Scripts

This directory contains scripts for generating and training the TensorFlow Lite model used by QubeGuard.

## 📁 Scripts

### 1. `generate_model.py`
Creates a **dummy TFLite model** with the correct architecture for testing.

**Input:** 10 features (matches `FeatureExtractor.kt` output)
**Output:** 6 classes (Legitimate, Ad, Tracker, Malware, Phishing, Analytics)

**Requirements:**
- Python 3.7+
- `tensorflow>=2.12.0`
- `numpy`

**Usage:**
```bash
python scripts/generate_model.py
```

**Output:**
- Saves `qubeguard_model.tflite` to `app/src/main/assets/`

---

### 2. `train_model.py`
Trains a **custom TFLite model** using real URL data.

**Dataset Format (CSV):**
```csv
url,label
https://example.com,Legitimate
https://ads.example.com,Ad
https://tracker.example.com,Tracker
https://malware-site.com,Malware
https://phishing-site.com,Phishing
https://analytics.example.com,Analytics
```

**Supported Labels:**
- `Legitimate`
- `Ad`
- `Tracker`
- `Malware`
- `Phishing`
- `Analytics`

**Requirements:**
- Python 3.7+
- `tensorflow>=2.12.0`
- `numpy`
- `pandas`
- `scikit-learn`

**Usage:**
```bash
python scripts/train_model.py --data_path data/url_dataset.csv
```

**Output:**
- Saves `qubeguard_model.tflite` to `app/src/main/assets/`
- Model architecture: 10 input features → Dense(64) → Dropout → Dense(32) → Dropout → Dense(6, softmax)

---

## 🎯 Model Architecture

The model expects:
- **Input:** Array of 10 floats (features extracted from URL/domain)
- **Output:** Array of 6 floats (probabilities for each class)

### Feature Extraction (from `FeatureExtractor.kt`)
1. URL length (normalized)
2. Subdomain depth
3. Shannon entropy
4. Has numeric characters
5. Has hyphens
6. TLD rarity
7. Has IP address
8. Has shortened URL
9. Has suspicious keywords
10. Uses HTTP (not HTTPS)

### Output Classes
1. Legitimate
2. Ad
3. Tracker
4. Malware
5. Phishing
6. Analytics

---

## 📥 Getting a Model

### Option 1: Generate a Dummy Model (Quick Start)
```bash
# Install dependencies
pip install tensorflow numpy

# Generate model
python scripts/generate_model.py

# Copy to Android project
cp app/src/main/assets/qubeguard_model.tflite <your-android-project>/app/src/main/assets/
```

### Option 2: Train a Custom Model (Recommended)
1. **Prepare your dataset** in CSV format (see above)
2. **Train the model:**
   ```bash
   pip install tensorflow numpy pandas scikit-learn
   python scripts/train_model.py --data_path your_dataset.csv
   ```
3. **Copy the model** to your Android project

### Option 3: Use a Pre-trained Model
1. Download a pre-trained TFLite model (e.g., from TensorFlow Hub)
2. Ensure it has:
   - Input shape: `(1, 10)` or `(10,)`
   - Output shape: `(1, 6)` or `(6,)`
3. Place it in `app/src/main/assets/qubeguard_model.tflite`
4. Update `Constants.MODEL_URL` if hosting remotely

---

## 🔧 Model Configuration

Update `Constants.kt` to configure the model:

```kotlin
// Local model file name
const val MODEL_FILE_NAME = "qubeguard_model.tflite"

// Remote model URL (for automatic downloads)
const val MODEL_URL = "https://your-server.com/models/qubeguard_model.tflite"
```

---

## 📊 Thresholds

Adjust confidence thresholds in `PolicyEngine.kt`:

```kotlin
private val mlThresholds = mapOf(
    "Ad" to 0.7f,
    "Tracker" to 0.7f,
    "Malware" to 0.85f,
    "Phishing" to 0.8f,
    "Analytics" to 0.75f
)
```

---

## 🚀 Deployment

1. **Place the model** in `app/src/main/assets/qubeguard_model.tflite`
2. **Or** update `Constants.MODEL_URL` to download from a server
3. The app will:
   - Check for local model first
   - Download from URL if not found
   - Load and use the model for classification

---

## 📚 Resources

- [TensorFlow Lite Model Maker](https://www.tensorflow.org/lite/guide/model_maker)
- [TensorFlow Hub](https://tfhub.dev/)
- [Phishing URL Datasets](https://www.kaggle.com/datasets?search=phishing)
- [Malware URL Datasets](https://www.kaggle.com/datasets?search=malware)

---

## 🤝 Contributing

If you train a better model, consider:
1. Sharing the model file (if licensed appropriately)
2. Contributing to the training script
3. Opening a PR with improvements

---

**Note:** The dummy model from `generate_model.py` has random weights and will not provide accurate classifications. Use it only for testing the app structure. For production use, train a model with real data using `train_model.py`.
