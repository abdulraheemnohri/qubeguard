#!/usr/bin/env python3
"""
QubeGuard TFLite Model Training Script

This script trains a TensorFlow Lite model for QubeGuard's ML-based blocking engine (Layer 3).
It automatically downloads free public datasets, extracts features, trains a model,
and exports it as a TFLite file ready for Android.

Features:
- Downloads URLs from PhishTank, OpenPhish, Malware Domains, EasyList, EasyPrivacy
- Generates legitimate URLs from top websites
- Extracts 10 features from each URL (matching FeatureExtractor.kt)
- Trains a neural network model
- Exports as TFLite for Android

Requirements:
- Python 3.7+
- tensorflow>=2.12.0
- numpy
- pandas
- scikit-learn
- requests
- beautifulsoup4

Usage:
    python scripts/train_qubeguard_model.py

Output:
    - app/src/main/assets/qubeguard_model.tflite (TFLite model)
    - app/src/main/assets/qubeguard_labels.json (Class labels)
    - data/training_dataset.csv (Generated dataset)
"""

import os
import re
import csv
import json
import random
import requests
import numpy as np
import pandas as pd
from datetime import datetime
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report
import tensorflow as tf
from tensorflow.keras import layers, models, optimizers, callbacks

# Constants
RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)
random.seed(RANDOM_SEED)
tf.random.set_seed(RANDOM_SEED)

# Configuration
DATA_LIMIT = {
    'phishing': 5000,
    'malware': 5000,
    'ad': 5000,
    'analytics': 5000,
    'legitimate': 20000
}

MODEL_CONFIG = {
    'input_shape': (10,),
    'hidden_layers': [64, 32],
    'dropout_rate': [0.3, 0.2],
    'output_classes': 6,
    'batch_size': 32,
    'epochs': 50,
    'learning_rate': 0.001
}

# Class names (must match TfLiteClassifier.kt)
CLASS_NAMES = ["Legitimate", "Ad", "Tracker", "Malware", "Phishing", "Analytics"]

# Directories
ASSETS_DIR = "app/src/main/assets"
DATA_DIR = "data"

# Ensure directories exist
os.makedirs(ASSETS_DIR, exist_ok=True)
os.makedirs(DATA_DIR, exist_ok=True)


def download_phishtank_urls(limit=5000):
    """Download phishing URLs from PhishTank."""
    url = "https://data.phishtank.com/data/online-valid.csv"
    try:
        print(f"🌐 Downloading phishing URLs from PhishTank...")
        response = requests.get(url, timeout=30)
        response.raise_for_status()
        
        urls = []
        for line in response.text.split('\n')[1:limit+1]:
            if ',' in line:
                parts = line.split(',')
                if len(parts) >= 2 and parts[1].strip():
                    urls.append(parts[1].strip())
        
        print(f"✅ Downloaded {len(urls)} phishing URLs from PhishTank")
        return urls[:limit]
    except Exception as e:
        print(f"⚠️ Could not download from PhishTank: {e}")
        return []


def download_openphish_urls(limit=5000):
    """Download phishing URLs from OpenPhish."""
    url = "https://openphish.com/feed.txt"
    try:
        print(f"🌐 Downloading phishing URLs from OpenPhish...")
        response = requests.get(url, timeout=30)
        response.raise_for_status()
        
        urls = [line.strip() for line in response.text.split('\n') if line.strip()]
        print(f"✅ Downloaded {len(urls)} phishing URLs from OpenPhish")
        return urls[:limit]
    except Exception as e:
        print(f"⚠️ Could not download from OpenPhish: {e}")
        return []


def download_malware_domains(limit=5000):
    """Download malware domains from malwaredomains.com."""
    url = "https://mirror1.malwaredomains.com/files/justdomains"
    try:
        print(f"🌐 Downloading malware domains...")
        response = requests.get(url, timeout=30)
        response.raise_for_status()
        
        domains = [line.strip() for line in response.text.split('\n') 
                  if line.strip() and not line.startswith('#')]
        urls = [f"http://{d}" for d in domains]
        print(f"✅ Downloaded {len(urls)} malware domains")
        return urls[:limit]
    except Exception as e:
        print(f"⚠️ Could not download malware domains: {e}")
        return []


def extract_domains_from_adblock(url, limit=5000):
    """Extract domains from AdBlock format list."""
    try:
        print(f"🌐 Downloading from {url}...")
        response = requests.get(url, timeout=30)
        response.raise_for_status()
        
        domains = []
        for line in response.text.split('\n'):
            line = line.strip()
            if line and not line.startswith('!') and not line.startswith('['):
                if '||' in line:
                    domain = line.split('||')[1].split('^')[0].split('/')[0]
                    if domain and '.' in domain:
                        domains.append(f"http://{domain}")
        
        print(f"✅ Extracted {len(domains)} domains")
        return domains[:limit]
    except Exception as e:
        print(f"⚠️ Could not download from {url}: {e}")
        return []


def download_easylist_urls(limit=5000):
    """Extract ad/tracker domains from EasyList."""
    url = "https://easylist.to/easylist/easylist.txt"
    return extract_domains_from_adblock(url, limit)


def download_easyprivacy_urls(limit=5000):
    """Extract analytics domains from EasyPrivacy."""
    url = "https://easylist.to/easylist/easyprivacy.txt"
    return extract_domains_from_adblock(url, limit)


def generate_legitimate_urls(limit=20000):
    """Generate legitimate URLs from top websites."""
    top_sites = [
        "google.com", "youtube.com", "facebook.com", "baidu.com", "wikipedia.org",
        "amazon.com", "twitter.com", "instagram.com", "weibo.com", "reddit.com",
        "yahoo.com", "linkedin.com", "ebay.com", "bing.com", "whatsapp.com",
        "pinterest.com", "paypal.com", "netflix.com", "spotify.com", "github.com",
        "stackoverflow.com", "apple.com", "microsoft.com", "adobe.com", "nytimes.com",
        "bbc.com", "cnn.com", "forbes.com", "washingtonpost.com", "theguardian.com",
        "stackexchange.com", "superuser.com", "askubuntu.com", "quora.com", "medium.com",
        "dev.to", "hashnode.com", "dribbble.com", "behance.net", "producthunt.com",
        "hackernews.com", "techcrunch.com", "theverge.com", "arstechnica.com", "wired.com",
        "github.io", "gitlab.com", "bitbucket.org", "sourceforge.net", "codepen.io",
        "jsfiddle.net", "replit.com", "glitch.com", "heroku.com", "vercel.com",
        "netlify.com", "firebase.google.com", "aws.amazon.com", "cloud.google.com",
        "azure.com", "digitalocean.com", "linode.com", "vultr.com", "render.com",
        "nginx.com", "apache.org", "mysql.com", "postgresql.org", "mongodb.com",
        "redis.io", "docker.com", "kubernetes.io", "terraform.io", "ansible.com",
        "ubuntu.com", "debian.org", "archlinux.org", "fedoraproject.org", "opensuse.org",
        "python.org", "java.com", "javascript.com", "typescriptlang.org", "rust-lang.org",
        "golang.org", "php.net", "ruby-lang.org", "swift.org", "kotlinlang.org",
        "android.com", "developer.android.com", "material.io", "jetpack.compose",
        "flutter.dev", "reactjs.org", "vuejs.org", "angular.io", "svelte.dev",
        "nextjs.org", "nuxtjs.org", "gatsbyjs.org", "tailwindcss.com", "bootstrap.com"
    ]
    
    legitimate_urls = []
    for site in top_sites:
        # Add main domain
        legitimate_urls.append(f"https://{site}")
        
        # Add common paths
        paths = ['', 'about', 'contact', 'blog', 'docs', 'support', 'pricing', 'features', 'api']
        for path in paths:
            if path:
                legitimate_urls.append(f"https://{site}/{path}")
                if len(legitimate_urls) < limit:
                    legitimate_urls.append(f"https://{site}/{path}/subpage")
                if len(legitimate_urls) < limit:
                    legitimate_urls.append(f"https://{site}/{path}?id=123&page=1")
        
        if len(legitimate_urls) >= limit:
            break
    
    print(f"✅ Generated {len(legitimate_urls)} legitimate URLs")
    return legitimate_urls[:limit]


def extract_features(url):
    """
    Extracts features from a URL to match FeatureExtractor.kt.
    Returns a numpy array of 10 features.
    """
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
            entropy -= prob * (np.log2(prob) if prob > 0 else 0)
        features.append(entropy / 4)  # Normalize
    else:
        features.append(0.0)
    
    # 4. Has numeric
    features.append(1.0 if any(c.isdigit() for c in domain) else 0.0)
    
    # 5. Has hyphen
    features.append(1.0 if '-' in domain else 0.0)
    
    # 6. TLD rarity
    tld = domain.split('.')[-1] if '.' in domain else ""
    common_tlds = {"com", "org", "net", "io", "co", "uk", "us", "de", "fr", "jp", "in", "au", "ca"}
    features.append(0.0 if tld in common_tlds else 1.0)
    
    # 7. Has IP address
    ip_pattern = r'^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$'
    features.append(1.0 if re.match(ip_pattern, domain) else 0.0)
    
    # 8. Has shortened URL
    shortened_domains = {"bit.ly", "goo.gl", "tinyurl.com", "ow.ly", "t.co", "is.gd", "buff.ly", "doiop.com"}
    features.append(1.0 if any(sd in domain for sd in shortened_domains) else 0.0)
    
    # 9. Has suspicious keywords
    suspicious_keywords = {"ad", "track", "analytics", "click", "banner", "promo", "xyz", "top", "free", "win", "prize"}
    features.append(1.0 if any(kw in domain for kw in suspicious_keywords) else 0.0)
    
    # 10. Uses HTTP
    features.append(1.0 if url.startswith("http://") else 0.0)
    
    return np.array(features, dtype=np.float32)


def create_model():
    """Creates the neural network model."""
    model = models.Sequential([
        layers.InputLayer(input_shape=MODEL_CONFIG['input_shape']),
        layers.Dense(MODEL_CONFIG['hidden_layers'][0], activation='relu'),
        layers.Dropout(MODEL_CONFIG['dropout_rate'][0]),
        layers.Dense(MODEL_CONFIG['hidden_layers'][1], activation='relu'),
        layers.Dropout(MODEL_CONFIG['dropout_rate'][1]),
        layers.Dense(MODEL_CONFIG['output_classes'], activation='softmax')
    ])
    
    model.compile(
        optimizer=optimizers.Adam(learning_rate=MODEL_CONFIG['learning_rate']),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    return model


def save_model(model, label_encoder):
    """Saves the model as TFLite and labels as JSON."""
    # Convert to TFLite
    print("\n💾 Converting to TFLite...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    
    # Save model
    model_path = os.path.join(ASSETS_DIR, "qubeguard_model.tflite")
    with open(model_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"✅ Model saved to: {model_path}")
    print(f"   Size: {len(tflite_model) / 1024:.2f} KB")
    
    # Save labels
    labels_path = os.path.join(ASSETS_DIR, "qubeguard_labels.json")
    with open(labels_path, 'w') as f:
        json.dump({
            'classes': label_encoder.classes_.tolist(),
            'created_at': datetime.now().isoformat()
        }, f)
    
    print(f"✅ Labels saved to: {labels_path}")
    
    return model_path, labels_path


def save_dataset(dataset, filename):
    """Saves the dataset to a CSV file."""
    df = pd.DataFrame(dataset, columns=["url", "label"])
    filepath = os.path.join(DATA_DIR, filename)
    df.to_csv(filepath, index=False)
    print(f"✅ Dataset saved to: {filepath}")
    return filepath


def main():
    print("=" * 60)
    print("🚀 QubeGuard TFLite Model Training")
    print("=" * 60)
    
    # Step 1: Download datasets
    print("\n📥 Step 1: Downloading datasets...")
    print("-" * 60)
    
    phishing_urls = download_phishtank_urls(DATA_LIMIT['phishing'])
    phishing_urls += download_openphish_urls(DATA_LIMIT['phishing'])
    phishing_urls = list(set(phishing_urls))[:DATA_LIMIT['phishing']]
    
    malware_urls = download_malware_domains(DATA_LIMIT['malware'])
    ad_urls = download_easylist_urls(DATA_LIMIT['ad'])
    analytics_urls = download_easyprivacy_urls(DATA_LIMIT['analytics'])
    legitimate_urls = generate_legitimate_urls(DATA_LIMIT['legitimate'])
    
    # Step 2: Create labeled dataset
    print("\n📊 Step 2: Creating labeled dataset...")
    print("-" * 60)
    
    dataset = []
    
    # Add phishing URLs
    for url in phishing_urls:
        dataset.append((url, "Phishing"))
    
    # Add malware URLs
    for url in malware_urls:
        dataset.append((url, "Malware"))
    
    # Add ad URLs (split between Ad and Tracker)
    for i, url in enumerate(ad_urls):
        if i % 2 == 0:
            dataset.append((url, "Ad"))
        else:
            dataset.append((url, "Tracker"))
    
    # Add analytics URLs
    for url in analytics_urls:
        dataset.append((url, "Analytics"))
    
    # Add legitimate URLs
    for url in legitimate_urls:
        dataset.append((url, "Legitimate"))
    
    # Shuffle the dataset
    random.shuffle(dataset)
    
    print(f"Total samples: {len(dataset)}")
    
    # Count by label
    label_counts = {}
    for _, label in dataset:
        label_counts[label] = label_counts.get(label, 0) + 1
    
    print("\nLabel Distribution:")
    for label, count in sorted(label_counts.items()):
        print(f"  {label}: {count} ({count/len(dataset)*100:.1f}%)")
    
    # Save dataset
    dataset_path = save_dataset(dataset, "training_dataset.csv")
    
    # Step 3: Extract features
    print("\n🔍 Step 3: Extracting features...")
    print("-" * 60)
    
    X = np.array([extract_features(url) for url, _ in dataset])
    y = np.array([label for _, label in dataset])
    
    print(f"✅ Features extracted: {X.shape}")
    
    # Step 4: Encode labels
    print("\n🏷️  Step 4: Encoding labels...")
    print("-" * 60)
    
    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)
    
    print(f"Classes: {list(label_encoder.classes_)}")
    
    # Step 5: Split data
    print("\n✂️  Step 5: Splitting data...")
    print("-" * 60)
    
    X_train, X_test, y_train, y_test = train_test_split(
        X, y_encoded, test_size=0.2, random_state=RANDOM_SEED, stratify=y_encoded
    )
    
    print(f"Train: {len(X_train)} samples")
    print(f"Test: {len(X_test)} samples")
    
    # Step 6: Create and train model
    print("\n🏗️  Step 6: Creating and training model...")
    print("-" * 60)
    
    model = create_model()
    model.summary()
    
    # Callbacks
    early_stopping = callbacks.EarlyStopping(
        monitor='val_loss', patience=10, restore_best_weights=True
    )
    reduce_lr = callbacks.ReduceLROnPlateau(
        monitor='val_loss', factor=0.2, patience=5, min_lr=1e-6
    )
    
    # Train
    history = model.fit(
        X_train, y_train,
        batch_size=MODEL_CONFIG['batch_size'],
        epochs=MODEL_CONFIG['epochs'],
        validation_data=(X_test, y_test),
        callbacks=[early_stopping, reduce_lr],
        verbose=1
    )
    
    # Step 7: Evaluate
    print("\n📊 Step 7: Evaluating model...")
    print("-" * 60)
    
    loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"Test Accuracy: {accuracy:.4f}")
    print(f"Test Loss: {loss:.4f}")
    
    # Classification report
    y_pred = model.predict(X_test)
    y_pred_classes = np.argmax(y_pred, axis=1)
    
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred_classes, target_names=label_encoder.classes_))
    
    # Step 8: Save model
    print("\n💾 Step 8: Saving model...")
    print("-" * 60)
    
    model_path, labels_path = save_model(model, label_encoder)
    
    # Step 9: Test the model
    print("\n🧪 Step 9: Testing the model...")
    print("-" * 60)
    
    # Load TFLite model
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    
    # Test URLs
    test_urls = [
        "https://google.com",
        "https://ads.example.com",
        "https://tracker.example.com",
        "https://malicious-site.com",
        "https://phishing-bank.com",
        "https://analytics.example.com"
    ]
    
    for url in test_urls:
        features = extract_features(url)
        features = np.expand_dims(features, axis=0).astype(np.float32)
        
        interpreter.set_tensor(interpreter.get_input_details()[0]['index'], features)
        interpreter.invoke()
        output = interpreter.get_tensor(interpreter.get_output_details()[0]['index'])
        
        predicted_class = np.argmax(output[0])
        predicted_label = label_encoder.classes_[predicted_class]
        confidence = np.max(output[0])
        
        print(f"\nURL: {url}")
        print(f"  Predicted: {predicted_label} ({confidence:.4f})")
        for i, (label, prob) in enumerate(zip(label_encoder.classes_, output[0])):
            print(f"    {label}: {prob:.4f}")
    
    # Step 10: Summary
    print("\n" + "=" * 60)
    print("✅ Model Training Complete!")
    print("=" * 60)
    
    print(f"\n📋 Summary:")
    print(f"  Model File: {model_path}")
    print(f"  Model Size: {os.path.getsize(model_path) / 1024:.2f} KB")
    print(f"  Labels File: {labels_path}")
    print(f"  Dataset: {dataset_path}")
    print(f"  Test Accuracy: {accuracy:.4f}")
    print(f"  Classes: {list(label_encoder.classes_)}")
    
    print(f"\n📥 Next Steps:")
    print(f"  1. Copy the model to your Android project:")
    print(f"     cp {model_path} <your-project>/app/src/main/assets/")
    print(f"  2. Or update Constants.MODEL_URL to download from a server")
    print(f"  3. Build and run the QubeGuard app")
    
    print(f"\n💡 To improve the model:")
    print(f"  - Add more training data")
    print(f"  - Balance the classes")
    print(f"  - Tune hyperparameters")
    print(f"  - Try different architectures")


if __name__ == "__main__":
    main()
