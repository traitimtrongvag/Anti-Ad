package com.AntiAD

import android.content.Context
import android.util.Log
import rikka.shizuku.Shizuku

object ShizukuManager {
    private val TAG = "ShizukuManager"
    @Volatile private var isShizukuRunning = false
    @Volatile private var isShizukuAvailable = false
    private var isInitialized = false
    @Volatile private var permissionRequestPending = false
    private var newProcessMethod: java.lang.reflect.Method? = null
    private var checkPermMethod: java.lang.reflect.Method? = null
    private var requestPermMethod: java.lang.reflect.Method? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        isShizukuAvailable = true
        isShizukuRunning = true
        if (permissionRequestPending) {
            permissionRequestPending = false
            requestPermissionIfNeeded()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        isShizukuRunning = false
        isShizukuAvailable = false
    }

    @Synchronized
    fun initialize(context: Context) {
        if (!isInitialized) {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)

            try {
                newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                newProcessMethod?.isAccessible = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to find Shizuku.newProcess method", e)
            }

            try {
                checkPermMethod = Shizuku::class.java.getMethod("checkSelfPermission")
            } catch (e: Exception) {
                Log.w(TAG, "checkSelfPermission not available", e)
            }

            try {
                requestPermMethod = Shizuku::class.java.getMethod("requestPermission", Int::class.javaPrimitiveType)
            } catch (e: NoSuchMethodException) {
                try {
                    requestPermMethod = Shizuku::class.java.getMethod("requestPermission", Int::class.javaPrimitiveType, android.os.ResultReceiver::class.java)
                } catch (e2: Exception) {
                    Log.w(TAG, "requestPermission not available", e2)
                }
            }
            isInitialized = true
        }

        try {
            isShizukuAvailable = Shizuku.pingBinder()
            isShizukuRunning = isShizukuAvailable
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku initialization failed", e)
            isShizukuAvailable = false
            isShizukuRunning = false
        }
    }

    fun isAvailable(): Boolean = isShizukuAvailable
    fun isRunning(): Boolean = isShizukuRunning

    private fun hasPermission(): Boolean {
        val method = checkPermMethod ?: return true // Shizuku versions without this API cannot be checked.
        return try {
            val result = method.invoke(null) as? Int
            result == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permission", e)
            false
        }
    }

    fun requestPermissionIfNeeded() {
        if (!Shizuku.pingBinder()) {
            permissionRequestPending = true
            return
        }
        permissionRequestPending = false
        if (!hasPermission()) {
            requestPermission()
        }
    }

    fun cancelPendingPermissionRequest() {
        permissionRequestPending = false
    }

    fun requestPermission() {
        val method = requestPermMethod ?: run {
            Log.w(TAG, "requestPermission method not available")
            return
        }
        try {
            if (method.parameterCount == 1) {
                method.invoke(null, 0)
            } else {
                method.invoke(null, 0, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
        }
    }

    private fun runCommand(cmd: String): java.lang.Process? {
        val method = newProcessMethod ?: return null

        if (!hasPermission()) {
            Log.w(TAG, "Shizuku permission not granted, requesting...")
            requestPermission()
            return null
        }

        return try {
            val result = method.invoke(null, arrayOf("sh", "-c", cmd), null, null)
            result as? java.lang.Process
        } catch (e: java.lang.reflect.InvocationTargetException) {
            val cause = e.targetException
            Log.e(TAG, "Shizuku command failed: ${cause?.message}")
            if (cause?.message?.contains("Permission Denial") == true) {
                requestPermission()
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error running command: $cmd", e)
            null
        }
    }

    fun launchSpotifyAndGoHome(): Boolean {
        // Fall back to the activity when neither service component can be started.
        return runCommand(
            "am startservice -n com.spotify.music/.spotify.MusicService 2>/dev/null " +
                "|| am startservice -n com.spotify.music/.MusicService 2>/dev/null " +
                "|| (am start -n com.spotify.music/.MainActivity --activity-no-animation " +
                "&& sleep 0.5 && input keyevent KEYCODE_HOME)"
        ) != null
    }

    fun sendToBackground() {
        runCommand("input keyevent KEYCODE_HOME")
    }

    fun sendMediaKey(code: String) {
        runCommand("input keyevent $code")
    }

    fun forceStopPackage(packageName: String): Boolean {
        if (!isRunning()) {
            Log.w(TAG, "Shizuku not running, cannot force-stop $packageName")
            return false
        }
        val process = runCommand("am force-stop $packageName")
        if (process == null) {
            Log.w(TAG, "Failed to create Shizuku process for force-stop")
            return false
        }
        return try {
            process.waitFor()
            val success = process.exitValue() == 0
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error force-stopping $packageName", e)
            false
        }
    }

    fun isPackageRunning(packageName: String): Boolean {
        if (!isRunning()) return false
        val process = runCommand("pidof $packageName")
        if (process == null) return false
        return try {
            process.waitFor()
            val output = process.inputStream.bufferedReader().readText().trim()
            output.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if $packageName is running", e)
            false
        }
    }
}