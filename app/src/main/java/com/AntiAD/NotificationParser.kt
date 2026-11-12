package com.AntiAD

import android.app.Notification

data class AppNotificationInfo(
    val notification: Notification,
    val packageName: String,
)

class NotificationParser(val appNotification: AppNotificationInfo) {
    var lastLogEntry: LogEntry? = null

    fun isAd(): Boolean {
        val title = appNotification.notification.extras?.get("android.title")?.toString() ?: ""
        val text = appNotification.notification.extras?.get("android.text")?.toString() ?: ""
        val subText = appNotification.notification.extras?.get("android.subText")?.toString() ?: ""
        val matchedText = findAdMatch(title, text, subText) ?: ""
        val isAd = matchedText.isNotEmpty()

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
        return AD_KEYWORDS
    }
}

internal fun findAdMatch(title: String, text: String, subText: String): String? {
    val exactTitleMatch = AD_KEYWORDS.firstOrNull { title.isNotEmpty() && title == it }
    if (exactTitleMatch != null) return exactTitleMatch
    if (subText.contains("Advertisement")) return subText
    if (text.contains("Advertisement")) return text

    fun matchesKeyword(field: String) = field != "null" && field.isNotEmpty() &&
        AD_KEYWORDS.any { field.contains(it, ignoreCase = true) }

    return when {
        matchesKeyword(subText) -> subText
        matchesKeyword(title) -> title
        matchesKeyword(text) -> text
        else -> null
    }
}

private val AD_KEYWORDS = listOf(
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