package com.AntiAD

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

@SuppressLint("LongLogTag")
class NotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListenerService"
    private var appNotificationHelper: AppNotificationHelper? = null
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var audioManager: AudioManager
    private var isMutedByApp = false
    private val activeAdNotifications = ActiveAdNotifications()
    private var unmuteRunnable: Runnable? = null
    private var rebindRequested = false
    private var listenerConnected = false
    private val rebindRetryRunnable = Runnable {
        rebindRequested = false
        requestListenerRebind()
    }

    private enum class InterruptState {
        IDLE,
        LISTENING,
        AD_DETECTED,
        INTERRUPTING,
        WAITING_FOR_SPOTIFY_STOP,
        LAUNCHING_SPOTIFY,
        WAITING_FOR_SPOTIFY_READY,
        RESUMING_PLAYBACK
    }

    private var currentState = InterruptState.IDLE
    private var isProcessingAd = false
    private var lastAdElapsedRealtime: Long? = null
    private val AD_COOLDOWN_MS = 30000L // 30 seconds cooldown between interrupts
    private var spotifyLaunchAttempts = 0
    private val MAX_SPOTIFY_LAUNCH_ATTEMPTS = 3
    private var isWaitingForSpotifyStop = false
    private var isWaitingForSpotifyReady = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        startTime = System.currentTimeMillis()
        appNotificationHelper = AppNotificationHelper(applicationContext)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        ShizukuManager.initialize(applicationContext)

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "anti_ad:NotificationListenerWakeLock"
        )

        logNotificationAccessState("service created")
        logLifecycle("Service Created", "Notification Listener Service Created", "Lifecycle Event")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val preference = Preference(applicationContext)
        // Always attempt to re-initialize Shizuku in case binder was dead (e.g. after reset)
        ShizukuManager.initialize(applicationContext)

        when (intent?.action) {
            "STOP_SERVICE" -> {
                preference.setEnabled(false)
                logLifecycle("Service Stop", "Service received STOP_SERVICE. Stopping service.", "Lifecycle Event")
                requestUnbind()
                listenerConnected = false
                rebindRequested = false
                activeAdNotifications.clear()
                resetProcessingState(InterruptState.IDLE)
                stopForeground(true)
                stopSelf()
                return Service.START_NOT_STICKY
            }
            "STOP_FOREGROUND" -> {
                // The listener must remain foreground while it is enabled.
                startForegroundCompat("Anti-Ad, listening for Spotify ads")
            }
            "START_SERVICE" -> {
                logLifecycle("Service Start", "Service received START_SERVICE. Starting foreground.", "Lifecycle Event")
                // Required after startForegroundService on Android 8+ to avoid a crash/restart loop.
                // isNotificationsEnabled only controls notification visibility.
                startForegroundCompat("Anti-Ad, listening for Spotify ads")
                requestListenerRebind()
                if (!listenerConnected) currentState = InterruptState.LISTENING
            }
            else -> {
                // Service restored by START_STICKY after being killed by the system
                logLifecycle("Service Restored", "Service restored by system (START_STICKY).", "Lifecycle Event")
                startForegroundCompat("Anti-Ad, listening for Spotify ads")
                requestListenerRebind()
                if (!listenerConnected) currentState = InterruptState.LISTENING
            }
        }
        return Service.START_STICKY
    }

    private fun startForegroundCompat(msg: String) {
        appNotificationHelper?.getNotificationBuilder(msg)?.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, this.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, this.build())
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val preference = Preference(applicationContext)
        if (!checkNotificationListenerPermission(applicationContext)) {
            listenerConnected = false
            rebindRequested = false
            logNotificationAccessState("listener connected callback without enabled access")
            stopForeground(true)
            stopSelf()
            return
        }

        listenerConnected = true
        rebindRequested = false
        logNotificationAccessState("listener connected")
        if (!preference.isEnabled()) {
            activeAdNotifications.clear()
            resetProcessingState(InterruptState.IDLE)
            stopForeground(true)
            stopSelf()
            return
        }
        // Always satisfy startForeground requirement; hide notification if user disabled it
        startForegroundCompat("Anti-Ad, listening for Spotify ads")
        logLifecycle("Listener Connected", "Notification Listener Connected", "Lifecycle Event")
        resetProcessingState(InterruptState.LISTENING)
        refreshActiveAdNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        listenerConnected = false
        logNotificationAccessState("listener disconnected")
        logLifecycle("Listener Disconnected", "Notification Listener Disconnected", "Lifecycle Event")
        resetProcessingState(InterruptState.IDLE)
        if (!checkNotificationListenerPermission(applicationContext)) {
            stopForeground(true)
            stopSelf()
            return
        }
        requestListenerRebind()
    }

    override fun onDestroy() {
        super.onDestroy()
        logNotificationAccessState("service destroyed")
        instance = null

        listenerConnected = false
        activeAdNotifications.clear()
        resetProcessingState(InterruptState.IDLE)
        wakeLock = null

        logLifecycle("Service Destroyed", "Notification Listener Service Destroyed", "Lifecycle Event")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val preference = Preference(applicationContext)

        if (!preference.isEnabled()) {
            return
        }

        sbn?.let {
            if (it.packageName != SPOTIFY_PACKAGE) {
                return
            }

            with(AppNotificationInfo(it.notification, it.packageName)) {
                val parser = NotificationParser(this)
                val isAd = parser.isAd()

                if (isAd) {
                    activeAdNotifications.update(it.key, true)
                    muteForAd()
                } else if (!activeAdNotifications.update(it.key, false)) {
                    unmute()
                }

                val isDebugEnabled = preference.isDebugLogEnabled()
                if (isDebugEnabled) {
                    parser.lastLogEntry?.let { entry -> LogManager.addLog(entry) }
                }

                if (isAd) handleAdDetected(preference)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            if (it.packageName == SPOTIFY_PACKAGE) {
                if (!activeAdNotifications.remove(it.key)) scheduleUnmute()
            }
        }
    }

    private fun handleAdDetected(preference: Preference) {
        unmuteRunnable?.let(handler::removeCallbacks)
        unmuteRunnable = null

        val now = SystemClock.elapsedRealtime()
        if (lastAdElapsedRealtime?.let { now - it < AD_COOLDOWN_MS } == true || isProcessingAd) {
            return
        }

        isProcessingAd = true
        lastAdElapsedRealtime = now
        wakeLock?.acquire(INTERRUPT_WAKE_LOCK_TIMEOUT_MS)
        currentState = InterruptState.AD_DETECTED
        updateNotification("Anti-Ad, advertisement detected")
        logLifecycle("Ad Detected", "Spotify advertisement detected, music muted", "Action")
        executeInterruptFlow(preference)
    }

    private fun muteForAd() {
        if (!isMutedByApp) {
            isMutedByApp = !audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        }
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_MUTE,
            0
        )
    }

    private fun refreshActiveAdNotifications() {
        val activeAds = try {
            activeNotifications.orEmpty()
                .filter { it.packageName == SPOTIFY_PACKAGE }
                .filter { NotificationParser(AppNotificationInfo(it.notification, it.packageName)).isAd() }
                .map { it.key }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh active Spotify notifications", e)
            return
        }

        if (activeAdNotifications.replace(activeAds)) {
            muteForAd()
        } else {
            unmute()
        }
    }

    private fun scheduleUnmute() {
        unmuteRunnable?.let(handler::removeCallbacks)
        unmuteRunnable = Runnable {
            unmute()
            unmuteRunnable = null
        }
        handler.postDelayed(unmuteRunnable!!, 300)
    }

    private fun unmute() {
        if (activeAdNotifications.hasActiveAds() || !isMutedByApp) return
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_UNMUTE,
            0
        )
        isMutedByApp = false
    }

    private fun requestListenerRebind() {
        if (listenerConnected || rebindRequested ||
            !Preference(applicationContext).isEnabled() ||
            !checkNotificationListenerPermission(applicationContext)
        ) {
            return
        }
        rebindRequested = true
        try {
            requestRebind(android.content.ComponentName(this, NotificationListener::class.java))
        } catch (e: Exception) {
            rebindRequested = false
            Log.e(TAG, "requestRebind failed", e)
        }
        if (!listenerConnected) {
            handler.postDelayed(rebindRetryRunnable, REBIND_RETRY_DELAY_MS)
        }
    }

    private fun resetProcessingState(state: InterruptState) {
        handler.removeCallbacksAndMessages(null)
        releaseWakeLock()
        rebindRequested = false
        unmuteRunnable = null
        unmute()
        isProcessingAd = false
        spotifyLaunchAttempts = 0
        isWaitingForSpotifyStop = false
        isWaitingForSpotifyReady = false
        currentState = state
    }

    private fun logNotificationAccessState(event: String) {
        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "$event: notificationAccessEnabled=${checkNotificationListenerPermission(applicationContext)}, " +
                    "listenerConnected=$listenerConnected, pid=${android.os.Process.myPid()}"
            )
        }
    }

    private fun executeInterruptFlow(preference: Preference) {
        currentState = InterruptState.INTERRUPTING
        val delaySeconds = preference.getForceStopDelay()
        updateNotification("Anti-Ad, interrupting Spotify in ${delaySeconds}s")

        logLifecycle("Interrupting", "Waiting ${delaySeconds}s before force-stopping Spotify", "Action")

        handler.postDelayed({
            doForceStop(preference)
        }, delaySeconds * 1000L)
    }

    private fun doForceStop(preference: Preference) {
        currentState = InterruptState.INTERRUPTING
        updateNotification("Anti-Ad, interrupting Spotify")

        // Re-check Shizuku availability in case it just became available
        if (!ShizukuManager.isRunning()) {
            ShizukuManager.initialize(applicationContext)
        }

        if (ShizukuManager.isRunning()) {
            val stopped = ShizukuManager.forceStopPackage(SPOTIFY_PACKAGE)
            if (stopped) {
                if (isDebugEnabled(preference)) {
                    LogManager.addLifecycleLog(LogEntry(
                        appName = "Anti-Ad",
                        timestamp = System.currentTimeMillis(),
                        isAd = false,
                        title = "Spotify Stopped",
                        text = "Spotify force-stopped via Shizuku",
                        subText = "Action"
                    ))
                }
                currentState = InterruptState.WAITING_FOR_SPOTIFY_STOP
                handler.postDelayed({
                    openAndResumeSpotify(preference)
                }, 0)
            } else {
                handleError("Shizuku force-stop failed")
            }
        } else {
            handleError("Shizuku not running")
        }
    }

    private fun openAndResumeSpotify(preference: Preference) {
        currentState = InterruptState.LAUNCHING_SPOTIFY
        updateNotification("Anti-Ad, launching Spotify")

        logLifecycle("Launching Spotify", "Opening Spotify application", "Action")

        // Use Shizuku to launch Spotify and immediately go home to avoid UI flash
        val launched = if (ShizukuManager.isRunning()) {
            ShizukuManager.launchSpotifyAndGoHome()
        } else {
            SpotifyController.openSpotify(applicationContext)
        }

        if (launched) {
            currentState = InterruptState.WAITING_FOR_SPOTIFY_READY
            // Wait for Spotify to initialize, then resume
            handler.postDelayed({
                resumeSpotifyPlayback(preference)
            }, 200)
        } else {
            spotifyLaunchAttempts++
            if (spotifyLaunchAttempts < MAX_SPOTIFY_LAUNCH_ATTEMPTS) {
                handler.postDelayed({
                    openAndResumeSpotify(preference)
                }, 300)
            } else {
                handleError("Failed to launch Spotify after $MAX_SPOTIFY_LAUNCH_ATTEMPTS attempts")
            }
        }
    }

    private fun resumeSpotifyPlayback(preference: Preference) {
        currentState = InterruptState.RESUMING_PLAYBACK
        updateNotification("Anti-Ad, resuming playback")

        logLifecycle("Resuming Playback", "Waiting for Spotify to resume playback", "Action")

        // Wait for MediaController to become available (after force-stop + relaunch)
        SpotifyController.waitForMediaController(applicationContext, handler, object : SpotifyController.MediaControllerCallback {
            override fun onControllerReady() {
                pressPlayWithRetry(0)
            }

            override fun onTimeout() {
                // MediaController never appeared, try media key as fallback
                Log.w(TAG, "MediaController timed out, trying media key")
                ShizukuManager.sendMediaKey("KEYCODE_MEDIA_PLAY")
                handler.postDelayed({
                    if (SpotifyController.getPlaybackState(applicationContext) != SpotifyController.State.PLAYING_MUSIC) {
                        ShizukuManager.sendMediaKey("KEYCODE_MEDIA_PLAY")
                    }
                    finishInterruptFlow()
                }, 1000)
            }
        })
    }

    private fun pressPlayWithRetry(attempt: Int) {
        val maxAttempts = 3
        if (attempt >= maxAttempts) {
            Log.w(TAG, "Spotify did not confirm playback after $maxAttempts play attempts")
            finishInterruptFlow()
            return
        }

        SpotifyController.resumePlayback(applicationContext)
        handler.postDelayed({
            when (SpotifyController.getPlaybackState(applicationContext)) {
                SpotifyController.State.PLAYING_MUSIC -> finishInterruptFlow()
                else -> pressPlayWithRetry(attempt + 1)
            }
        }, 500)
    }

    private fun finishInterruptFlow() {
        releaseWakeLock()
        currentState = InterruptState.LISTENING
        updateNotification("Anti-Ad, listening for Spotify ads")
        spotifyLaunchAttempts = 0
        isProcessingAd = false
        unmute()
    }



    private fun handleError(message: String) {
        releaseWakeLock()
        Log.e(TAG, "Error: $message")
        logLifecycle("Error", message, "Error")
        currentState = InterruptState.LISTENING
        updateNotification("Anti-Ad, error: $message")
        spotifyLaunchAttempts = 0
        isProcessingAd = false
        unmute()
    }

    private fun updateNotification(msg: String) {
        val preference = Preference(applicationContext)
        if (preference.isNotificationsEnabled()) {
            appNotificationHelper?.updateNotification(msg)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock release failed", e)
        }
    }

    private fun logLifecycle(title: String, text: String, subText: String) {
        val preference = Preference(applicationContext)
        if (preference.isDebugLogEnabled()) {
            LogManager.addLifecycleLog(LogEntry(
                appName = "Anti-Ad",
                timestamp = System.currentTimeMillis(),
                isAd = false,
                title = title,
                text = text,
                subText = subText
            ))
        }
    }

    private fun isDebugEnabled(preference: Preference): Boolean = preference.isDebugLogEnabled()

    companion object {
        const val SPOTIFY_PACKAGE = "com.spotify.music"
        private const val REBIND_RETRY_DELAY_MS = 30_000L
        private const val INTERRUPT_WAKE_LOCK_TIMEOUT_MS = 60_000L
        @Volatile var startTime: Long = 0
        @Volatile var instance: NotificationListener? = null
    }
}