# How to build the APK

## In Android Studio
1. Open the project folder.
2. Wait for Gradle sync to finish.
3. Install any requested SDK packages.
4. Build a debug APK from:
   **Build -> Build APK(s)**
5. Android Studio will show a notification with the output path.

Typical debug APK path:
`app/build/outputs/apk/debug/app-debug.apk`

## Signed release APK
1. Open:
   **Build -> Generate Signed Bundle / APK**
2. Choose **APK**
3. Select or create a keystore
4. Choose the **release** build variant
5. Finish the wizard

Typical release APK path:
`app/build/outputs/apk/release/app-release.apk`

## If Gradle does not sync
- Check that Android SDK Platform 34 is installed
- Check that Build Tools for API 34 are installed
- Use the JDK bundled with Android Studio or Java 17
