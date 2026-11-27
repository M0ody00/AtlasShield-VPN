#!/usr/bin/env python3
"""Minimal reproducible identity patch for an independent upstream test build."""
from pathlib import Path
import sys

root = Path(sys.argv[1])
strings = root / "app/src/main/res/values/strings.xml"
old = '<string name="app_name" translatable="false">Proton VPN</string>'
new = '<string name="app_name" translatable="false">AtlasShield Preview</string>'
s = strings.read_text(encoding="utf-8")
if s.count(old) != 1:
    raise SystemExit("Unexpected upstream app_name resource")
strings.write_text(s.replace(old, new), encoding="utf-8")
print("Preview app label patched; upstream notices and security code preserved")
