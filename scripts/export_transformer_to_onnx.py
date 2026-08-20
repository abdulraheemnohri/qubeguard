"""Export the selected Hugging Face malicious-URL Transformer for Android.

Source checkpoint:
    r3ddkahili/final-complete-malicious-url-model

The Android application uses ONNX Runtime. The source Transformer is exported
and dynamically quantized to INT8 so the mobile runtime does not need the full
float32 checkpoint in memory.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

from onnxruntime.quantization import QuantType, quantize_dynamic
from optimum.onnxruntime import ORTModelForSequenceClassification
from transformers import AutoTokenizer

SOURCE_MODEL = "r3ddkahili/final-complete-malicious-url-model"
OUTPUT_DIR = Path("dist/qubeguard-transformer")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    float_dir = OUTPUT_DIR / "float32"
    float_dir.mkdir(parents=True, exist_ok=True)

    model = ORTModelForSequenceClassification.from_pretrained(
        SOURCE_MODEL,
        export=True,
    )
    tokenizer = AutoTokenizer.from_pretrained(SOURCE_MODEL)
    model.save_pretrained(float_dir)
    tokenizer.save_pretrained(float_dir)

    float_model = float_dir / "model.onnx"
    mobile_model = OUTPUT_DIR / "model.onnx"
    if not float_model.exists():
        raise FileNotFoundError(float_model)

    quantize_dynamic(
        str(float_model),
        str(mobile_model),
        weight_type=QuantType.QInt8,
        per_channel=True,
        reduce_range=True,
    )

    for filename in (
        "vocab.txt",
        "tokenizer.json",
        "tokenizer_config.json",
        "special_tokens_map.json",
        "config.json",
    ):
        source = float_dir / filename
        if source.exists():
            (OUTPUT_DIR / filename).write_bytes(source.read_bytes())

    manifest = {
        "source_model": SOURCE_MODEL,
        "runtime": "onnxruntime-android",
        "architecture": "BertForSequenceClassification",
        "quantization": "dynamic-int8",
        "max_length": 128,
        "labels": ["Benign", "Defacement", "Phishing", "Malware"],
        "version": sha256(mobile_model),
        "sha256": sha256(mobile_model),
        "model_file": "model.onnx",
        "vocab_file": "vocab.txt",
        "config_file": "config.json",
    }
    (OUTPUT_DIR / "manifest.json").write_text(
        json.dumps(manifest, indent=2) + "\n",
        encoding="utf-8",
    )

    print(json.dumps(manifest, indent=2))


if __name__ == "__main__":
    main()
