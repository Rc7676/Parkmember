#!/usr/bin/env bash
# Runs the instrumented tests on the CI emulator while feeding it a fixed GPS position.
set -u

# Keep the emulator's GPS fed with the test location (geo fix takes longitude first).
(while true; do adb emu geo fix 34.7818 32.0853 >/dev/null 2>&1; sleep 3; done) &
GPS_LOOP=$!

adb shell rm -rf /data/local/tmp/qa
./gradlew connectedDebugAndroidTest --stacktrace
RESULT=$?

kill "$GPS_LOOP" 2>/dev/null || true
mkdir -p qa-screenshots
adb pull /data/local/tmp/qa/. qa-screenshots/ || true
exit $RESULT
