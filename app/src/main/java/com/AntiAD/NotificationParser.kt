package com.AntiAD

import android.app.Notification

data class AppNotificationInfo(
    val notification: Notification,
    val packageName: String,
)

class NotificationParser(val appNotification: AppNotificationInfo) {
    var lastLogEntry: LogEntry? = null
    private var matchedText: String = ""

    fun isAd(): Boolean {
        val title = appNotification.notification.extras?.get("android.title")?.toString() ?: ""
        val text = appNotification.notification.extras?.get("android.text")?.toString() ?: ""
        val subText = appNotification.notification.extras?.get("android.subText")?.toString() ?: ""

        var isAd = false

        if (title.isNotEmpty()) {
            for (adString in getAdKeywords()) {
                if (title == adString) {
                    matchedText = adString
                    isAd = true
                    break
                }
            }
        }

        if (!isAd) {
            val subTextStr = subText
            if (subTextStr.contains("Advertisement") || subTextStr == "Advertisement") {
                matchedText = subTextStr
                isAd = true
            }
        }

        if (!isAd) {
            val textStr = text
            if (textStr.contains("Advertisement") || textStr == "Advertisement") {
                matchedText = textStr
                isAd = true
            }
        }

        if (!isAd) {
            listOf(subText, title, text).forEach { field ->
                if (field != "null" && field.isNotEmpty() && !isAd) {
                    val matches = getAdKeywords().filter { field.contains(it, ignoreCase = true) }
                    if (matches.isNotEmpty()) {
                        matchedText = field
                        isAd = true
                    }
                }
            }
        }

        lastLogEntry = LogEntry(
            appName = appNotification.packageName,
            timestamp = System.currentTimeMillis(),
            isAd = isAd,
            title = title,
            text = text,
            subText = subText,
            matchedText = matchedText
        )

        return isAd
    }

    fun getAdKeywords(): List<String> {
        return listOf(
            "Advertisement",
            "Advertensie",
            "Werbung",
            "Publicité",
            "Pubblicità",
            "広告",
            "广告",
            "廣告",
            "광고",
            "Реклама",
            "Anuncio",
            "Anúncio",
            "Mainos",
            "Reklama",
            "Reklam",
            "Annonse",
            "Iklan",
            "Διαφήμιση",
            "Advertentie",
            "Reklame",
            "Oglas",
            "Auglýsing",
            "إعلان",
            "إشهار",
            "विज्ञापन",
            "पब्लिसिटी",
            "오디오 광고",
            "Quảng cáo"
        )
    }
}