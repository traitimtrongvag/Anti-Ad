package com.AntiAD

internal class ActiveAdNotifications {
    private val keys = mutableSetOf<String>()

    fun update(key: String, isAd: Boolean): Boolean {
        if (isAd) keys.add(key) else keys.remove(key)
        return hasActiveAds()
    }

    fun remove(key: String): Boolean {
        keys.remove(key)
        return hasActiveAds()
    }

    fun replace(activeKeys: Collection<String>): Boolean {
        keys.clear()
        keys.addAll(activeKeys)
        return hasActiveAds()
    }

    fun clear() = keys.clear()

    fun hasActiveAds(): Boolean = keys.isNotEmpty()
}
