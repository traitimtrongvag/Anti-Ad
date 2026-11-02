package com.AntiAD

data class LogEntry(
    val appName: String,
    val timestamp: Long,
    val isAd: Boolean,
    val title: String,
    val text: String,
    val subText: String,
    val matchedText: String? = null,
    val isExpanded: Boolean = false
)

object LogManager {
    private val logs = java.util.LinkedList<LogEntry>()
    private val lifecycleLogs = java.util.LinkedList<LogEntry>()
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()

    fun addLog(entry: LogEntry) {
        synchronized(logs) {
            logs.addFirst(entry)
            if (logs.size > 100) {
                logs.removeLast()
            }
        }
        notifyListeners()
    }

    fun addLifecycleLog(entry: LogEntry) {
        synchronized(lifecycleLogs) {
            lifecycleLogs.addFirst(entry)
            if (lifecycleLogs.size > 100) {
                lifecycleLogs.removeLast()
            }
        }
        notifyListeners()
    }

    fun getLogs(): List<LogEntry> {
        val allLogs = ArrayList<LogEntry>()
        synchronized(logs) { allLogs.addAll(logs) }
        synchronized(lifecycleLogs) { allLogs.addAll(lifecycleLogs) }
        allLogs.sortByDescending { it.timestamp }
        return allLogs
    }

    fun clearLogs() {
        synchronized(logs) { logs.clear() }
        synchronized(lifecycleLogs) { lifecycleLogs.clear() }
        notifyListeners()
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }
}