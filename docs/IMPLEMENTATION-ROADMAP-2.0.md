# QubeGuard 2.0 Implementation Roadmap

This branch is production hardening work. Features are marked only when implemented and tested.

## Phase 1 — VPN/TUN
- [x] DNS-only TUN architecture
- [x] Protected upstream sockets
- [x] IPv4/UDP DNS packet extraction
- [x] DNS response packet construction
- [ ] IPv6 packet support
- [ ] True full-tunnel IP forwarding (separate project milestone)

## Phase 2 — DNS core
- [x] bounded positive cache
- [x] upstream failover
- [x] response validation
- [x] UDP truncation detection
- [x] DNS-over-TCP fallback
- [x] bounded negative-cache component
- [x] upstream circuit-breaker component
- [ ] integrate negative cache into request path
- [ ] EDNS(0) parser/forwarding
- [ ] DNS-over-TLS
- [ ] DNS-over-HTTPS
- [ ] DNSSEC validation/policy

## Phase 3 — Blocklists
- [x] atomic rule compiler snapshots
- [x] domain radix tree
- [x] bounded regex engine
- [x] Bloom-filter acceleration
- [x] secure HTTPS source URL validation
- [x] SHA-256 utility
- [ ] downloader with size limits
- [ ] streaming parser
- [ ] signed manifest verification
- [ ] atomic database activation/rollback
- [ ] scheduled updates

## Phase 4 — Browser/Qubes
- [ ] isolated WebView storage profiles
- [ ] per-Qube permissions
- [ ] per-Qube DNS profiles
- [ ] download security
- [ ] popup/intent hardening
- [ ] site permission manager

## Phase 5 — AI security
- [ ] local inference abstraction
- [ ] model manifest verification
- [ ] bounded inference queue
- [ ] phishing classification
- [ ] explainable risk score
- [ ] model rollback

## Phase 6 — Privacy/operations
- [ ] encrypted sensitive preferences
- [ ] redacted diagnostics
- [ ] configurable log retention
- [ ] exportable diagnostics
- [ ] dependency/SBOM checks
- [ ] release signing verification

## Rule
Do not mark an item complete until code, unit tests, and integration validation exist. Avoid enabling full-tunnel routing until packet forwarding is actually implemented.
