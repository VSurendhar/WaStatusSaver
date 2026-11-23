# How to Set Up the Project

## Requirements

- Android Studio with support for Android Gradle Plugin 8.12.3
- JDK 17 or newer
- Android SDK Platform 36
- A device or emulator running Android 10 (API 29) or newer

## Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/VSurendhar/Whatsapp-Status-Saver.git
   ```

2. Open the `Whatsapp-Status-Saver` folder in Android Studio and let Gradle sync.
3. Run the `app` configuration, or build from the command line:

   ```bash
   ./gradlew assembleDebug
   ```

## Configure secrets

Debug builds work without this step. For analytics and release builds, create `secrets.properties` in the project root:

```properties
mixpanel_token=your_mixpanel_project_token
SIGNING_KEY_ALIAS=your_key_alias
SIGNING_KEY_PASS=your_key_password
SIGNING_STORE_PASS=your_keystore_password
```

If `mixpanel_token` is not set, the app does not crash, but Mixpanel analytics fails silently and no events are recorded.

## Build a release

Place the keystore in the project root as `waStatusSaver.jks`, then run:

```bash
./gradlew assembleRelease   # APK
./gradlew bundleRelease     # App Bundle
```

Do not commit `secrets.properties` or the keystore.
