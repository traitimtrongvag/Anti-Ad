# Contributing

Thanks for taking the time to improve Anti-Ad. Bug reports, documentation fixes, and focused code changes are welcome. For larger changes, open an issue first so the approach can be discussed before you spend time implementing it.

## What the app does

Anti-Ad listens for Spotify notifications and looks for text that identifies an advertisement. When it finds one, it uses Shizuku to interrupt Spotify, then launches Spotify again and attempts to resume playback.

The main flow lives in `app/src/main/java/com/AntiAD`:

- `NotificationListener` receives notifications and coordinates the interruption flow.
- `NotificationParser` checks notification text for ad keywords.
- `SpotifyController` finds Spotify's media session and sends playback commands.
- `ShizukuManager` checks Shizuku and runs the required privileged actions.
- `Preference` stores the user's settings.

Notification Access is needed to read Spotify notifications. Shizuku must be installed, running, and authorized for the interruption flow. Android and device manufacturers may restrict background services, so behavior should be checked on a device when a change affects service lifecycle or permissions.

## Set up a development environment

You will need:

- Android Studio or a compatible Android SDK installation.
- JDK 17.
- Android SDK Platform 35 and Build Tools 35.0.0.

Clone the repository and open it in Android Studio, or use the Gradle wrapper from the repository root. The wrapper downloads the configured Gradle version on its first run.

On macOS or Linux, make the wrapper executable if needed:

```sh
chmod +x gradlew
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Build and test

Build the debug APK:

```sh
./gradlew :app:assembleDebug
```

Run the unit tests:

```sh
./gradlew :app:testDebugUnitTest
```

The APK is written under `app/build/outputs/apk/debug/`. You can install it on a connected device or emulator with:

```sh
./gradlew :app:installDebug
```

The existing unit tests cover notification parsing and log management. If a change affects a different area, add focused tests where practical. Changes to notification access, service behavior, Shizuku, or Spotify playback should also be checked manually on a device when possible.

## Making a change

Keep changes focused and preserve existing behavior unless the issue calls for a correction. Reuse the current app structure and dependencies; avoid adding a library for a small, isolated need.

For parser changes, update or add tests in `app/src/test/java/com/AntiAD/notification/NotificationParserTest.kt`. For log handling, use `app/src/test/java/com/AntiAD/logging/LogManagerTest.kt` as a reference. Keep user-facing text in Android string resources rather than hard-coding it in views.

Before opening a pull request, run the relevant tests and build. Review your diff for unrelated changes, and note any checks you could not run.

## Reporting a problem

Open an issue with:

- The phone model and Android version.
- The app version and where it was installed from.
- What you expected, what happened, and steps to reproduce it.
- Relevant Anti-Ad debug logs, if available.

Remove private information before sharing logs. Do not include the contents of personal notifications.

## Pull requests

Use a clear title and explain the problem and how the change addresses it. Include the commands you ran and their results. For device-specific behavior, state which device and Android version you tested on.

## License

By contributing, you agree that your contributions are distributed under the project's GNU General Public License, version 3 or later. Keep existing copyright and license notices intact.
