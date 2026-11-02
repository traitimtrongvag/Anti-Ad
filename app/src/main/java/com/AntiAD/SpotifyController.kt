package com.AntiAD

import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.util.Log

object SpotifyController {
    private val TAG = "SpotifyController"
    private const val SPOTIFY_PACKAGE = "com.spotify.music"

    enum class State {
        UNKNOWN,
        PLAYING_MUSIC,
        PLAYING_AD,
        PAUSED,
        STOPPED,
        NOT_RUNNING
    }

    interface MediaControllerCallback {
        fun onControllerReady()
        fun onTimeout()
    }

    fun waitForMediaController(context: Context, handler: Handler, callback: MediaControllerCallback, maxAttempts: Int = 15, delayMs: Long = 400) {
        var attempts = 0
        val retryRunnable = object : Runnable {
            override fun run() {
                attempts++
                val controller = getSpotifyMediaController(context)
                if (controller != null) {
                    callback.onControllerReady()
                } else if (attempts < maxAttempts) {
                    handler.postDelayed(this, delayMs)
                } else {
                    callback.onTimeout()
                }
            }
        }
        handler.post(retryRunnable)
    }

    fun openSpotify(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
                if (Preference(context).isDebugLogEnabled()) {
                    LogManager.addLifecycleLog(LogEntry(
                        appName = "Anti-Ad",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Spotify Launched",
                        text = "Launched Spotify via launch intent",
                        subText = "Action"
                    ))
                }
                true
            } else {
                Log.w(TAG, "Spotify not installed or no launch intent")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Spotify", e)
            false
        }
    }

    fun resumePlayback(context: Context): Boolean {
        return try {
            val mediaController = getSpotifyMediaController(context)
            if (mediaController != null) {
                mediaController.transportControls.play()
                if (Preference(context).isDebugLogEnabled()) {
                    LogManager.addLifecycleLog(LogEntry(
                        appName = "Anti-Ad",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Playback Resume",
                        text = "Sent play command via MediaController",
                        subText = "Action"
                    ))
                }
                true
            } else {
                Log.w(TAG, "No MediaController available for Spotify, launching app")
                openSpotify(context)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming playback", e)
            openSpotify(context)
            false
        }
    }

    fun pausePlayback(context: Context): Boolean {
        return try {
            val mediaController = getSpotifyMediaController(context)
            if (mediaController != null) {
                mediaController.transportControls.pause()
                true
            } else {
                Log.w(TAG, "No MediaController available for Spotify")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback", e)
            false
        }
    }

    fun getPlaybackState(context: Context): State {
        return try {
            val mediaController = getSpotifyMediaController(context)
            if (mediaController != null) {
                val playbackState = mediaController.playbackState
                val metadata = mediaController.metadata
                if (playbackState != null) {
                    when (playbackState.state) {
                        PlaybackState.STATE_PLAYING -> {
                            // Check if the metadata title suggests an ad
                            val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: ""
                            if (isAdTitle(title)) {
                                State.PLAYING_AD
                            } else {
                                State.PLAYING_MUSIC
                            }
                        }
                        PlaybackState.STATE_PAUSED -> State.PAUSED
                        PlaybackState.STATE_STOPPED -> State.STOPPED
                        PlaybackState.STATE_BUFFERING -> {
                            // Could be loading an ad or track
                            State.PLAYING_MUSIC
                        }
                        else -> State.UNKNOWN
                    }
                } else {
                    State.UNKNOWN
                }
            } else {
                State.NOT_RUNNING
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting playback state", e)
            State.UNKNOWN
        }
    }

    fun getSpotifyMediaController(context: Context): MediaController? {
        return try {
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
            val controllers = sessionManager.getActiveSessions(null)
            for (controller in controllers) {
                if (controller.packageName == SPOTIFY_PACKAGE) {
                    return controller
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting MediaController", e)
            null
        }
    }

    fun isSpotifyInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SPOTIFY_PACKAGE, 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isAdTitle(title: String): Boolean {
        if (title.isBlank()) return false
        val adKeywords = arrayOf(
            "Advertisement", "Advertensie", "Werbung", "Publicité", "Pubblicità",
            "広告", "广告", "廣告", "광고", "Реклама", "Anuncio", "Anúncio",
            "Mainos", "Reklama", "Reklam", "Annonse", "Iklan", "Διαφήμιση",
            "Advertentie", "Reklame", "Oglas", "Auglýsing", "علن", "إعلان",
            "إشهار", "पब्लिसिटी", "विज्ञापन", "باز یابی", "오디오 광고",
            "Quảng cáo"
        )
        return adKeywords.any { title.equals(it, ignoreCase = true) || title.contains(it, ignoreCase = true) }
    }
}