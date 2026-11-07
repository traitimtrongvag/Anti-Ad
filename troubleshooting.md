# Troubleshooting

For setup instructions, see [Anti-Ad Help](help.md).

## Spotify ads are not detected

- Make sure Notification Access is enabled for Anti-Ad in Android settings. This is separate from permission to show Anti-Ad notifications.
- Open Anti-Ad and check that its main switch is on.
- Check that Spotify is posting a notification while an ad is playing. Anti-Ad can only detect ads from notification text it recognizes.
- If access appears enabled but Anti-Ad still reports it missing, switch Notification Access off and on again in Android settings, then return to Anti-Ad.

## Ads are detected, but Spotify is not interrupted

- Make sure Shizuku is installed, running, and has authorized Anti-Ad.
- Open Anti-Ad and check the Shizuku status. Restart Shizuku and authorize Anti-Ad again if needed.
- Check that Spotify is installed and can be opened normally.

## Spotify does not resume playback

After an interruption, Anti-Ad waits for Spotify to become ready and sends a playback command. If playback does not resume, open Spotify and start playback manually. This can happen when Spotify or the device delays its media session.

## Anti-Ad stops working after being idle

Allow unrestricted background or battery use for Anti-Ad in Android settings. On some phones, also allow background activity or auto-start for Anti-Ad. The names and locations vary by manufacturer.

If the problem continues, check Notification Access again. Android or the phone maker can stop background services, and Anti-Ad cannot silently restore access if Android revokes it.

## Anti-Ad's status notification is missing

The status notification is optional and is separate from Notification Access. Check Anti-Ad's notification setting and Android's notification permission for the app. On Android 13 and later, permission to post notifications can be turned off without turning off Notification Access.

## Can Anti-Ad get my Spotify account suspended?

Anti-Ad does not modify Spotify's APK, inject code into Spotify, or change its internal files. It works externally through Android's Notification Access and Shizuku to detect ad notifications and control media playback.

This is different from using a modified Spotify client or modifying Spotify's internal behavior. However, we cannot guarantee that Spotify will never take action against an account, as this depends on Spotify's Terms of Service and enforcement policies.

Use Anti-Ad at your own discretion. The project does not guarantee account safety or protection against account restrictions.

## Report a problem

If these steps do not help, [open an issue](https://github.com/traitimtrongvag/Anti-Ad/issues). Include your phone model, Android version, app version, what you expected, what happened, and the steps that reproduce the problem.

If you include debug logs, remove private information first. Do not share the contents of personal notifications.
