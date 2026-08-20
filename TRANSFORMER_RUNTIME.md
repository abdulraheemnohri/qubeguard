# QubeGuard Local Transformer Runtime

QubeGuard Layer 3 now uses a local Transformer model and does not use TensorFlow Lite or remote Hugging Face inference.

## Source model

`r3ddkahili/final-complete-malicious-url-model`

The source model is a BERT sequence classifier with four classes:

- Benign
- Defacement
- Phishing
- Malware

The Hugging Face repository currently publishes a 438 MB `model.safetensors` checkpoint plus tokenizer/config files. It does not publish an Android-ready ONNX artifact, so QubeGuard does **not** attempt to execute the safetensors file directly on Android.

## Mobile runtime

The model is exported with Hugging Face Optimum to ONNX and dynamically quantized to INT8. The Android app runs that artifact with `onnxruntime-android`.

Pipeline:

```text
Hugging Face BERT checkpoint
        |
        v
Optimum ONNX export
        |
        v
ONNX dynamic INT8 quantization
        |
        v
QubeGuard Transformer model repository
        |
        v
Automatic Android download + SHA-256 verification
        |
        v
ONNX Runtime Android
        |
        v
Local URL classification
```

## Automatic model updates

`QubeGuardApp` schedules an initial network-constrained download and a daily update check. The runtime stores the model under private app storage and verifies the SHA-256 value from `manifest.json` before installation.

No browsing URL is sent to a remote inference API.

## Publishing the runtime artifact

The repository contains `.github/workflows/publish-transformer-model.yml`.

Create a GitHub Actions secret named `HUGGINGFACE_HUB_TOKEN` with permission to create/write model repositories. The workflow exports the source checkpoint and publishes the mobile artifact to:

`abdulraheemnohri/qubeguard-transformer-model`

The Android downloader expects that repository.

## Why ONNX instead of executing safetensors directly?

The selected checkpoint is a Transformers/PyTorch BERT checkpoint. Android needs an inference representation and runtime that can execute it efficiently. Hugging Face documents ONNX export for Transformers models, and ONNX Runtime provides an Android Java/Kotlin package. This keeps the **model architecture Transformer** while using a production mobile inference runtime.

## Blocking policy

The selected model does not classify ads or trackers. Its four outputs are used for malicious URL detection. Deterministic blocklists remain responsible for advertising/tracking categories.

Layer 3 blocks when confidence reaches the configured threshold:

- Defacement: `>= 0.80`
- Phishing: `>= 0.80`
- Malware: `>= 0.85`

Benign URLs are allowed by the ML layer.
