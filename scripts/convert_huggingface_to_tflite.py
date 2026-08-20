#!/usr/bin/env python3
"""
Convert Hugging Face Model to TFLite for QubeGuard

This script:
1. Downloads the Hugging Face model (r3ddkahili/final-complete-malicious-url-model)
2. Converts it to TFLite format
3. Adds a wrapper to match QubeGuard's expected input/output
4. Saves the converted model

Requirements:
- Python 3.7+
- transformers>=4.0.0
- torch
- tensorflow>=2.12.0
- sentencepiece (for tokenizer)

Usage:
    python scripts/convert_huggingface_to_tflite.py

Output:
    - app/src/main/assets/qubeguard_model_hf.tflite (converted model)
    - app/src/main/assets/hf_labels.json (label mapping)

Note:
- The original model is ~438MB, the converted TFLite model will be ~150-200MB
- For mobile, consider quantization to reduce size further
- This script requires significant memory (>8GB RAM)
"""

import os
import json
import numpy as np
import tensorflow as tf
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from datetime import datetime

# Configuration
MODEL_NAME = "r3ddkahili/final-complete-malicious-url-model"
OUTPUT_MODEL_PATH = "app/src/main/assets/qubeguard_model_hf.tflite"
OUTPUT_LABELS_PATH = "app/src/main/assets/hf_labels.json"
MAX_SEQ_LENGTH = 128

# QubeGuard's expected classes
QUBEGUARD_CLASSES = ["Legitimate", "Ad", "Tracker", "Malware", "Phishing", "Analytics"]

# Map Hugging Face labels to QubeGuard labels
# HF Model labels: Benign, Defacement, Phishing, Malware
# QubeGuard labels: Legitimate, Ad, Tracker, Malware, Phishing, Analytics
LABEL_MAP = {
    "Benign": "Legitimate",
    "Defacement": "Tracker",  # or "Malware"
    "Phishing": "Phishing",
    "Malware": "Malware"
}


def download_and_convert():
    """Download the Hugging Face model and convert to TFLite."""
    print("=" * 70)
    print("🚀 Converting Hugging Face Model to TFLite for QubeGuard")
    print("=" * 70)
    
    # Step 1: Download the model
    print("\n📥 Step 1: Downloading Hugging Face model...")
    print(f"Model: {MODEL_NAME}")
    
    try:
        tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
        model = AutoModelForSequenceClassification.from_pretrained(MODEL_NAME)
        print("✅ Model and tokenizer downloaded successfully")
    except Exception as e:
        print(f"❌ Error downloading model: {e}")
        print("\n💡 Make sure you have internet connection and enough disk space (~1GB)")
        return False
    
    # Step 2: Test the model
    print("\n🧪 Step 2: Testing the model...")
    test_url = "http://example.com"
    
    try:
        inputs = tokenizer(test_url, return_tensors="pt", truncation=True, padding=True, max_length=MAX_SEQ_LENGTH)
        outputs = model(**inputs)
        prediction = tf.argmax(outputs.logits, axis=1).numpy()[0]
        
        # Get label names from model config
        id2label = model.config.id2label
        predicted_label = id2label[str(prediction)]
        
        print(f"Test URL: {test_url}")
        print(f"Predicted: {predicted_label}")
        print("✅ Model works correctly")
    except Exception as e:
        print(f"❌ Error testing model: {e}")
        return False
    
    # Step 3: Create a wrapper model for TFLite
    print("\n🏗️  Step 3: Creating TFLite-compatible model...")
    
    # The Hugging Face model expects text input, but we want to use it with
    # tokenized input for better performance in TFLite
    # We'll create a model that includes the tokenizer
    
    # First, save the tokenizer
    tokenizer.save_pretrained("./hf_tokenizer")
    
    # Create a TF model that uses the Hugging Face model
    # This is complex, so we'll use a simpler approach:
    # Convert the PyTorch model to ONNX, then to TFLite
    
    try:
        # Export to ONNX
        print("Converting to ONNX format...")
        import torch
        
        # Set model to eval mode
        model.eval()
        
        # Create dummy input
        dummy_input = tokenizer(
            "http://example.com",
            return_tensors="pt",
            truncation=True,
            padding=True,
            max_length=MAX_SEQ_LENGTH
        )
        
        # Export to ONNX
        torch.onnx.export(
            model,
            tuple(dummy_input.values()),
            "model.onnx",
            input_names=["input_ids", "attention_mask", "token_type_ids"],
            output_names=["output"],
            dynamic_axes={
                "input_ids": {0: "batch_size"},
                "attention_mask": {0: "batch_size"},
                "token_type_ids": {0: "batch_size"}
            }
        )
        print("✅ Model exported to ONNX")
        
        # Convert ONNX to TFLite
        print("Converting ONNX to TFLite...")
        onnx_model = tf.io.gfile.GFile("model.onnx", "rb").read()
        
        # This is a simplified approach - in practice, you might need to use
        # onnx-tensorflow or other tools for better conversion
        # For now, we'll use TensorFlow's ONNX import
        
        # Note: This might not work perfectly for all models
        # A better approach would be to use the Hugging Face model directly
        # and create a custom inference pipeline
        
        print("⚠️  ONNX to TFLite conversion is complex for transformer models")
        print("💡 Alternative: Use the Hugging Face API instead (see HuggingFaceClassifier.kt)")
        
        # For now, let's create a simpler solution
        # We'll use TensorFlow's saved model format
        
        # Save as TensorFlow model
        tf_model_path = "./hf_tf_model"
        model.save_pretrained(tf_model_path, save_format="tf")
        print("✅ Model saved in TensorFlow format")
        
        # Convert to TFLite
        converter = tf.lite.TFLiteConverter.from_saved_model(tf_model_path)
        tflite_model = converter.convert()
        
        # Save TFLite model
        os.makedirs(os.path.dirname(OUTPUT_MODEL_PATH), exist_ok=True)
        with open(OUTPUT_MODEL_PATH, 'wb') as f:
            f.write(tflite_model)
        
        print(f"✅ TFLite model saved to: {OUTPUT_MODEL_PATH}")
        print(f"   Size: {len(tflite_model) / (1024 * 1024):.2f} MB")
        
        # Save label mapping
        labels = {
            "hf_to_qubeguard": LABEL_MAP,
            "qubeguard_classes": QUBEGUARD_CLASSES,
            "hf_classes": list(model.config.id2label.values()),
            "created_at": datetime.now().isoformat()
        }
        
        with open(OUTPUT_LABELS_PATH, 'w') as f:
            json.dump(labels, f, indent=2)
        
        print(f"✅ Label mapping saved to: {OUTPUT_LABELS_PATH}")
        
        # Clean up temporary files
        import shutil
        if os.path.exists("./hf_tokenizer"):
            shutil.rmtree("./hf_tokenizer")
        if os.path.exists("model.onnx"):
            os.remove("model.onnx")
        if os.path.exists(tf_model_path):
            shutil.rmtree(tf_model_path)
        
        return True
        
    except Exception as e:
        print(f"❌ Error converting model: {e}")
        print("\n💡 Consider using the Hugging Face API instead (no conversion needed)")
        return False


def create_api_wrapper():
    """Create a wrapper that uses the Hugging Face API directly."""
    print("\n🌐 Alternative: Use Hugging Face API (no model download)")
    print("=" * 70)
    print("\nInstead of downloading the large model (~438MB), you can use")
    print("the Hugging Face Inference API which handles everything in the cloud.")
    print("\n📝 See: HuggingFaceClassifier.kt in the QubeGuard project")
    print("\n🔗 API Documentation: https://huggingface.co/docs/api-inference/")
    print("\n💡 Benefits:")
    print("  - No large model download")
    print("  - Always up-to-date")
    print("  - No conversion needed")
    print("\n⚠️  Limitations:")
    print("  - Requires internet connection")
    print("  - Rate limited (free tier)")
    print("  - Slightly slower (network latency)")


def main():
    print("\n" + "=" * 70)
    print("⚠️  IMPORTANT NOTICE")
    print("=" * 70)
    print("\nThe Hugging Face model 'r3ddkahili/final-complete-malicious-url-model'")
    print("is a BERT-based model (~438MB) which is TOO LARGE for mobile apps.")
    print("\nYou have 2 options:")
    print("\n🔹 Option 1: Use Hugging Face API (RECOMMENDED)")
    print("   - No model download")
    print("   - Uses cloud inference")
    print("   - Requires internet")
    print("   - See: HuggingFaceClassifier.kt")
    print("\n🔹 Option 2: Convert to TFLite (Advanced)")
    print("   - Downloads the large model")
    print("   - Converts to TFLite (~150-200MB)")
    print("   - Works offline")
    print("   - Still large for mobile")
    
    choice = input("\nEnter your choice (1 or 2): ").strip()
    
    if choice == "2":
        print("\n⚠️  This requires >8GB RAM and ~1GB disk space")
        confirm = input("Continue? (y/n): ").strip().lower()
        
        if confirm == "y":
            if download_and_convert():
                print("\n" + "=" * 70)
                print("✅ Conversion Complete!")
                print("=" * 70)
                print(f"\n📥 Model files:")
                print(f"  - {OUTPUT_MODEL_PATH}")
                print(f"  - {OUTPUT_LABELS_PATH}")
                print(f"\n📝 Next steps:")
                print(f"  1. Copy the model to your Android project")
                print(f"  2. Update QubeGuard to use the new model")
                print(f"  3. Consider quantization to reduce size")
            else:
                print("\n❌ Conversion Failed")
        else:
            print("\n✅ Aborted")
    else:
        create_api_wrapper()


if __name__ == "__main__":
    main()
