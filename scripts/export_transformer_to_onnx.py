"""Export the selected Hugging Face malicious-URL Transformer for Android.

Source checkpoint:
    r3ddkahili/final-complete-malicious-url-model

The Android application uses ONNX Runtime. This script keeps the original
Transformer checkpoint as the source of truth and produces an optimized ONNX
artifact for on-device inference.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

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

    model = ORTModelForSequenceClassification.from_pretrained(
        SOURCE_MODEL,
        export=True,
    )
    tokenizer = AutoTokenizer.from_pretrained(SOURCE_MODEL)
    model.save_pretrained(OUTPUT_DIR)
    tokenizer.save_pretrained(OUTPUT_DIR)

    model_path = OUTPUT_DIR / "model.onnx"
    if not model_path.exists():
        raise FileNotFoundError(model_path)

    manifest = {
        "source_model": SOURCE_MODEL,
        "runtime": "onnxruntime-android",
        "architecture": "BertForSequenceClassification",
        "max_length": 128,
        "labels": ["Benign", "Defacement", "Phishing", "Malware"],
        "version": sha256(model_path),
        "sha256": sha256(model_path),
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
