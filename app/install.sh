adb devices
adb install -r app-release.apk
adb shell am start -n "com.duy.screenfilter/com.duy.screenfilter.activities.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
