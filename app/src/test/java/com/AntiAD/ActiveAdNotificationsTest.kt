package com.AntiAD

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveAdNotificationsTest {
    @Test
    fun remainsActiveUntilAllAdNotificationsEnd() {
        val notifications = ActiveAdNotifications()

        assertTrue(notifications.update("first", isAd = true))
        assertTrue(notifications.update("second", isAd = true))
        assertTrue(notifications.update("first", isAd = false))
        assertFalse(notifications.remove("second"))
    }

    @Test
    fun replacesTrackedNotificationsWhenRefreshing() {
        val notifications = ActiveAdNotifications()
        notifications.update("stale", isAd = true)

        assertTrue(notifications.replace(listOf("current")))
        assertFalse(notifications.replace(emptyList()))
    }
}
