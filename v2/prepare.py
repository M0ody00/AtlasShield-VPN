#!/usr/bin/env python3
"""GPL upstream patch: AtlasShield home, local protection monitor and packaging."""
from pathlib import Path
import re
import shutil
import sys

root = Path(sys.argv[1])
here = Path(__file__).resolve().parent
app = root / "app"
src = app / "src/main"
def patch(path, old, new):
    data = path.read_text(encoding="utf-8")
    if data.count(old) != 1:
        raise SystemExit(f"Unexpected upstream anchor in {path}: {old[:80]}")
    path.write_text(data.replace(old, new, 1), encoding="utf-8")

patch(src/"res/values/strings.xml", '<string name="app_name" translatable="false">Proton VPN</string>', '<string name="app_name" translatable="false">AtlasShield VPN</string>')
manifest = src/"AndroidManifest.xml"
s = manifest.read_text(encoding="utf-8")
s = re.sub(r'android:(icon|roundIcon)="@mipmap/ic_launcher[^"]*"', r'android:\1="@drawable/atlas_globe"', s)
manifest.write_text(s, encoding="utf-8")
shutil.copyfile(here/"atlas_globe.xml", src/"res/drawable/atlas_globe.xml")
pkg = src/"java/com/protonvpn/android"
home = pkg/"redesign/home_screen/ui/Home.kt"
s = home.read_text(encoding="utf-8")
start = s.index("    HomeView(\n        vpnState = vpnStatusViewState,")
end = s.index("\n\n    homeViewModel.eventFlow",start)
old_call = s[start:end].strip()
# Keep the original connection view accessible, but suppress home promotional banners.
for name in ("upsellCarouselState", "bottomPromoComponent", "prominentPromoComponent"):
    old_call = re.sub(r"(?m)^(\s*"+name+r"\s*=\s*)[^,\n]+,",r"\1null,",old_call)
new_call = """    var atlasAdvanced by remember { mutableStateOf(false) }
    if (atlasAdvanced) {
        androidx.compose.foundation.layout.Column {
            androidx.compose.material3.TextButton(onClick = { atlasAdvanced = false }) {
                Text("AtlasShield")
            }
            Box(modifier = Modifier.weight(1f)) {
                """ + old_call + """
            }
        }
    } else {
        AtlasHome(
            onConnect = connectionCardComponent.onConnect,
            onDisconnect = connectionCardComponent.onDisconnect,
            onDetails = connectionCardComponent.onConnectionCardClick,
            onAdvanced = { atlasAdvanced = true }
        )
    }"""
# weight is a ColumnScope member extension, resolved inside Column.
home.write_text(s[:start]+new_call+s[end:], encoding="utf-8")
shutil.copyfile(here/"AtlasHome.kt",home.parent/"AtlasHome.kt")
shutil.copyfile(here/"AtlasMonitor.java",home.parent/"AtlasMonitor.java")
main = pkg/"redesign/app/ui/MainActivity.kt"
patch(main,"        super.onCreate(savedInstanceState)","        super.onCreate(savedInstanceState)\n        com.protonvpn.android.redesign.home_screen.ui.AtlasMonitor.install(applicationContext)")
patch(pkg/"telemetry/IsVpnTelemetryEnabled.kt","effectiveCurrentUserSettings.telemetry.first()","false")
patch(pkg/"telemetry/Telemetry.kt","private suspend fun isEnabled() = userSettings.telemetry.first()","private suspend fun isEnabled() = false")
# Keep upstream account, entitlement and transport code; only package ARM64 native libs.
patch(app/"build.gradle","        applicationId appId","        applicationId appId\n        ndk { abiFilters 'arm64-v8a' }")
# Debug variant is kept for baseline compatibility; turn on shrinking only in a later validated release.
print("AtlasShield independent UI, ARM64 packaging, local monitor and two optional telemetry gates patched.")
