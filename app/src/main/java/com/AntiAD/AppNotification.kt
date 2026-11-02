package com.AntiAD

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.RequiresApi
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

const val NOTIFICATION_CHANNEL_ID = "anti_ad_service"
const val NOTIFICATION_ID = 69

class AppNotificationHelper(val context: Context)

fun AppNotificationHelper.updateNotification(status: String): Notification {
    val notifiBuilder = createNotification(status)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createChannel()
    }
    val notification = notifiBuilder.build()
    with(NotificationManagerCompat.from(context)) {
        notify(NOTIFICATION_ID, notification)
    }
    return notification
}

fun AppNotificationHelper.getNotificationBuilder(status: String): NotificationCompat.Builder {
    val notifiBuilder = createNotification(status)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        createChannel()
    } else {
        notifiBuilder.setSound(null).setVibrate(null)
    }
    return notifiBuilder
}

private fun AppNotificationHelper.createNotification(status: String): NotificationCompat.Builder {
    val adInterruptIntent = Intent(context, AntiAdActivity::class.java)
    val pendingIntent = Build.VERSION.SDK_INT.let {
        when {
            it >= Build.VERSION_CODES.M -> PendingIntent.getActivity(
                context, 0, adInterruptIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            else -> PendingIntent.getActivity(
                context, 0, adInterruptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
    return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_ad_off)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(status)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setAutoCancel(false)
        .addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.stop),
                PendingIntent.getService(
                    context,
                    1,
                    Intent(context, NotificationListener::class.java).apply {
                        action = "STOP_SERVICE"
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            ).build()
        )
        .setContentIntent(pendingIntent)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun AppNotificationHelper.createChannel() {
    val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        context.getString(R.string.channel_name),
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = context.getString(R.string.channel_description)
        setSound(null, null)
    }
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

fun AppNotificationHelper.enable() {
    val packageManager = context.packageManager
    val componentName = ComponentName(context, NotificationListener::class.java)
    packageManager.setComponentEnabledSetting(
        componentName,
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP
    )
}