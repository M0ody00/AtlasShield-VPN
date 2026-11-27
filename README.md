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
