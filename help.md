# Anti-Ad Help

Anti-Ad watches for Spotify notifications that look like advertisements. When it finds one, it uses Shizuku to interrupt Spotify and then tries to reopen it and resume playback.

## Getting started

1. Install Anti-Ad and open it.
2. Tap **Grant Permission** and allow Anti-Ad under Android's Notification Access settings.
3. Install and start [Shizuku](https://shizuku.rikka.app/), then authorize Anti-Ad in Shizuku.
4. Return to Anti-Ad and turn on the main switch.

The app needs Notification Access to recognize Spotify ads. Shizuku is needed to interrupt Spotify. Both must remain enabled while you use Anti-Ad.

## Keep Anti-Ad running

Some phones limit background apps to save power. If Anti-Ad stops responding after the screen has been off or the app has been unused, allow unrestricted background or battery use for Anti-Ad in your phone settings. The setting name and location depend on the phone.

Android or the phone maker may still stop background services. See [Troubleshooting](troubleshooting.md) if the app stops working.

## Notifications

The optional notification setting controls Anti-Ad's status notification. It is separate from Notification Access, which is required to detect Spotify ads.

On Android 13 and later, Android may also ask whether Anti-Ad can post notifications. This controls visible notifications from Anti-Ad; it does not grant Notification Access.

## Privacy

Anti-Ad needs Notification Access to receive notification events. It checks Spotify notifications for ad-related text. The app does not request internet access. Debug entries are kept in memory while the app process is running and are not a permanent notification history.

Only install Anti-Ad from a source you trust. Notification Access is a sensitive permission.

## Still need help?

See [Troubleshooting](troubleshooting.md). If the problem continues, [open an issue](https://github.com/traitimtrongvag/Anti-Ad/issues) with your phone model, Android version, app version, and steps to reproduce it. Remove private information from any logs and do not share notification contents.
