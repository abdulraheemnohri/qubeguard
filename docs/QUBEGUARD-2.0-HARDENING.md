# QubeGuard 2.0 Hardening

This branch is the production-hardening track for QubeGuard.

## Networking architecture

QubeGuard now treats the Android VPN as a **DNS-only TUN bridge**, not a fake full-tunnel router. Android's `VpnService` exposes IP packets through the established file descriptor; the service must read/write that descriptor when it owns routed traffic. The previous implementation created a separate UDP socket and claimed `0.0.0.0/0`, which did not implement the required packet forwarding path.

The hardened path is:

```text
Android DNS resolver
        |
        v
VPN DNS address 10.0.0.1:53
        |
        v
TUN IPv4/UDP parser
        |
        v
127.0.0.1:5353 QubeGuard DNS proxy
        |
        +--> local records
        +--> PolicyEngine
        +--> sinkhole
        +--> conditional DNS
        +--> upstream DNS
        |
        v
TUN response packet
```

Normal application traffic is intentionally **not** routed through this service. A future full-tunnel mode must implement a real IP forwarding stack before enabling `0.0.0.0/0`.

Android documents that `VpnService.establish()` returns a file descriptor carrying IP packets and that VPN applications must process those packets; it also documents `protect()` for sockets used to reach an upstream gateway without creating a VPN loop. See the Android `VpnService` and `VpnService.Builder` API documentation.

## Fixed in this phase

- Removed the duplicate UDP:5353 socket from `VpnServiceImplementation`.
- Removed the incorrect default route from the DNS firewall VPN.
- Added an explicit TUN IPv4/UDP DNS packet codec.
- Added DNS response packet generation with IP/UDP checksums.
- Added a DNS-only TUN processing loop.
- Added upstream socket protection to prevent VPN recursion.
- Kept split-tunnel package exclusions.
- Hardened DNS question parsing and class validation.
- Added unit coverage for DNS packet extraction.
- Kept all policy decisions inside `PolicyEngine` through `DnsProxy`.

## Remaining 2.0 work

### Core

- Immutable atomic compiled rule sets.
- Separate domain and URL Bloom filters.
- Safe URL canonicalization including IDN/punycode and IPv6 literals.
- DNS cache with TTL and negative caching.
- Multiple upstream resolvers with health checks and failover.
- DNS-over-TLS and DNS-over-HTTPS transports.
- EDNS preservation and TCP fallback for oversized DNS responses.
- DNSSEC-aware pass-through and validation policy.

### Security

- Signed model manifests plus SHA-256 model verification.
- Streaming model downloads.
- Regex resource limits and untrusted-rule isolation.
- WebView scheme/intent/file-access hardening.
- Privacy-preserving log levels and automatic retention.
- Central settings repository instead of direct SharedPreferences reads.

### Browser/Qubes

- Independent WebView storage directories per Qube.
- Truly ephemeral Incognito Qube storage.
- Site permission isolation.
- Network filtering before cosmetic filtering.
- Browser tab/download/reader components split from the Activity.

### Reliability

- DNS parser/response test vectors.
- Policy engine tests.
- Blocklist compiler tests.
- Model-manager tests.
- Qube transaction tests.
- Android instrumented VPN tests on multiple Android versions/OEMs.
- Release R8 build, lint, static analysis, dependency scanning and SBOM.

## Safety rule

Never enable a full default route until a complete, tested IP forwarding implementation exists. A DNS firewall and a general-purpose VPN router are different products and must not be conflated.
