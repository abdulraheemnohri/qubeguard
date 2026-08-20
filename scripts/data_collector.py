#!/usr/bin/env python3
"""
QubeGuard Data Collector

This script collects URLs from free public sources and saves them to a CSV file
for training the QubeGuard ML model.

Sources:
- PhishTank (phishing URLs)
- OpenPhish (phishing URLs)
- Malware Domains (malware URLs)
- EasyList (ad/tracker URLs)
- EasyPrivacy (analytics URLs)
- Top websites (legitimate URLs)

Usage:
    python scripts/data_collector.py --output data/training_dataset.csv

Output:
    CSV file with columns: url,label
    Labels: Legitimate, Ad, Tracker, Malware, Phishing, Analytics
"""

import argparse
import os
import re
import random
import requests
import csv
from datetime import datetime

# Configuration
DEFAULT_OUTPUT = "data/training_dataset.csv"
DATA_LIMIT = {
    'phishing': 5000,
    'malware': 5000,
    'ad': 5000,
    'analytics': 5000,
    'legitimate': 20000
}


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
        "nextjs.org", "nuxtjs.org", "gatsbyjs.org", "tailwindcss.com", "bootstrap.com",
        "wordpress.org", "joomla.org", "drupal.org", "shopify.com", "magento.com",
        "salesforce.com", "zendesk.com", "slack.com", "discord.com", "zoom.us",
        "notion.so", "trello.com", "asana.com", "clickup.com", "Monday.com",
        "figma.com", "sketch.com", "invisionapp.com", "miro.com", "canva.com",
        "unsplash.com", "pexels.com", "pixabay.com", "flickr.com", "imgur.com"
    ]
    
    legitimate_urls = []
    for site in top_sites:
        legitimate_urls.append(f"https://{site}")
        
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


def save_dataset(dataset, output_path):
    """Saves the dataset to a CSV file."""
    # Ensure output directory exists
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    with open(output_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['url', 'label'])  # Header
        for url, label in dataset:
            writer.writerow([url, label])
    
    print(f"✅ Dataset saved to: {output_path}")
    print(f"   Total samples: {len(dataset)}")
    
    # Print label distribution
    label_counts = {}
    for _, label in dataset:
        label_counts[label] = label_counts.get(label, 0) + 1
    
    print("\nLabel Distribution:")
    for label, count in sorted(label_counts.items()):
        print(f"  {label}: {count} ({count/len(dataset)*100:.1f}%)")


def main():
    parser = argparse.ArgumentParser(description='QubeGuard Data Collector')
    parser.add_argument('--output', type=str, default=DEFAULT_OUTPUT,
                        help='Output CSV file path')
    parser.add_argument('--limit', type=int, default=None,
                        help='Maximum number of samples per category')
    args = parser.parse_args()
    
    # Update limits if specified
    if args.limit:
        for key in DATA_LIMIT:
            DATA_LIMIT[key] = args.limit
    
    print("=" * 60)
    print("🚀 QubeGuard Data Collector")
    print("=" * 60)
    print()
    
    # Collect URLs
    print("📥 Collecting URLs from public sources...")
    print("-" * 60)
    
    phishing_urls = download_phishtank_urls(DATA_LIMIT['phishing'])
    phishing_urls += download_openphish_urls(DATA_LIMIT['phishing'])
    phishing_urls = list(set(phishing_urls))[:DATA_LIMIT['phishing']]
    
    malware_urls = download_malware_domains(DATA_LIMIT['malware'])
    ad_urls = download_easylist_urls(DATA_LIMIT['ad'])
    analytics_urls = download_easyprivacy_urls(DATA_LIMIT['analytics'])
    legitimate_urls = generate_legitimate_urls(DATA_LIMIT['legitimate'])
    
    # Create dataset
    print("\n📊 Creating labeled dataset...")
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
    random.seed(42)
    random.shuffle(dataset)
    
    print(f"Total samples: {len(dataset)}")
    
    # Save dataset
    save_dataset(dataset, args.output)
    
    print("\n" + "=" * 60)
    print("✅ Data Collection Complete!")
    print("=" * 60)
    print(f"\n📥 Next Steps:")
    print(f"  1. Use the dataset to train the model:")
    print(f"     python scripts/train_qubeguard_model.py")
    print(f"  2. Or use the dataset with your own training script")
    print(f"  3. The dataset is ready at: {args.output}")


if __name__ == "__main__":
    main()
