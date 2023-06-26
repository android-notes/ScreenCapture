* ./gradlew app:assembleDebug
* adb push `find . -name "app-debug.apk"` /sdcard
* adb shell 'export CLASSPATH=/sdcard/app-debug.apk && app_process xx com.example.lib.Main'
* adb pull /sdcard/1.png ./
