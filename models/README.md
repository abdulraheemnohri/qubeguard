# Models Directory

This directory is for hosting **TensorFlow Lite (TFLite) models** for QubeGuard.

## 📥 Adding a Model

### Option 1: Host on GitHub (Recommended for Small Models)
1. **Generate or train your model** using the scripts in `scripts/`:
   ```bash
   # Generate a dummy model
   python scripts/generate_model.py
   
   # Or train a custom model
   python scripts/train_model.py --data_path your_dataset.csv
   ```

2. **Copy the model** to this directory:
   ```bash
   cp app/src/main/assets/qubeguard_model.tflite models/
   ```

3. **Commit and push** the model:
   ```bash
   git add models/qubeguard_model.tflite
   git commit -m "feat: add trained TFLite model"
   git push origin main
   ```

4. **Update the model URL** in `Constants.kt`:
   ```kotlin
   const val MODEL_URL = "https://raw.githubusercontent.com/abdulraheemnohri/qubeguard/main/models/qubeguard_model.tflite"
   ```

---

### Option 2: Host on a CDN (Recommended for Large Models)
For models > 10MB, use a CDN or file hosting service:

1. **Upload your model** to a hosting service (e.g., AWS S3, Firebase Storage, GitHub Releases)
2. **Get the direct download URL**
3. **Update the model URL** in `Constants.kt`:
   ```kotlin
   const val MODEL_URL = "https://your-cdn.com/models/qubeguard_model.tflite"
   ```

---

## 📊 Model Requirements

The model must have the following architecture:

### Input
- **Shape:** `(1, 10)` or `(10,)`
- **Type:** `float32`
- **Description:** 10 features extracted from URLs (see `FeatureExtractor.kt`)

### Output
- **Shape:** `(1, 6)` or `(6,)`
- **Type:** `float32`
- **Description:** Probabilities for 6 classes (softmax output)

### Classes (in order)
1. Legitimate
2. Ad
3. Tracker
4. Malware
5. Phishing
6. Analytics

---

## 🎯 Model Training

See `scripts/README.md` for detailed instructions on:
- Generating a dummy model
- Training a custom model
- Model architecture details

---

## ⚠️ Important Notes

1. **GitHub File Size Limit:** GitHub has a **100MB** file size limit. For larger models:
   - Use Git LFS (Git Large File Storage)
   - Host on a CDN or external service
   - Compress the model

2. **Model Updates:** When you update the model:
   - Increment the version in `Constants.APP_VERSION`
   - Update the model URL if using remote hosting
   - Test thoroughly before releasing

3. **Model Security:** 
   - Verify model integrity (check hash)
   - Use HTTPS for model downloads
   - Consider signing models for production

---

## 📁 Current Models

| Model | Size | Description | Status |
|-------|------|-------------|--------|
| `qubeguard_model.tflite` | ~10-50KB | Dummy model (random weights) | ⚠️ For testing only |
| (Your model) | Varies | Custom trained model | ✅ Recommended |

---

## 🔗 Useful Links

- [TensorFlow Lite Model Maker](https://www.tensorflow.org/lite/guide/model_maker)
- [TensorFlow Hub](https://tfhub.dev/)
- [Git LFS Documentation](https://git-lfs.com/)
- [Model Optimization Guide](https://www.tensorflow.org/lite/performance/model_optimization)
