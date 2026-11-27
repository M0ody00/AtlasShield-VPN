# AtlasShield-VPN

Native Android VPN client for Android 7+ using the official WireGuard tunnel library. Android 11 / OPPO F11 device testing pending.

## Build
JDK 17, Android SDK 36, Gradle 8.13, AGP 8.13.2. GitHub Actions builds and lints a debug APK. Debug signing is not production signing.

## Usage
Import an authorized WireGuard configuration locally, choose its actual country and connect. No VPN credentials or server endpoints are bundled. The country picker does not imply deployed servers. Enable Always-on VPN and Block connections without VPN in Android settings if needed and test on your device.

## Security and limitations
Encrypted profile storage uses Android Keystore and AES-GCM. No ad SDK or analytics. VPN operators can still observe metadata. This source preview has not been independently audited; actual connectivity, DNS and IPv6 leak protection, device compatibility and throughput require testing. Do not publish private keys, configuration files or signing keys.

Official library: https://github.com/WireGuard/wireguard-android

## v0.1.1
Open Proton's official downloads page from the app, download an authorized WireGuard profile, then import `.conf` or `.conf.txt`. Server country/name suggestions are derived from the filename or peer comment, and are not verified server geolocation. Duplicate exact profiles are selected instead of added twice. Private keys remain in the local encrypted vault; no account password or keys are included in the public APK. Rotate any previously shared key.

## Smart Global beta 1.0.0
Multi-select up to 50 authorized WireGuard files in Android's picker; encrypted vault capped at 200 profiles for bulk import. Smart Connect tests up to 4 saved profiles sequentially, selected profile first, using an HTTPS 204 request explicitly bound to the active VPN network. It checks a fresh authenticated WireGuard handshake and ranks successful tests by response time, then reconnects and rechecks the winner. Not a bandwidth benchmark. Traffic to the HTTPS check endpoint (connectivitycheck.gstatic.com) occurs only when the user requests Smart Connect. Enable Android lockdown to prevent unrelated app traffic escaping during switching. No automated Proton authentication, entitlement bypass, or remote config downloads; no fabricated country availability or load. New debug builds may have different signing certificates; keep a fresh local config before reinstalling.

## 1.1.0 experimental Proton discovery
The Discover Proton FREE servers button loads a bundled, MIT-licensed community catalog from qdm12/gluetun-servers (source blob 427d2ca22c784625934d3811256629555032402d), containing 53 free WireGuard entries. Not an official Proton API, no live load numbers or guaranteed entitlements. Requires one user-imported Proton FREE configuration. After explicit consent, up to six catalog entries are tried sequentially using the private key already stored on device. The key is never uploaded to the catalog source; only successful authenticated WireGuard and VPN-bound HTTPS probes are saved. Provider may reject reusing a manual key across servers; the UI reports zero if all fail. No paid servers, no credential automation. Enable Android lockdown for switching. Catalog snapshot does not refresh automatically; future releases can update it. Source notice included in assets.
