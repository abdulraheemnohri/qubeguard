#!/usr/bin/env python3
"""
Script to generate a simple TensorFlow Lite model for QubeGuard.

This script creates a dummy TFLite model with the correct architecture:
- Input: 10 features (float32) - matches FeatureExtractor output
- Output: 6 classes (Legitimate, Ad, Tracker, Malware, Phishing, Analytics)

The model is saved as 'qubeguard_model.tflite' in the app/src/main/assets/ directory.

Requirements:
- Python 3.7+
- tensorflow>=2.12.0
- numpy

Usage:
    python scripts/generate_model.py
"""

import os
import numpy as np
import tensorflow as tf
from tensorflow.keras import layers, models, optimizers

# Constants
MODEL_NAME = "qubeguard_model.tflite"
INPUT_SHAPE = (10,)  # 10 features from FeatureExtractor
NUM_CLASSES = 6  # Legitimate, Ad, Tracker, Malware, Phishing, Analytics
ASSETS_DIR = "app/src/main/assets"

# Class names (must match TfLiteClassifier.kt)
CLASS_NAMES = ["Legitimate", "Ad", "Tracker", "Malware", "Phishing", "Analytics"]


def create_model():
    """Creates a simple neural network model for URL classification."""
    model = models.Sequential([
        # Input layer - 10 features
        layers.InputLayer(input_shape=INPUT_SHAPE),
        
        # Hidden layers
        layers.Dense(32, activation='relu'),
        layers.Dropout(0.2),
        layers.Dense(16, activation='relu'),
        
        # Output layer - 6 classes with softmax
        layers.Dense(NUM_CLASSES, activation='softmax')
    ])
    
    # Compile the model
    model.compile(
        optimizer=optimizers.Adam(learning_rate=0.001),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    return model


def generate_dummy_data():
    """Generates dummy training data for demonstration."""
    # Generate random features (10 features per sample)
    num_samples = 1000
    X = np.random.rand(num_samples, *INPUT_SHAPE).astype(np.float32)
    
    # Generate random labels (0-5 for 6 classes)
    y = np.random.randint(0, NUM_CLASSES, size=(num_samples,))
    
    return X, y


def save_model(model, output_path):
    """Converts and saves the model as a TFLite file."""
    # Create assets directory if it doesn't exist
    os.makedirs(ASSETS_DIR, exist_ok=True)
    
    # Convert the model to TFLite format
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    
    # Save the model
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"✅ Model saved to: {output_path}")
    print(f"   Size: {len(tflite_model) / 1024:.2f} KB")


def main():
    print("🚀 Generating QubeGuard TFLite Model")
    print("=" * 50)
    
    # Create model
    print("📊 Creating model architecture...")
    model = create_model()
    model.summary()
    
    # Generate dummy data (optional, for testing)
    print("\n📈 Generating dummy training data...")
    X, y = generate_dummy_data()
    
    # Train briefly (optional, for demonstration)
    print("\n🎓 Training model (1 epoch)...")
    model.fit(X, y, epochs=1, batch_size=32, verbose=1)
    
    # Save model
    output_path = os.path.join(ASSETS_DIR, MODEL_NAME)
    print(f"\n💾 Saving model to {output_path}...")
    save_model(model, output_path)
    
    print("\n" + "=" * 50)
    print("✨ Model generation complete!")
    print("\nNext steps:")
    print("1. Copy the model to your Android project:")
    print(f"   cp {output_path} <your-project>/app/src/main/assets/")
    print("2. Or update Constants.MODEL_URL to point to your model URL")
    print("3. Build and run the app")


if __name__ == "__main__":
    main()
