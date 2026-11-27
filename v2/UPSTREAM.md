# AtlasShield V2 — Proton foundation preview

This is an independent experimental build, not an official Proton product. Upstream: https://github.com/ProtonVPN/android-app at commit 416d2ee1d979615d7c644f19338b56f82cb39b68. Preserve upstream GPLv3 license, notices and authorship. The build workflow checks out this exact public source and applies the version-controlled patch below; the corresponding source is recoverable from the pinned revision plus this repository's v2 files.

Baseline objectives: launch on API 30, test provider-supported guest session or normal sign-in, live authorized server list, automatic connection, key generation and protocol selection, without importing files. Guest mode and server entitlement remain controlled by Proton. A compiled APK is not evidence of service access or privacy guarantees.

The preview uses a separate application ID; account redirects, provider API compatibility, guest-mode availability, telemetry defaults and all security features need device verification. No attempt to bypass service restrictions or impersonate official signing keys. Do not enter real account credentials into an unreviewed build; use guest mode if offered, or a dedicated test account after review. No credentials in CI.

Upstream requires SDK 36, NDK 28.1.13356709, JDK 17, Gradle wrapper 8.13, SWIG and CMake. Build target from upstream README: assembleProductionVanillaOpenSourceDebug.

## Acceptance gates
- CI: source checkout, dependency resolution, compile and package, verify APK signature and ARM64.
- Device: install alongside V1.1; launch and authenticate normally, discover real entitled servers, connect without configuration import, verify public IP changes.
- Reliability: switch Wi-Fi/mobile, reconnect, test blocked UDP / supported fallback, screen off and reboot.
- Security: check DNS/IPv6 leaks and Android lockdown; audit provider telemetry and certificate validation. Do not advertise zero logs or no analytics until verified.
- Release: complete distinct AtlasShield visual identity, preserve license and attribution, use stable private release signing key, validate provider terms.

## 2.1 Lite Secure preview
New green/black globe and custom Compose home with native upstream connect/disconnect and advanced controls. Founder & Owner: Ahmed Nasr Labib (AtlasShield project only; upstream authorship preserved). ARM64 native packaging; actual size and compatibility require CI verification. Two optional telemetry gates forced off; not a complete audit of provider communications. No payment or entitlement bypass. Home promotions suppressed in advanced view, other provider UI may retain plan choices. Local network monitor detects default VPN loss while process alive, reports Android route/DNS configuration indicators, captive portal and validated reachability; it cannot prove leak prevention or detect spyware. Android lockdown settings accessible. No remote probes or external logs added. Full security audit, stable release signing, background service, active leak tests, comprehensive theme/navigation redesign and private free server network remain pending.
