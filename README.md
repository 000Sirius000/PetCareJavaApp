# PetCare Java App

Native Android application in **Java** for managing pet profiles, reminders, health history,
activity, weight and symptom logs.

## What is included now

- Android 10+ project structure
- Java Activities, Fragments, Adapters and Room persistence layer
- Local offline storage with Room
- Multiple pet profiles with archive / recover / delete flows
- Profile photo picker from storage and camera capture flow
- Vet-visit attachment picker for images / PDF into app-private storage
- Full create/edit/delete forms for:
  - Pet profile
  - Vet visit
  - Vaccination
  - Medication
  - Feeding schedule
  - Activity session
  - Weight entry
  - Symptom entry
- Dashboard and pet detail hub
- Weight history line chart (custom view)
- Symptom frequency bar chart (custom view)
- JSON backup export/import via SAF file picker
- PDF health export via SAF destination picker
- Reminder scheduling and boot rescheduling
- Manual theme override (System / Light / Dark)

## Important note

This environment does **not** include a full Android SDK toolchain, so the project was edited
carefully but the APK itself was **not built here**. Open it in Android Studio, let Gradle sync,
and build the APK on your machine.

## How to get an APK in Android Studio

1. Open the project folder in Android Studio.
2. Wait for **Gradle Sync** to finish.
3. If Android Studio asks to install SDK components, install them.
4. Use **Build → Build APK(s)** for a debug APK.
5. After the build completes, open the link from the notification:
   `app/build/outputs/apk/debug/app-debug.apk`

## Release APK

For a release build:
1. Use **Build → Generate Signed Bundle / APK**
2. Choose **APK**
3. Create or select a keystore
4. Finish the wizard

The signed APK will usually be created in:
`app/build/outputs/apk/release/`

## Main package

`com.example.petcare`

## Core technologies

- Java
- AndroidX
- Material Design 3
- Room (SQLite)
- Gson
- Android PdfDocument
- AlarmManager exact alarms
- Storage Access Framework
- FileProvider
