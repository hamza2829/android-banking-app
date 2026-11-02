#!/usr/bin/env bash
set -euo pipefail

# The emulator runner shuts down its emulator on return. Always retrieve evidence first.
collect_evidence() {
  local result=$?
  mkdir -p evidence/screenshots
  adb pull /sdcard/Download/pocket-demo-screenshots/. evidence/screenshots/ || true
  if [ "$result" -ne 0 ]; then
    adb logcat -d -t 1500 > evidence/device-logcat.txt || true
    adb shell uiautomator dump /sdcard/failure-window.xml || true
    adb pull /sdcard/failure-window.xml evidence/device-window.xml || true
    adb exec-out screencap -p > evidence/device-failure.png || true
  fi
  exit "$result"
}
trap collect_evidence EXIT

adb shell locksettings set-pin 1234
adb shell settings put system screen_off_timeout 1800000
adb shell wm dismiss-keyguard
./gradlew connectedDebugAndroidTest --stacktrace
