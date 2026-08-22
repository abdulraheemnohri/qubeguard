package com.qubeguard.app.data.blocklist

/**
 * Curated upstream catalog. Lists are referenced, not bundled, so users receive
 * the latest upstream data and QubeGuard can respect each project's terms.
 */
object BlocklistCatalog {
    val defaults: List<BlocklistSource> = listOf(
        source("easylist", "EasyList", "ads", "https://easylist.to/easylist/easylist.txt", "adblock_plus", "GPLv3"),
        source("easyprivacy", "EasyPrivacy", "privacy", "https://easylist.to/easylist/easyprivacy.txt", "adblock_plus", "GPLv3"),
        source("adguard_base", "AdGuard Base", "ads", "https://filters.adtidy.org/extension/ublock/filters/2.txt", "adblock_plus", "GPLv3"),
        source("adguard_tracking", "AdGuard Tracking Protection", "privacy", "https://filters.adtidy.org/extension/ublock/filters/3.txt", "adblock_plus", "GPLv3"),
        source("adguard_social", "AdGuard Social Media", "social", "https://filters.adtidy.org/extension/ublock/filters/4.txt", "adblock_plus", "GPLv3"),
        source("adguard_video", "AdGuard Video & Mobile Ads", "ads", "https://filters.adtidy.org/extension/ublock/filters/11.txt", "adblock_plus", "GPLv3"),
        source("peter_lowe", "Peter Lowe's Ad and tracking server list", "privacy", "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext", "hosts", "Upstream terms"),
        source("stevenblack_unified", "StevenBlack Unified Hosts", "ads", "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts", "hosts", "MIT"),
        source("stevenblack_porn", "StevenBlack Porn + Ads", "annoyances", "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn/hosts", "hosts", "MIT"),
        source("stevenblack_gambling", "StevenBlack Gambling + Ads", "annoyances", "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/gambling/hosts", "hosts", "MIT"),
        source("urlhaus_malware", "URLhaus Malware Domains", "security", "https://urlhaus.abuse.ch/downloads/hostfile/", "hosts", "Abuse.ch terms"),
        source("hagezi_pro", "HaGeZi Pro", "privacy", "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/pro.txt", "adblock_plus", "Upstream terms"),
        source("hagezi_ultimate", "HaGeZi Ultimate", "security", "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/ultimate.txt", "adblock_plus", "Upstream terms"),
        source("hagezi_gambling", "HaGeZi Gambling Blocklist", "annoyances", "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/gambling.txt", "adblock_plus", "Upstream terms"),
        source("hagezi_tlds", "HaGeZi TLDs", "security", "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/tif.txt", "adblock_plus", "Upstream terms"),
        source("oisd_basic", "OISD Basic", "ads", "https://big.oisd.nl/basic", "adblock_plus", "Upstream terms"),
        source("oisd_small", "OISD Small", "privacy", "https://small.oisd.nl/", "adblock_plus", "Upstream terms"),
        source("phishing_army", "Phishing Army", "security", "https://phishing.army/download/phishing_army_blocklist.txt", "hosts", "Upstream terms"),
        source("notrack", "NoTrack Tracker Blocklist", "privacy", "https://raw.githubusercontent.com/quidsup/notrack/master/trackers.txt", "hosts", "GPLv3"),
        source("dan_pollock", "SomeoneWhoCares Hosts", "privacy", "https://someonewhocares.org/hosts/hosts", "hosts", "Upstream terms"),
        source("adaway", "AdAway Hosts", "ads", "https://adaway.org/hosts.txt", "hosts", "GPLv3"),
        source("ublock_ads", "uBlock Filters", "ads", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt", "adblock_plus", "GPLv3"),
        source("ublock_privacy", "uBlock Privacy", "privacy", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt", "adblock_plus", "GPLv3"),
        source("ublock_badware", "uBlock Badware Risks", "security", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt", "adblock_plus", "GPLv3"),
        source("ublock_annoyances", "uBlock Annoyances", "annoyances", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt", "adblock_plus", "GPLv3"),
        source("1hosts_pro", "1Hosts Pro", "privacy", "https://raw.githubusercontent.com/badmojr/1Hosts/master/Pro/adblock.txt", "adblock_plus", "Upstream terms"),
        source("lightswitch05_ads", "Lightswitch05 Ads and Tracking", "privacy", "https://www.github.developerdan.com/hosts/lists/ads-and-tracking-extended.txt", "hosts", "Upstream terms"),
        source("blocklistproject_ads", "Blocklist Project Ads", "ads", "https://blocklistproject.github.io/Lists/ads.txt", "hosts", "Upstream terms"),
        source("blocklistproject_tracking", "Blocklist Project Tracking", "privacy", "https://blocklistproject.github.io/Lists/tracking.txt", "hosts", "Upstream terms"),
        source("blocklistproject_malware", "Blocklist Project Malware", "security", "https://blocklistproject.github.io/Lists/malware.txt", "hosts", "Upstream terms"),
        source("blocklistproject_phishing", "Blocklist Project Phishing", "security", "https://blocklistproject.github.io/Lists/phishing.txt", "hosts", "Upstream terms"),
        source("blocklistproject_fraud", "Blocklist Project Fraud", "security", "https://blocklistproject.github.io/Lists/fraud.txt", "hosts", "Upstream terms"),
        source("blocklistproject_crypto", "Blocklist Project Crypto Mining", "security", "https://blocklistproject.github.io/Lists/crypto.txt", "hosts", "Upstream terms"),
        source("blocklistproject_ransomware", "Blocklist Project Ransomware", "security", "https://blocklistproject.github.io/Lists/ransomware.txt", "hosts", "Upstream terms"),
        source("blocklistproject_social", "Blocklist Project Social", "social", "https://blocklistproject.github.io/Lists/social.txt", "hosts", "Upstream terms"),
        source("kadhosts", "KADhosts", "ads", "https://raw.githubusercontent.com/PolishFiltersTeam/KADhosts/master/KADhosts.txt", "hosts", "Upstream terms"),
        source("fade_spam", "FadeMind Spam Hosts", "annoyances", "https://raw.githubusercontent.com/FadeMind/hosts.extras/master/add.Spam/hosts", "hosts", "Upstream terms"),
        source("energized_blitz", "Energized BLITZ", "privacy", "https://block.energized.pro/blitz/domains.txt", "hosts", "Upstream terms")
    )

    private fun source(
        id: String,
        name: String,
        category: String,
        url: String,
        format: String,
        license: String
    ) = BlocklistSource(
        id = id,
        name = name,
        category = category,
        url = url,
        format = format,
        license = license,
        updateIntervalHours = 24,
        version = null,
        sha256Hash = null,
        lastUpdated = null,
        enabled = true
    )
}
