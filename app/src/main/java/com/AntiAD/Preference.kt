package com.AntiAD

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class Preference(private val context: Context) {
    private var preference: SharedPreferences = context.getSharedPreferences(
        "AntiAdPrefs", Context.MODE_PRIVATE
    )

    private val APP_ENABLED = "appEnabled"
    private val APP_ENABLED_DEFAULT = true

    private val Android13NotificationPermissionGranted = "Android13NotificationPermissionGranted"
    private val Android13NotificationPermissionGrantedDefault = false

    private val HAS_REQUESTD_NOTIFICATON_POSTING_PERMISSION = "HasRequestedNotificationPostingPermission"
    private val HAS_REQUESTED_NOTIFICATION_POSTING_PERMISSION_DEFAULT = false

    private val EnableNotifications = "EnableNotifications"
    private val EnableNotificationsDefault = false

    private val HAS_DISABLED_HIBERNATION = "HasDisabledHibernation"
    private val HAS_DISABLED_HIBERNATION_DEFAULT = false

    private val DEBUG_LOG_ENABLED = "DebugLogEnabled"
    private val DEBUG_LOG_ENABLED_DEFAULT = false

    private val MUTE_ENTIRE_DEVICE = "MuteEntireDevice"
    private val MUTE_ENTIRE_DEVICE_DEFAULT = false

    private val FORCE_MUTE_NO_CHECK = "ForceMuteNoCheck"
    private val FORCE_MUTE_NO_CHECK_DEFAULT = true

    private val FORCE_STOP_DELAY = "ForceStopDelay"
    private val FORCE_STOP_DELAY_DEFAULT = 3

    fun isEnabled(): Boolean {
        return preference.getBoolean(APP_ENABLED, APP_ENABLED_DEFAULT)
    }

    fun setEnabled(status: Boolean) {
        preference.edit { putBoolean(APP_ENABLED, status).commit() }
    }

    fun isNotificationPostingPermissionGranted(): Boolean {
        return preference.getBoolean(Android13NotificationPermissionGranted, Android13NotificationPermissionGrantedDefault)
    }

    fun setNotificationPostingPermission(status: Boolean) {
        preference.edit { putBoolean(Android13NotificationPermissionGranted, status).commit() }
    }

    fun isNotificationPermissionRequested(): Boolean {
        return preference.getBoolean(HAS_REQUESTD_NOTIFICATON_POSTING_PERMISSION, HAS_REQUESTED_NOTIFICATION_POSTING_PERMISSION_DEFAULT)
    }

    fun setNotificationPermissionRequested(status: Boolean) {
        preference.edit { putBoolean(HAS_REQUESTD_NOTIFICATON_POSTING_PERMISSION, status).commit() }
    }

    fun isNotificationsEnabled(): Boolean {
        return preference.getBoolean(EnableNotifications, EnableNotificationsDefault)
    }

    fun setNotificationEnabled(status: Boolean) {
        preference.edit { putBoolean(EnableNotifications, status).commit() }
    }

    fun isHibernationDisabled(): Boolean {
        return preference.getBoolean(HAS_DISABLED_HIBERNATION, HAS_DISABLED_HIBERNATION_DEFAULT)
    }

    fun setHibernatonDisabledStatus(status: Boolean) {
        preference.edit { putBoolean(HAS_DISABLED_HIBERNATION, status).commit() }
    }

    fun isDebugLogEnabled(): Boolean {
        return preference.getBoolean(DEBUG_LOG_ENABLED, DEBUG_LOG_ENABLED_DEFAULT)
    }

    fun setDebugLogEnabled(status: Boolean) {
        preference.edit { putBoolean(DEBUG_LOG_ENABLED, status).commit() }
    }

    fun isMuteEntireDeviceEnabled(): Boolean {
        return preference.getBoolean(MUTE_ENTIRE_DEVICE, MUTE_ENTIRE_DEVICE_DEFAULT)
    }

    fun setMuteEntireDeviceEnabled(status: Boolean) {
        preference.edit { putBoolean(MUTE_ENTIRE_DEVICE, status).commit() }
    }

    fun isForceMuteNoCheckEnabled(): Boolean {
        return preference.getBoolean(FORCE_MUTE_NO_CHECK, FORCE_MUTE_NO_CHECK_DEFAULT)
    }

    fun setForceMuteNoCheckEnabled(status: Boolean) {
        preference.edit { putBoolean(FORCE_MUTE_NO_CHECK, status).commit() }
    }

    fun getForceStopDelay(): Int {
        return preference.getInt(FORCE_STOP_DELAY, FORCE_STOP_DELAY_DEFAULT)
    }

    fun setForceStopDelay(seconds: Int) {
        preference.edit { putInt(FORCE_STOP_DELAY, seconds).commit() }
    }
}