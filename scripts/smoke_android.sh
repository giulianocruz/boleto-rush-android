#!/usr/bin/env bash
set -euo pipefail

APK="${BOLETO_TEST_APK:-app/build/outputs/apk/debug/app-debug.apk}"
PACKAGE="br.com.proximaera.boletorush"
ACTIVITY="$PACKAGE/.MainActivity"
OUT="smoke-output"
mkdir -p "$OUT"

adb wait-for-device
adb shell wm size 1080x1920
adb install -r "$APK"
adb shell am force-stop "$PACKAGE" || true
# Certain Google API emulator images refuse to clear the log buffer.
# Do not treat that platform diagnostic limitation as an app failure.
adb logcat -c || echo 'WARN: logcat reset unavailable; continuing smoke test'
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
capture menu-rc156

# Confirm vibration preference is interactive and can be restored for the tester.
read -r x y < <(find_button_center 'VIBRAÇÃO: LIGADA')
adb shell input tap "$x" "$y"
sleep 1
read -r x y < <(find_button_center 'VIBRAÇÃO: DESLIGADA')
adb shell input tap "$x" "$y"
sleep 1
find_button_center 'VIBRAÇÃO: LIGADA' >/dev/null

# Verify diagnostics. On shorter screens, scroll the menu to the lower buttons.
if ! find_button_center 'DIAGNÓSTICO DO APARELHO' >/dev/null 2>&1; then
  adb shell input swipe 540 1350 540 690 450
  sleep 1
fi
read -r x y < <(find_button_center 'DIAGNÓSTICO DO APARELHO')
adb shell input tap "$x" "$y"
sleep 2
find_button_center 'COMPARTILHAR' >/dev/null
capture diagnostics-rc156
read -r x y < <(find_button_center 'FECHAR')
adb shell input tap "$x" "$y"
sleep 1
if ! find_button_center 'JOGAR AGORA' >/dev/null 2>&1; then
  adb shell input swipe 540 700 540 1420 450
  sleep 1
fi
find_button_center 'JOGAR AGORA' >/dev/null

# Capture an authentic tutorial screen directly from the current app build.
read -r x y < <(find_button_center 'COMO JOGAR')
adb shell input tap "$x" "$y"
sleep 2
assert_alive
capture instructions-rc156
read -r x y < <(find_button_center 'TREINAR CONTROLES')
adb shell input tap "$x" "$y"
sleep 2
assert_alive
capture tutorial-step1-rc156
find_button_center 'TREINO RÁPIDO' >/dev/null
# Move right to collect the tutorial coin, then left to reach the shield.
adb shell input swipe 540 1050 865 1050 350
sleep 1
find_button_center 'TREINO RÁPIDO • 2/3' >/dev/null
adb shell input swipe 865 1050 210 1050 400
sleep 1
find_button_center 'TREINO RÁPIDO • 3/3' >/dev/null
capture tutorial-complete-rc156
read -r x y < <(find_button_center 'COMEÇAR PARTIDA!')
adb shell input tap "$x" "$y"
sleep 3
assert_alive
capture gameplay-rc156

# Let the actual 45s Classic gameplay finish. If the player lost, decline
# the optional test ad and capture the genuine score/results screen.
sleep 48
assert_alive
if read -r x y < <(find_button_center 'NÃO, ENCERRAR PARTIDA' 2>/dev/null); then
  adb shell input tap "$x" "$y"
  sleep 2
fi
read -r x y < <(find_button_center 'JOGAR DE NOVO')
assert_alive
capture results-rc156
adb logcat -d -t 1500 -s AndroidRuntime:E > "$OUT/crash-log.txt" || true
if grep -q 'FATAL EXCEPTION' "$OUT/crash-log.txt"; then
  cat "$OUT/crash-log.txt"
  exit 1
fi
echo "SMOKE PASSED: menu, device diagnostics, tutorial 3/3, full round, final result, no AndroidRuntime crashes — APK=$APK"
