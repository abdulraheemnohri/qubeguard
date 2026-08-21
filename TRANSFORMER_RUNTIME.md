# QubeGuard Local Transformer Runtime

Layer 3 is an **optional** local AI security feature. QubeGuard remains fully functional with Layer 3 disabled or when the model is unavailable.

## Source model

`r3ddkahili/final-complete-malicious-url-model`

The source model is a BERT sequence classifier with four classes:

- Benign
- Defacement
- Phishing
- Malware

The model is used only for malicious URL classification. Ads and trackers continue to be handled by deterministic blocklists/DNS rules.

## Mobile runtime

The Android application executes an Android-ready ONNX representation with `onnxruntime-android`. The original Hugging Face/PyTorch checkpoint is not executed directly on Android.

The mobile model artifact is expected to be supplied through the configured model source used by the application. QubeGuard does not publish or deploy model artifacts to a separate GitHub/Hugging Face repository.

## Optional AI behavior

If the user disables **Layer 3 / AI Protection**, QubeGuard uses Layer 1 and Layer 2 only.

If Layer 3 is enabled but the model is not downloaded, invalid, unavailable, or fails to load, QubeGuard automatically falls back to deterministic protection. The app must never fail closed merely because the optional AI model is unavailable.

## Automatic model updates

WorkManager may check for a newer configured model when the user enables automatic model updates. Model downloads are stored in private app storage and verified before activation.

No browsing URL is sent to a remote inference API.

## Local policy

The selected model does not classify ads or trackers. Its four outputs are used for malicious URL detection. Deterministic blocklists remain responsible for advertising/tracking categories.

Default AI thresholds:

- Defacement: `>= 0.80`
- Phishing: `>= 0.80`
- Malware: `>= 0.85`

All thresholds are user-configurable.

## GitHub policy

QubeGuard's GitHub repository contains source code and CI only. It does **not** deploy the model to GitHub or create a separate model repository. No `HUGGINGFACE_HUB_TOKEN` or model-publishing secret is required.
