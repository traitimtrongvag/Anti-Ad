package com.AntiAD.logging

import com.AntiAD.LogEntry
import com.AntiAD.LogManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogManagerTest {
    @Before
    fun clearLogsBeforeTest() {
        LogManager.clearLogs()
    }

    @After
    fun clearLogsAfterTest() {
        LogManager.clearLogs()
    }

    @Test
    fun retainsLatestHundredEntriesInDescendingTimestampOrder() {
        repeat(101) { index ->
            LogManager.addLog(entry(title = "log-$index", timestamp = index.toLong()))
        }

        val logs = LogManager.getLogs()
        assertEquals(100, logs.size)
        assertEquals("log-100", logs.first().title)
        assertEquals("log-1", logs.last().title)
        assertTrue(logs.zipWithNext().all { (newer, older) -> newer.timestamp >= older.timestamp })
    }

    @Test
    fun combinesRegularAndLifecycleLogsByTimestamp() {
        LogManager.addLog(entry(title = "regular", timestamp = 2))
        LogManager.addLifecycleLog(entry(title = "lifecycle", timestamp = 3))

        assertEquals(
            listOf("lifecycle", "regular"),
            LogManager.getLogs().map { it.title }
        )
    }

    private fun entry(title: String, timestamp: Long) = LogEntry(
        appName = "test",
        timestamp = timestamp,
        isAd = false,
        title = title,
        text = "",
        subText = ""
    )
}
