#!/usr/bin/env bash
# Chạy trên máy ảo Android (reactivecircus/android-emulator-runner): cài APK debug, dựng lại trạng thái người dùng
# (Theo thời tiết, mưa/giông, ngày/đêm), mở app và gọi cập nhật ba widget, rồi in mọi lỗi nghiêm trọng của app.
# Thất bại (exit 1) nếu có FATAL EXCEPTION của com.vava.widgetpremium.
set +e
PKG=com.vava.widgetpremium
APK=$(ls app/build/outputs/apk/debug/*.apk | head -n 1)
FAIL=0
adb install -r "$APK" || exit 2
adb shell pm list packages | grep $PKG

seed() { # $1 mã WMO, $2 chế độ ngày/đêm
  python3 .github/smoke/seed.py /tmp/seed "$1" "$2"
  adb push /tmp/seed/weather.xml /data/local/tmp/weather.xml >/dev/null
  adb push /tmp/seed/style.xml /data/local/tmp/style.xml >/dev/null
  adb shell am force-stop $PKG
  adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat /data/local/tmp/weather.xml > shared_prefs/weather.xml && cat /data/local/tmp/style.xml > shared_prefs/style.xml'"
}

scenario() { # $1 tên, $2 mã WMO (rỗng = cài mới, không seed), $3 chế độ
  echo "=================== KỊCH BẢN: $1 ==================="
  adb shell am force-stop $PKG
  adb shell pm clear $PKG >/dev/null
  [ -n "$2" ] && seed "$2" "$3"
  adb logcat -c
  adb shell am start -W -n $PKG/.MainActivity
  sleep 8
  for p in ClockWidgetProvider InfoWidgetProvider WeekWidgetProvider; do
    adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE -n $PKG/.$p --eia appWidgetIds 1,2,3
  done
  sleep 6
  adb logcat -d > /tmp/full.txt
  echo "--- pid của app: $(adb shell pidof $PKG)"
  if grep -q "FATAL EXCEPTION" /tmp/full.txt; then
    echo "!!! CÓ CRASH trong kịch bản $1"
    grep -n "FATAL EXCEPTION" -A45 /tmp/full.txt | head -150
    FAIL=1
  else
    echo "OK: không có FATAL EXCEPTION trong kịch bản $1"
  fi
  echo "--- cảnh báo/lỗi liên quan app:"
  grep -E "(E|W) .*(widgetpremium|AndroidRuntime|RemoteViews|AppWidget)" /tmp/full.txt | head -40
}

scenario "cài mới" "" ""
scenario "mưa ban ngày" 61 DAY
scenario "mưa ban đêm" 61 NIGHT
scenario "giông ban đêm" 95 NIGHT
scenario "nắng ban ngày" 0 DAY
exit $FAIL
