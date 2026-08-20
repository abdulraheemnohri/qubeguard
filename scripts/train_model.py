#!/usr/bin/env python3
"""
Script to train a TensorFlow Lite model for QubeGuard using real data.

This script trains a model to classify URLs into 6 categories:
- Legitimate
- Ad
- Tracker
- Malware
- Phishing
- Analytics

The model uses features extracted from URLs (see FeatureExtractor.kt).

Requirements:
- Python 3.7+
- tensorflow>=2.12.0
- numpy
- pandas

Usage:
    python scripts/train_model.py --data_path data/url_dataset.csv

Dataset Format (CSV):
    url,label
    https://example.com,Legitimate
    https://ads.example.com,Ad
    https://tracker.example.com,Tracker
    ...
"""

import argparse
import os
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow.keras import layers, models, optimizers, callbacks
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

# Constants
MODEL_NAME = "qubeguard_model.tflite"
INPUT_SHAPE = (10,)  # Must match FeatureExtractor output
NUM_CLASSES = 6
CLASS_NAMES = ["Legitimate", "Ad", "Tracker", "Malware", "Phishing", "Analytics"]
ASSETS_DIR = "app/src/main/assets"
BATCH_SIZE = 32
EPOCHS = 50
TEST_SIZE = 0.2
RANDOM_SEED = 42


def extract_features(url: str) -> np.ndarray:
    """
    Extracts features from a URL to match FeatureExtractor.kt.
    This is a Python version of the Kotlin FeatureExtractor.
    """
    import re
    from math import log2
    
    # Extract domain
    domain = url.lower().strip()
    domain = re.sub(r'^https?://', '', domain)
    if '/' in domain:
        domain = domain.split('/')[0]
    if ':' in domain:
        domain = domain.split(':')[0]
    
    features = []
    
    # 1. URL length (normalized)
    url_length = len(url)
    features.append((url_length - 20) / 100)  # Normalize around 20-120 chars
    
    # 2. Subdomain depth
    subdomain_depth = domain.count('.')
    features.append(subdomain_depth / 5)  # Normalize by max depth of 5
    
    # 3. Shannon entropy
    if domain:
        char_counts = {}
        for char in domain:
            char_counts[char] = char_counts.get(char, 0) + 1
        entropy = 0.0
        for count in char_counts.values():
            prob = count / len(domain)
            entropy -= prob * log2(prob) if prob > 0 else 0
        features.append(entropy / 4)  # Normalize
    else:
        features.append(0.0)
    
    # 4. Has numeric
    features.append(1.0 if any(c.isdigit() for c in domain) else 0.0)
    
    # 5. Has hyphen
    features.append(1.0 if '-' in domain else 0.0)
    
    # 6. TLD rarity
    tld = domain.split('.')[-1] if '.' in domain else ""
    common_tlds = {"com", "org", "net", "io", "co", "uk", "us", "de", "fr", "jp"}
    features.append(0.0 if tld in common_tlds else 1.0)
    
    # 7. Has IP address
    ip_pattern = r'^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$'
    features.append(1.0 if re.match(ip_pattern, domain) else 0.0)
    
    # 8. Has shortened URL
    shortened_domains = {"bit.ly", "goo.gl", "tinyurl.com", "ow.ly", "t.co", "is.gd"}
    features.append(1.0 if any(sd in domain for sd in shortened_domains) else 0.0)
    
    # 9. Has suspicious keywords
    suspicious_keywords = {"ad", "track", "analytics", "click", "banner", "promo", "xyz", "top"}
    features.append(1.0 if any(kw in domain for kw in suspicious_keywords) else 0.0)
    
    # 10. Uses HTTP
    features.append(1.0 if url.startswith("http://") else 0.0)
    
    return np.array(features, dtype=np.float32)


def load_and_preprocess_data(data_path: str):
    """Loads and preprocesses the dataset."""
    print(f"📂 Loading data from {data_path}...")
    df = pd.read_csv(data_path)
    
    # Extract features
    print("🔍 Extracting features...")
    X = np.array([extract_features(url) for url in df['url']])
    
    # Encode labels
    print("🏷️  Encoding labels...")
    label_encoder = LabelEncoder()
    y = label_encoder.fit_transform(df['label'])
    
    # Validate class names
    assert set(label_encoder.classes_) == set(CLASS_NAMES), \
        f"Dataset labels {set(label_encoder.classes_)} don't match expected {set(CLASS_NAMES)}"
    
    # Split data
    print("✂️  Splitting data...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=TEST_SIZE, random_state=RANDOM_SEED, stratify=y
    )
    
    return X_train, X_test, y_train, y_test


def create_model():
    """Creates the neural network model."""
    model = models.Sequential([
        layers.InputLayer(input_shape=INPUT_SHAPE),
        layers.Dense(64, activation='relu'),
        layers.Dropout(0.3),
        layers.Dense(32, activation='relu'),
        layers.Dropout(0.2),
        layers.Dense(NUM_CLASSES, activation='softmax')
    ])
    
    model.compile(
        optimizer=optimizers.Adam(learning_rate=0.001),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    return model


def train_model(model, X_train, y_train, X_test, y_test):
    """Trains the model."""
    print("\n🎓 Training model...")
    
    # Callbacks
    early_stopping = callbacks.EarlyStopping(
        monitor='val_loss',
        patience=10,
        restore_best_weights=True
    )
    
    reduce_lr = callbacks.ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.2,
        patience=5,
        min_lr=1e-6
    )
    
    # Train
    history = model.fit(
        X_train, y_train,
        batch_size=BATCH_SIZE,
        epochs=EPOCHS,
        validation_data=(X_test, y_test),
        callbacks=[early_stopping, reduce_lr],
        verbose=1
    )
    
    # Evaluate
    print("\n📊 Evaluating model...")
    loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"Test Accuracy: {accuracy:.4f}")
    print(f"Test Loss: {loss:.4f}")
    
    return model


def save_model(model, output_path: str):
    """Saves the model as a TFLite file."""
    print(f"\n💾 Saving model to {output_path}...")
    
    # Create assets directory
    os.makedirs(ASSETS_DIR, exist_ok=True)
    
    # Convert to TFLite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    
    # Save
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"✅ Model saved!")
    print(f"   Size: {len(tflite_model) / 1024:.2f} KB")
    print(f"   Input shape: {INPUT_SHAPE}")
    print(f"   Output classes: {NUM_CLASSES}")


def main():
    parser = argparse.ArgumentParser(description='Train QubeGuard TFLite Model')
    parser.add_argument('--data_path', type=str, required=True, 
                        help='Path to the CSV dataset file')
    args = parser.parse_args()
    
    print("🚀 QubeGuard Model Training")
    print("=" * 50)
    
    # Load data
    X_train, X_test, y_train, y_test = load_and_preprocess_data(args.data_path)
    
    # Create model
    print("\n📊 Model Architecture:")
    model = create_model()
    model.summary()
    
    # Train
    model = train_model(model, X_train, y_train, X_test, y_test)
    
    # Save
    output_path = os.path.join(ASSETS_DIR, MODEL_NAME)
    save_model(model, output_path)
    
    print("\n" + "=" * 50)
    print("✨ Training complete!")
    print("\nNext steps:")
    print("1. Copy the model to your Android project:")
    print(f"   cp {output_path} <your-project>/app/src/main/assets/")
    print("2. Or host it online and update Constants.MODEL_URL")
    print("3. Build and run the app")


if __name__ == "__main__":
    main()
