#!/usr/bin/env bash
set -euo pipefail

APK="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="br.com.proximaera.boletorush"
ACTIVITY="$PACKAGE/.MainActivity"
OUT="smoke-output"
mkdir -p "$OUT"

adb wait-for-device
adb shell wm size 1080x1920
adb install -r "$APK"
adb shell am force-stop "$PACKAGE" || true
adb logcat -c
adb shell am start -W -n "$ACTIVITY"
sleep 8

assert_alive() {
  local process_id
  process_id="$(adb shell pidof "$PACKAGE" | tr -d '\r')"
  if [ -z "$process_id" ]; then
    echo "FAIL: Boleto Rush exited or crashed"
    adb logcat -d -t 600 -s AndroidRuntime:E ActivityManager:E > "$OUT/crash-log.txt" || true
    cat "$OUT/crash-log.txt"
    exit 1
  fi
  echo "APP_ALIVE_PID=$process_id"
}

capture() {
  local name="$1"
  adb shell screencap -p "/sdcard/$name.png"
  adb pull "/sdcard/$name.png" "$OUT/$name.png" >/dev/null
}

find_button_center() {
  local label="$1"
  adb shell uiautomator dump /sdcard/smoke.xml >/dev/null
  adb pull /sdcard/smoke.xml "$OUT/ui.xml" >/dev/null
  python3 - "$OUT/ui.xml" "$label" <<'PY'
import sys, re, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
label = sys.argv[2]
for node in root.iter('node'):
    if label in (node.get("text", "") or "") or label in (node.get("content-desc", "") or ""):
        bounds = node.get("bounds", "")
        match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print((x1 + x2)//2, (y1 + y2)//2)
            raise SystemExit(0)
raise SystemExit("UI BUTTON NOT FOUND: " + label)
PY
}

assert_alive
# First-launch Android emulator displays its own immersive-mode confirmation.
# Dismiss it before looking for Compose buttons; this is not an app crash.
adb shell uiautomator dump /sdcard/initial-window.xml >/dev/null
adb pull /sdcard/initial-window.xml "$OUT/initial-window.xml" >/dev/null
if grep -q 'immersive_cling_title' "$OUT/initial-window.xml"; then
  read -r confirm_x confirm_y < <(python3 - "$OUT/initial-window.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
for node in root.iter('node'):
    if node.get("resource-id") == "android:id/ok":
        match=re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",node.get("bounds",""))
        if match:
            x1,y1,x2,y2=map(int,match.groups())
            print((x1+x2)//2,(y1+y2)//2)
            raise SystemExit
raise SystemExit("Immersive confirmation not found")
PY
  )
  adb shell input tap "$confirm_x" "$confirm_y"
  sleep 2
fi
assert_alive
capture menu-rc15
read -r x y < <(find_button_center 'JOGAR AGORA')
adb shell input tap "$x" "$y"
sleep 3
assert_alive
capture gameplay-rc15
adb shell input keyevent KEYCODE_BACK || true
sleep 1
assert_alive
adb logcat -d -t 1200 -s AndroidRuntime:E > "$OUT/crash-log.txt" || true
if grep -q 'FATAL EXCEPTION' "$OUT/crash-log.txt"; then
  cat "$OUT/crash-log.txt"
  exit 1
fi
echo "SMOKE PASSED: main menu, game launch, and running process on Android emulator"
