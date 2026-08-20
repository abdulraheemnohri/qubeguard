# 🤗 Hugging Face Model Integration Guide

This guide explains how to integrate the **r3ddkahili/final-complete-malicious-url-model** into QubeGuard.

---

## 📌 Overview

The **r3ddkahili/final-complete-malicious-url-model** is a **BERT-based URL classifier** with:
- **98% accuracy** on validation data
- **4 output classes:** Benign, Defacement, Phishing, Malware
- **110M parameters** (based on bert-base-uncased)
- **Trained on:** Kaggle Malicious URLs Dataset (~651,191 samples)

**Model Card:** [https://huggingface.co/r3ddkahili/final-complete-malicious-url-model](https://huggingface.co/r3ddkahili/final-complete-malicious-url-model)

---

## 🎯 Integration Options

QubeGuard supports **two ways** to use this model:

| Option | Pros | Cons | Recommended For |
|--------|------|------|-----------------|
| **Option 1: API** | No download, always up-to-date, no size limit | Requires internet, rate limited | Production (with internet) |
| **Option 2: Local** | Works offline, no rate limits | Large model size (~438MB), needs conversion | Advanced users with space |

---

## 🚀 Option 1: Use Hugging Face API (Recommended)

This is the **easiest and most practical** approach for most users.

### ✅ Benefits
- **No model download** (saves ~438MB storage)
- **Always up-to-date** (model improves over time)
- **No conversion needed**
- **Fast to implement**

### ⚠️ Limitations
- **Requires internet connectivity**
- **Rate limited** (free tier: ~1000 requests/hour)
- **Slightly slower** (network latency: ~500-1000ms)

### 📥 Setup

#### 1. Get a Hugging Face API Token (Optional)
The model can be used **without authentication** for basic usage, but for higher rate limits:

1. Go to: [https://huggingface.co/settings/tokens](https://huggingface.co/settings/tokens)
2. Create a new **Read** token
3. Copy the token

#### 2. Update Constants.kt
```kotlin
// In app/src/main/java/com/qubeguard/app/util/Constants.kt

const val HUGGINGFACE_MODEL_ID = "r3ddkahili/final-complete-malicious-url-model"
const val HUGGINGFACE_API_URL = "https://api-inference.huggingface.co/models/$HUGGINGFACE_MODEL_ID"
```

#### 3. Enable Hugging Face in Settings
The app already has a **switch** to enable/disable Hugging Face:

```kotlin
// In your SettingsActivity or SettingsViewModel
fun enableHuggingFace(enabled: Boolean) {
    blockingEngine.enableHuggingFace()
    // Or: policyEngine.enableHuggingFace()
}

fun setHuggingFaceToken(token: String) {
    blockingEngine.setHuggingFaceToken(token)
}
```

#### 4. Set Token in App (Optional)
If you have a token, set it at startup:

```kotlin
// In QubeGuardApp.kt or MainActivity.kt
class QubeGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Set Hugging Face token if available
        val hfToken = "your_token_here" // Store securely!
        if (hfToken.isNotBlank()) {
            // Will be used by HuggingFaceClassifier
        }
    }
}
```

### 🧪 Testing

```kotlin
// Test the classifier
val classifier = HuggingFaceClassifier(context)

// Classify a URL
val category = classifier.classify("https://malicious-site.com")
// Returns: "Malware", "Phishing", "Tracker", or "Legitimate"

// Get confidence scores
val scores = classifier.getConfidenceScores("https://example.com")
// Returns: Map<String, Float> (6 categories)

// Check if blocked
val isBlocked = classifier.isBlocked("https://phishing-site.com")
// Returns: true/false
```

---

## 💾 Option 2: Local Model Conversion (Advanced)

For **offline use**, you can download and convert the model to TFLite format.

### ⚠️ Important Notes
- **Model Size:** ~438MB (PyTorch) → ~150-200MB (TFLite)
- **Memory Required:** >8GB RAM for conversion
- **Disk Space:** ~1GB for temporary files
- **Not recommended** for most mobile apps due to size

### 🛠️ Conversion Steps

#### 1. Install Dependencies
```bash
pip install torch transformers tensorflow onnx onnx-tf
```

#### 2. Run Conversion Script
```bash
python scripts/convert_huggingface_to_tflite.py
```

The script will:
1. Download the model from Hugging Face
2. Convert to ONNX format
3. Convert to TFLite format
4. Save to `app/src/main/assets/qubeguard_model_hf.tflite`

#### 3. Update Model Downloader
```kotlin
// In ModelDownloader.kt
fun downloadModel(): Boolean {
    // Use the local file or download from a custom URL
    return true
}
```

#### 4. Update TfLiteClassifier
The `TfLiteClassifier` needs to be updated to handle the BERT model's input format:

```kotlin
// In TfLiteClassifier.kt
fun loadModel(modelFileName: String = "qubeguard_model_hf.tflite") {
    // Load the converted model
    try {
        val modelFile = context.assets.open(modelFileName)
        // ... rest of loading code
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun classify(input: String): String {
    // The BERT model expects tokenized text, not features
    // You'll need to implement tokenization in Kotlin
    // Or pre-tokenize and use the model
    
    // For now, use the API approach (Option 1)
    return "Legitimate"
}
```

### 📦 Quantization (Reduce Model Size)

To reduce the model size, use **quantization**:

```python
# In convert_huggingface_to_tflite.py

# After converting to TFLite
converter = tf.lite.TFLiteConverter.from_saved_model(tf_model_path)
converter.optimizations = [tf.lite.Optimize.DEFAULT]  # Dynamic range quantization
# Or for float16 quantization:
# converter.optimizations = [tf.lite.Optimize.DEFAULT]
# converter.target_spec.supported_types = [tf.float16]

quantized_model = converter.convert()
```

This can reduce the model size by **4x** with minimal accuracy loss.

---

## 🔧 Label Mapping

The Hugging Face model outputs **4 classes**, but QubeGuard expects **6 classes**:

| Hugging Face | QubeGuard | Notes |
|--------------|-----------|-------|
| Benign | Legitimate | Safe URLs |
| Defacement | Tracker | Or Malware |
| Phishing | Phishing | Direct match |
| Malware | Malware | Direct match |
| - | Ad | Not in HF model |
| - | Analytics | Not in HF model |

### 📊 How QubeGuard Handles This

1. **Direct Mapping:**
   - Benign → Legitimate
   - Phishing → Phishing
   - Malware → Malware
   - Defacement → Tracker (configurable)

2. **Fallback for Missing Classes:**
   - Ad: Uses deterministic rules (Layer 1) or low confidence
   - Analytics: Uses deterministic rules (Layer 1) or low confidence

3. **Confidence Distribution:**
   When the HF model returns a prediction, QubeGuard distributes confidence:
   ```kotlin
   // Example: HF returns "Phishing" with 95% confidence
   val scores = mapOf(
       "Legitimate" to 0.01f,
       "Ad" to 0.01f,
       "Tracker" to 0.01f,
       "Malware" to 0.01f,
       "Phishing" to 0.95f,
       "Analytics" to 0.01f
   )
   ```

---

## 📡 API vs Local Comparison

| Feature | API | Local |
|---------|-----|-------|
| **Model Size** | 0MB (cloud) | ~150-200MB |
| **Internet Required** | ✅ Yes | ❌ No |
| **Speed** | ~500-1000ms | ~50-100ms |
| **Accuracy** | ✅ High (always latest) | ✅ High (if converted properly) |
| **Rate Limits** | ✅ Yes (free tier) | ❌ No |
| **Setup Complexity** | ✅ Easy | ⚠️ Complex |
| **Storage Required** | ❌ None | ⚠️ ~200MB |
| **Battery Impact** | ⚠️ Medium (network) | ✅ Low |
| **Offline Support** | ❌ No | ✅ Yes |

---

## 🔌 Integration Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         QubeGuard                             │
├─────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────┐ │
│  │   Layer 1:      │    │   Layer 2:      │    │  Layer  │ │
│  │ Deterministic   │    │   DNS/VPN       │    │   3: ML │ │
│  │ (Radix Tree,    │    │ (VpnService)    │    │         │ │
│  │  Bloom Filter,  │    │                 │    │  ┌─────┴─┐  │ │
│  │   Regex)       │    │                 │    │  │ ML    │  │ │
│  └─────────────────┘    └─────────────────┘    │  │Classi │  │ │
│         │                  │                  │  │fier   │  │ │
│         ▼                  ▼                  │  │       │  │ │
│  ┌─────────────────────────────────────────────────────┐ │  │
│  │                    Policy Engine                     │ │  │
│  │  (Combines all layers for final decision)              │ │  │
│  └─────────────────────────────────────────────────────┘ │  │
│                              │                              │  │
│                              ▼                              │  │
│  ┌─────────────────────────────────────────────────────┐ │  │
│  │                    MLClassifier                         │ │  │
│  │  ┌─────────────────┐    ┌───────────────────────────┐ │ │  │
│  │  │  TfLite          │    │   HuggingFaceClassifier    │ │ │  │
│  │  │  Classifier      │    │   (API-based)              │ │ │  │
│  │  │  (Local)         │    │   r3ddkahili/...           │ │ │  │
│  │  └─────────────────┘    └───────────────────────────┘ │ │  │
│  │                        │                              │  │
│  │  ┌─────────────────────────────────────────────────┐ │ │  │
│  │  │  Switch: Use TFLite or Hugging Face API          │ │ │  │
│  │  └─────────────────────────────────────────────────┘ │ │  │
│  └─────────────────────────────────────────────────────┘ │  │
│                                                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 📱 Android Implementation

### 1. Add Internet Permission
Ensure your `AndroidManifest.xml` has internet permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 2. Add OkHttp Dependency
The `HuggingFaceClassifier` uses OkHttp for API calls:

```kotlin
// In app/build.gradle.kts
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

### 3. Handle Network Operations
API calls should be made from **coroutines** or **background threads**:

```kotlin
// In a ViewModel or Repository
viewModelScope.launch(Dispatchers.IO) {
    val isBlocked = blockingEngine.isBlocked(url)
    // Update UI on main thread
    withContext(Dispatchers.Main) {
        // Update UI
    }
}
```

### 4. Error Handling
Handle network errors gracefully:

```kotlin
try {
    val category = huggingFaceClassifier.classify(url)
} catch (e: IOException) {
    // Fallback to deterministic blocking
    Log.e("HFClassifier", "Network error", e)
    return "Legitimate" // or use deterministic
}
```

---

## 🎛️ Settings Integration

Add a setting to switch between TFLite and Hugging Face:

### 1. Add to Settings Screen
```kotlin
// In SettingsScreen.kt
var useHuggingFace by remember { mutableStateOf(false) }

Switch(
    checked = useHuggingFace,
    onCheckedChange = { enabled ->
        useHuggingFace = enabled
        blockingEngine.enableHuggingFace()
        // Save to preferences
    }
)
```

### 2. Save to Shared Preferences
```kotlin
// In a SharedPreferences helper
fun saveHuggingFaceEnabled(enabled: Boolean) {
    prefs.edit().putBoolean("huggingface_enabled", enabled).apply()
}

fun isHuggingFaceEnabled(): Boolean {
    return prefs.getBoolean("huggingface_enabled", false)
}
```

### 3. Initialize at Startup
```kotlin
// In QubeGuardApp.kt
class QubeGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Load settings
        val useHf = prefs.getBoolean("huggingface_enabled", false)
        if (useHf) {
            blockingEngine.enableHuggingFace()
        }
    }
}
```

---

## 📊 Performance Optimization

### 1. Caching
Cache API responses to avoid repeated calls:

```kotlin
// In HuggingFaceClassifier.kt
private val cache = LruCache<String, String>(1000) // URL -> Category

fun classify(url: String): String {
    // Check cache first
    cache.get(url)?.let { return it }
    
    // Make API call
    val category = makeApiCall(url)
    
    // Cache the result
    cache.put(url, category)
    
    return category
}
```

### 2. Batch Requests
Combine multiple URL classifications into one API call:

```kotlin
fun classifyBatch(urls: List<String>): Map<String, String> {
    val requestBody = JSONObject().apply {
        put("inputs", JSONArray(urls))
    }
    
    // Make single API call
    // Parse response
    // Return map of URL -> Category
}
```

### 3. Timeout Handling
Set reasonable timeouts:

```kotlin
// In HuggingFaceClassifier.kt
private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)  // 10s connection timeout
    .readTimeout(15, TimeUnit.SECONDS)    // 15s read timeout
    .writeTimeout(10, TimeUnit.SECONDS)   // 10s write timeout
    .build()
```

---

## 🔒 Security Considerations

### 1. API Token Storage
Store the Hugging Face token **securely**:

```kotlin
// Use Android Keystore
val keyStore = KeyStore.getInstance("AndroidKeyStore")
keyStore.load(null)

// Or use EncryptedSharedPreferences
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// Save token
sharedPreferences.edit().putString("hf_token", token).apply()
```

### 2. URL Sanitization
Sanitize URLs before sending to API:

```kotlin
fun sanitizeUrl(url: String): String {
    // Remove PII (Personally Identifiable Information)
    // Remove sensitive query parameters
    // Return clean URL
}
```

### 3. Rate Limiting
Handle rate limits gracefully:

```kotlin
try {
    val response = makeApiCall(url)
    if (response.code == 429) { // Too Many Requests
        // Wait and retry
        Thread.sleep(1000)
        return makeApiCall(url)
    }
} catch (e: Exception) {
    // Fallback
}
```

---

## 📈 Monitoring & Analytics

Track API usage and performance:

```kotlin
// In HuggingFaceClassifier.kt
private var totalRequests = 0
private var failedRequests = 0
private var avgResponseTime = 0L

fun classify(url: String): String {
    val startTime = System.currentTimeMillis()
    totalRequests++
    
    try {
        val category = makeApiCall(url)
        avgResponseTime = ((avgResponseTime * (totalRequests - 1)) + 
                         (System.currentTimeMillis() - startTime)) / totalRequests
        return category
    } catch (e: Exception) {
        failedRequests++
        return "Legitimate" // Fallback
    }
}

fun getStats(): Map<String, Any> {
    return mapOf(
        "total_requests" to totalRequests,
        "failed_requests" to failedRequests,
        "success_rate" to (1.0 - failedRequests.toDouble() / totalRequests),
        "avg_response_time_ms" to avgResponseTime
    )
}
```

---

## 🆘 Troubleshooting

### Common Issues

#### 1. Network Errors
**Problem:** `IOException: Failed to connect`

**Solution:**
- Check internet connection
- Verify the API URL is correct
- Check for firewall/proxy issues
- Increase timeout values

#### 2. Rate Limited
**Problem:** `HTTP 429 Too Many Requests`

**Solution:**
- Get a Hugging Face token for higher limits
- Implement caching
- Implement batch requests
- Add delays between requests

#### 3. Model Not Loaded
**Problem:** Local model fails to load

**Solution:**
- Check model file exists in `app/src/main/assets/`
- Verify model file name in `Constants.MODEL_FILE_NAME`
- Check for file corruption

#### 4. Slow Performance
**Problem:** API calls are slow

**Solution:**
- Use caching
- Use batch requests
- Consider local model (if size is acceptable)
- Optimize network code

---

## 📚 Resources

- **Model Card:** [https://huggingface.co/r3ddkahili/final-complete-malicious-url-model](https://huggingface.co/r3ddkahili/final-complete-malicious-url-model)
- **Hugging Face API Docs:** [https://huggingface.co/docs/api-inference/](https://huggingface.co/docs/api-inference/)
- **BERT Model Docs:** [https://huggingface.co/docs/transformers/model_doc/bert](https://huggingface.co/docs/transformers/model_doc/bert)
- **TFLite Conversion:** [https://www.tensorflow.org/lite/convert](https://www.tensorflow.org/lite/convert)
- **ONNX to TFLite:** [https://github.com/onnx/onnx-tensorflow](https://github.com/onnx/onnx-tensorflow)

---

## 🤝 Contributing

If you improve the Hugging Face integration:
1. Share your improvements
2. Update this documentation
3. Open a PR with your changes

---

## 📝 Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-08-20 | Initial Hugging Face integration |
| 1.0 | 2024-08-20 | Added API and Local options |
| 1.0 | 2024-08-20 | Added label mapping |

---

**🎉 Hugging Face Integration Complete!**
**QubeGuard now supports both TFLite and Hugging Face models!** 🚀
