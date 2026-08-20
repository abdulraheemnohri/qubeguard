# Data Directory

This directory is for storing **training datasets** for the QubeGuard ML model.

## 📁 Dataset Format

Create a CSV file with the following format:

```csv
url,label
https://example.com,Legitimate
https://ads.example.com,Ad
https://tracker.example.com,Tracker
https://malware-site.com,Malware
https://phishing-site.com,Phishing
https://analytics.example.com,Analytics
```

### Supported Labels
| Label | Description | Example URLs |
|-------|-------------|--------------|
| `Legitimate` | Safe, non-malicious content | `https://google.com`, `https://wikipedia.org` |
| `Ad` | Advertisement-related content | `https://ads.example.com`, `https://doubleclick.net` |
| `Tracker` | Tracking scripts/pixels | `https://google-analytics.com`, `https://facebook-pixel.com` |
| `Malware` | Malicious software sites | `https://malicious-site.com` |
| `Phishing` | Phishing/fraudulent sites | `https://fake-bank.com` |
| `Analytics` | Analytics/telemetry services | `https://analytics.example.com` |

---

## 📥 Sample Dataset

Here's a small sample dataset to get you started (`sample_dataset.csv`):

```csv
url,label
https://google.com,Legitimate
https://youtube.com,Legitimate
https://wikipedia.org,Legitimate
https://github.com,Legitimate
https://stackoverflow.com,Legitimate
https://adservice.google.com,Ad
https://pagead2.googlesyndication.com,Ad
https://doubleclick.net,Ad
https://googlesyndication.com,Ad
https://google-analytics.com,Tracker
https://facebook-pixel.com,Tracker
https://connect.facebook.net,Tracker
https://tracker.example.com,Tracker
https://malicious-site.com,Malware
https://evil-domain.xyz,Malware
https://fake-bank.com,Phishing
https://phishing-example.net,Phishing
https://analytics.example.com,Analytics
https://mixpanel.com,Analytics
```

---

## 🌐 Public Datasets

### Phishing URLs
1. **PhishTank** - [https://www.phishtank.com/](https://www.phishtank.com/)
   - Real-time phishing URL feed
   - API access available

2. **OpenPhish** - [https://openphish.com/](https://openphish.com/)
   - Real-time phishing URL feed
   - Free for non-commercial use

3. **Kaggle Phishing Datasets**
   - [Phishing URL Dataset](https://www.kaggle.com/datasets/sid321axn/malware-url-dataset)
   - [URL Phishing Detection](https://www.kaggle.com/datasets/)

### Malware URLs
1. **Malware Domains** - [http://www.malwaredomains.com/](http://www.malwaredomains.com/)
   - List of malware-related domains
   - Free for non-commercial use

2. **Malware URL Dataset (Kaggle)](https://www.kaggle.com/datasets/sid321axn/malware-url-dataset)
   - 650,000+ URLs with labels
   - Includes phishing, malware, and benign URLs

### Ad/Tracker URLs
1. **EasyList** - [https://easylist.to/](https://easylist.to/)
   - Ad and tracker blocklists
   - Can be used to extract ad/tracker URLs

2. **AdGuard** - [https://adguard.com/](https://adguard.com/)
   - Ad and tracker blocklists
   - Open source

---

## 🛠️ Dataset Preparation

### Step 1: Collect URLs
Gather URLs from various sources:
- Your own browsing history (anonymized)
- Public blocklists (EasyList, AdGuard)
- Public datasets (Kaggle, PhishTank)
- Manual labeling

### Step 2: Label URLs
Label each URL with one of the 6 categories:
- Use browser extensions to identify trackers/ads
- Check URL against known malicious domain lists
- Manually verify phishing/malware sites

### Step 3: Clean Data
- Remove duplicates
- Validate URLs (use `String.isValidUrl()` from `Extensions.kt`)
- Balance classes (aim for roughly equal samples per class)
- Split into training/test sets (80/20 split recommended)

### Step 4: Save as CSV
Save your dataset as a CSV file in this directory:
```bash
# Example: save as data/url_dataset.csv
url,label
https://example.com,Legitimate
...
```

---

## 🚀 Training the Model

Once you have a dataset, train the model:

```bash
# Install dependencies
pip install tensorflow numpy pandas scikit-learn

# Train the model
python scripts/train_model.py --data_path data/url_dataset.csv
```

The trained model will be saved to `app/src/main/assets/qubeguard_model.tflite`.

---

## 📊 Dataset Statistics

For a good model, aim for:
- **Minimum 10,000 samples** (more is better)
- **Balanced classes** (roughly equal samples per category)
- **Diverse sources** (don't rely on a single dataset)
- **Recent data** (URLs change over time)

Example distribution:
| Label | Samples | Percentage |
|-------|---------|------------|
| Legitimate | 50,000 | 50% |
| Ad | 10,000 | 10% |
| Tracker | 10,000 | 10% |
| Malware | 10,000 | 10% |
| Phishing | 10,000 | 10% |
| Analytics | 10,000 | 10% |

---

## ⚠️ Important Notes

1. **Privacy:** If collecting URLs from users:
   - Anonymize all data
   - Remove personally identifiable information (PII)
   - Comply with privacy laws (GDPR, CCPA)

2. **License:** Respect the license of public datasets:
   - Check usage restrictions
   - Attribute sources properly
   - Don't redistribute without permission

3. **Security:** When handling malicious URLs:
   - Never visit them directly
   - Use safe APIs for verification
   - Scan files in isolated environments

---

## 📁 Current Files

| File | Description | Size |
|------|-------------|------|
| (None yet) | Add your dataset files | - |

---

## 🔗 Useful Tools

- [CSV Kitchen](https://csvkitchen.com/) - Clean and format CSV files
- [OpenRefine](https://openrefine.org/) - Data cleaning and transformation
- [Pandas](https://pandas.pydata.org/) - Python data analysis library
- [TensorFlow Data Validation](https://www.tensorflow.org/tfx/data_validation/get_started) - Validate your dataset
