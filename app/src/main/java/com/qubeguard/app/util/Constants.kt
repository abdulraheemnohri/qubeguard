package com.qubeguard.app.util

/**
 * App-wide constants for QubeGuard.
 */
object Constants {
    // App
    const val APP_NAME = "QubeGuard"
    const val APP_VERSION = "1.0.0"

    // VPN
    const val VPN_SERVICE_NOTIFICATION_ID = 1
    const val VPN_SERVICE_NOTIFICATION_CHANNEL_ID = "QubeGuard VPN Channel"
    const val VPN_SERVICE_NOTIFICATION_CHANNEL_NAME = "QubeGuard VPN"

    // DNS Proxy
    const val DNS_PROXY_PORT = 5353
    const val UPSTREAM_DNS_SERVER = "1.1.1.1" // Cloudflare DNS
    const val UPSTREAM_DNS_PORT = 53

    // ML Model
    const val MODEL_FILE_NAME = "qubeguard_model.tflite"
    // Default model URL (host your own model or use a public one)
    // For testing, use the dummy model from scripts/generate_model.py
    // For production, train a model using scripts/train_model.py and host it
    const val MODEL_URL = "https://raw.githubusercontent.com/abdulraheemnohri/qubeguard/main/models/qubeguard_model.tflite"
    
    // Alternative public model URLs (uncomment to use)
    // const val MODEL_URL = "https://your-server.com/models/qubeguard_model.tflite"

    // Blocklist
    const val DEFAULT_BLOCKLIST_SOURCES = """
        [
            {
                "id": "easylist_ads",
                "name": "EasyList",
                "category": "ads",
                "url": "https://easylist.to/easylist/easylist.txt",
                "format": "adblock_plus",
                "license": "GPLv3",
                "update_interval_hours": 24
            },
            {
                "id": "easyprivacy",
                "name": "EasyPrivacy",
                "category": "privacy",
                "url": "https://easylist.to/easylist/easyprivacy.txt",
                "format": "adblock_plus",
                "license": "GPLv3",
                "update_interval_hours": 24
            },
            {
                "id": "malware_domains",
                "name": "Malware Domains",
                "category": "security",
                "url": "https://mirror1.malwaredomains.com/files/justdomains",
                "format": "hosts",
                "license": "CC BY-SA 4.0",
                "update_interval_hours": 48
            },
            {
                "id": "adguard_mobile",
                "name": "AdGuard Mobile",
                "category": "ads",
                "url": "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt",
                "format": "adblock_plus",
                "license": "GPLv3",
                "update_interval_hours": 24
            }
        ]
    """

    // Feedback
    const val FEEDBACK_UPLOAD_URL = "https://your-server.com/api/feedback" // Replace with your endpoint
    const val FEEDBACK_UPLOAD_BATCH_SIZE = 10

    // Qube
    const val DEFAULT_QUBE_ID = "default"
    const val INCOGNITO_QUBE_ID = "incognito"

    // Timeouts
    const val NETWORK_TIMEOUT_SECONDS = 30L
    const val DNS_QUERY_TIMEOUT_SECONDS = 10L

    // Thresholds for ML Classifier
    const val ML_AD_THRESHOLD = 0.7f
    const val ML_TRACKER_THRESHOLD = 0.7f
    const val ML_MALWARE_THRESHOLD = 0.85f
    const val ML_PHISHING_THRESHOLD = 0.8f
    const val ML_ANALYTICS_THRESHOLD = 0.75f
    
    // Default blocklist update interval (in hours)
    const val DEFAULT_BLOCKLIST_UPDATE_INTERVAL = 24
}
