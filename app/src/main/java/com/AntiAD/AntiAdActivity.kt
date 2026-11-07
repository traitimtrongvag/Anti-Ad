package com.AntiAD

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Html.fromHtml
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat

class AntiAdActivity : Activity() {

    private val TAG = "Anti-Ad.Activity"
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private var aboutDialog: AlertDialog? = null
    private var batteryOptimizationDialog: AlertDialog? = null
    private var debugLogDialog: AlertDialog? = null
    private var settingsDialog: AlertDialog? = null
    private var shizukuDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anti_ad)
        ShizukuManager.initialize(applicationContext)
        configurePermission()
        configureToggle()
        configureAdditionalViews()
        configureBatteryOptimization()
        configureShizuku()
    }

    override fun onResume() {
        super.onResume()
        configurePermission()
        configureToggle()
        configureAdditionalViews()
        configureBatteryOptimization()
        configureShizuku()
    }

    override fun onDestroy() {
        super.onDestroy()
        listOf(
            debugLogDialog, aboutDialog, batteryOptimizationDialog,
            settingsDialog, shizukuDialog
        ).forEach { dialog ->
            if (dialog != null && dialog.isShowing) {
                dialog.dismiss()
            }
        }
    }

    private fun configurePermission() {
        notificationListenerPermission()
        configureNotificationSwitch()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPostingPermission()
        }
    }

    private fun notificationListenerPermission() {
        findViewById<Button>(R.id.grant_permission)?.run {
            when (checkNotificationListenerPermission(applicationContext)) {
                true -> {
                    this.text = getString(R.string.permission_granted)
                    this.isEnabled = false
                }
                false -> {
                    findViewById<TextView>(R.id.status_subtitle)?.text =
                        getString(R.string.app_status_permission_not_granted)
                    this.isEnabled = true
                    this.setOnClickListener {
                        val intent = Intent(getString(R.string.notification_listener_settings_intent))
                        if (intent.resolveActivity(applicationContext.packageManager) != null) {
                            startActivity(intent)
                        } else {
                            Toast.makeText(applicationContext, "Unable to open notification settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun configureNotificationSwitch() {
        val preference = Preference(applicationContext)
        val activity = this

        findViewById<Switch>(R.id.enable_notifications_switch)?.run {
            this.visibility = View.VISIBLE
            this.isChecked = preference.isNotificationsEnabled()

            this.setOnClickListener {
                val newState = !preference.isNotificationsEnabled()
                this.isChecked = newState
                preference.setNotificationEnabled(newState)

                if (newState) {
                    val intent = Intent(activity, NotificationListener::class.java)
                    intent.action = "START_SERVICE"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setNotificationPostingRequestPermission(true)
                        if (!preference.isNotificationPostingPermissionGranted()) {
                            if (preference.isNotificationPermissionRequested()) {
                                navigateToSettingsPageToGrantNotificationPostingPermission()
                            } else {
                                ActivityCompat.requestPermissions(
                                    activity,
                                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                    NOTIFICATION_PERMISSION_REQUEST_CODE
                                )
                            }
                        }
                    }
                } else {
                    val intent = Intent(activity, NotificationListener::class.java)
                    intent.action = "STOP_FOREGROUND"
                    startService(intent)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setNotificationPostingRequestPermission(false)
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            findViewById<TextView>(R.id.permission_issue_info)?.visibility = View.VISIBLE
        }
    }

    private fun notificationPostingPermission() {
        val preference = Preference(applicationContext)
        val activity = this

        if (preference.isNotificationsEnabled()) {
            setNotificationPostingRequestPermission(true)
        }

        findViewById<Button>(R.id.grant_notification_posting_permission)?.run {
            if (preference.isNotificationPostingPermissionGranted()) {
                this.text = getString(R.string.granted)
                this.isEnabled = false
            }
            this.setOnClickListener {
                when (preference.isNotificationPermissionRequested()) {
                    true -> navigateToSettingsPageToGrantNotificationPostingPermission()
                    false -> ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    private fun setNotificationPostingRequestPermission(request: Boolean) {
        val preference = Preference(applicationContext)
        preference.setNotificationPermissionRequested(request)
    }

    private fun navigateToSettingsPageToGrantNotificationPostingPermission() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun configureToggle() {
        val preference = Preference(applicationContext)
        val statusToggle = findViewById<Switch>(R.id.status_toggle)
        val appNotificationHelper = AppNotificationHelper(applicationContext)
        val statusSubtitle = findViewById<TextView>(R.id.status_subtitle)

        statusToggle.text = ""

        if (!checkNotificationListenerPermission(applicationContext)) {
            appNotificationHelper.enable()
            statusToggle.isChecked = false
            statusToggle.isEnabled = false
            statusSubtitle?.text = getString(R.string.app_status_permission_not_granted)
            return
        } else {
            statusSubtitle?.text = getString(R.string.app_status)
            statusToggle.isEnabled = true
        }

        statusToggle.setOnClickListener {
            val toChange = !preference.isEnabled()
            preference.setEnabled(toChange)

            if (toChange) {
                ShizukuManager.initialize(applicationContext)
                ShizukuManager.requestPermissionIfNeeded()
                startNotificationService()
            } else {
                val intent = Intent(this, NotificationListener::class.java)
                intent.action = "STOP_SERVICE"
                startService(intent)
            }
        }

        statusToggle.isChecked = preference.isEnabled()
        if (preference.isEnabled()) {
            startNotificationService()
        }
    }

    private fun startNotificationService() {
        val intent = Intent(this, NotificationListener::class.java)
        intent.action = "START_SERVICE"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun configureAdditionalViews() {
        val versionCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME

        findViewById<Button>(R.id.settings_btn)?.setOnClickListener {
            showSettingsDialog()
        }

        findViewById<Button>(R.id.about_btn)?.setOnClickListener {
            layoutInflater.inflate(R.layout.about, null)?.run {
                this.findViewById<Button>(R.id.github_button).setOnClickListener {
                    Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link))).run {
                        startActivity(this)
                    }
                }
                this.findViewById<Button>(R.id.report_issue_btn)?.setOnClickListener {
                    Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_issues_link))).run {
                        startActivity(this)
                    }
                }
                this.findViewById<TextView>(R.id.about_text_view).also {
                    it.movementMethod = LinkMovementMethod.getInstance()
                    it.text = fromHtml(getString(R.string.about_window_text))
                }
                this.findViewById<TextView>(R.id.tv_app_version)?.text = "$versionName ($versionCode)"

                aboutDialog = AlertDialog.Builder(this@AntiAdActivity)
                    .setView(this)
                    .create()
                aboutDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
                aboutDialog?.setOnDismissListener { aboutDialog = null }
                aboutDialog?.show()
            }
        }

        findViewById<Button>(R.id.debug_log_btn)?.setOnClickListener {
            showDebugLogDialog()
        }
    }

    private fun showSettingsDialog() {
        val settingsView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val preference = Preference(applicationContext)

        settingsView.findViewById<NumberPicker>(R.id.delay_picker)?.run {
            minValue = 1
            maxValue = 10
            value = preference.getForceStopDelay()
            setOnValueChangedListener { _, _, newVal ->
                preference.setForceStopDelay(newVal)
            }
        }

        settingsView.findViewById<Button>(R.id.btn_close_settings)?.setOnClickListener {
            settingsDialog?.dismiss()
        }

        settingsDialog = AlertDialog.Builder(this)
            .setView(settingsView)
            .create()
        settingsDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        settingsDialog?.setOnDismissListener { settingsDialog = null }
        settingsDialog?.show()
    }

    private fun showDebugLogDialog() {
        val debugView = layoutInflater.inflate(R.layout.dialog_debug_log, null)
        val preference = Preference(applicationContext)

        debugView.findViewById<Switch>(R.id.debug_log_toggle)?.run {
            isChecked = preference.isDebugLogEnabled()
            setOnClickListener {
                preference.setDebugLogEnabled(!preference.isDebugLogEnabled())
            }
        }

        val logListView = debugView.findViewById<ListView>(R.id.log_list_view)
        val logAdapter = LogAdapter(LogManager.getLogs())
        logListView.adapter = logAdapter

        logListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val logs = LogManager.getLogs().toMutableList()
            if (position < logs.size) {
                logs[position] = logs[position].copy(isExpanded = !logs[position].isExpanded)
                logAdapter.updateLogs(logs)
            }
        }

        val listener = {
            logAdapter.updateLogs(LogManager.getLogs())
        }
        LogManager.addListener(listener)

        debugView.findViewById<Button>(R.id.clear_log_btn)?.setOnClickListener {
            LogManager.clearLogs()
        }

        debugLogDialog = AlertDialog.Builder(this)
            .setView(debugView)
            .create()
        debugLogDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        debugLogDialog?.setOnDismissListener {
            LogManager.removeListener(listener)
            debugLogDialog = null
        }
        debugLogDialog?.show()
    }

    private fun configureBatteryOptimization() {
        val container = findViewById<LinearLayout>(R.id.battery_optimization_container)
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                container?.visibility = View.VISIBLE
                // Only open system dialog when user explicitly taps the switch
                findViewById<Switch>(R.id.disable_battery_optimization_switch)?.setOnClickListener {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not open battery optimization settings", e)
                    }
                }
            } else {
                container?.visibility = View.GONE
            }
        }
    }

    private fun configureShizuku() {
        findViewById<TextView>(R.id.shizuku_status)?.let { statusView ->
            when {
                ShizukuManager.isRunning() -> {
                    statusView.text = getString(R.string.shizuku_running)
                    statusView.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                }
                ShizukuManager.isAvailable() -> {
                    statusView.text = getString(R.string.shizuku_not_running)
                    statusView.setTextColor(android.graphics.Color.parseColor("#FFA000"))
                    statusView.setOnClickListener {
                        showShizukuDialog()
                    }
                }
                else -> {
                    statusView.text = getString(R.string.shizuku_not_available)
                    statusView.setTextColor(android.graphics.Color.parseColor("#FF5252"))
                    statusView.setOnClickListener {
                        showShizukuDialog()
                    }
                }
            }
        }
    }

    private fun showShizukuDialog() {
        val shizukuView = layoutInflater.inflate(R.layout.dialog_shizuku, null)
        shizukuDialog = AlertDialog.Builder(this)
            .setView(shizukuView)
            .create()
        shizukuDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        shizukuDialog?.setOnDismissListener { shizukuDialog = null }

        shizukuView.findViewById<Button>(R.id.btn_open_shizuku)?.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/")))
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open Shizuku page", Toast.LENGTH_SHORT).show()
            }
        }
        shizukuView.findViewById<Button>(R.id.btn_close_shizuku_dialog)?.setOnClickListener {
            shizukuDialog?.dismiss()
        }
        shizukuDialog?.show()
    }
}