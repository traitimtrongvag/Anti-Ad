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

## Spotify restarts the previous song after an ad

Spotify may need a few seconds to stabilize after an ad starts. If Anti-Ad interrupts it too soon, Spotify may resume the song that was playing before the ad. In Anti-Ad, open **Settings > Force-Stop Delay** and increase the delay from the default 3 seconds to 4 or 5 seconds.

## Spotify does not resume playback

After an interruption, Anti-Ad waits for Spotify to become ready and sends a playback command. If playback does not resume, open Spotify and start playback manually. This can happen when Spotify or the device delays its media session.

## Anti-Ad stops working after being idle

Allow unrestricted background or battery use for Anti-Ad in Android settings. On some phones, also allow background activity or auto-start for Anti-Ad. The names and locations vary by manufacturer.

If the problem continues, check Notification Access again. Android or the phone maker can stop background services, and Anti-Ad cannot silently restore access if Android revokes it.

## Anti-Ad's status notification is missing

The status notification is optional and is separate from Notification Access. Check Anti-Ad's notification setting and Android's notification permission for the app. On Android 13 and later, permission to post notifications can be turned off without turning off Notification Access.

## Report a problem

If these steps do not help, [open an issue](https://github.com/traitimtrongvag/Anti-Ad/issues). Include your phone model, Android version, app version, what you expected, what happened, and the steps that reproduce the problem.

If you include debug logs, remove private information first. Do not share the contents of personal notifications.
