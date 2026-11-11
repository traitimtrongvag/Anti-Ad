package com.AntiAD

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.text.format.DateFormat
import java.util.Date

class LogAdapter(private var logs: List<LogEntry>) : BaseAdapter() {

    override fun getCount(): Int = logs.size

    override fun getItem(position: Int): LogEntry = logs[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: LogViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
            holder = LogViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as LogViewHolder
        }

        val log = getItem(position)
        val context = parent.context

        val timeFormat = DateFormat.getTimeFormat(context)
        val timeString = timeFormat.format(Date(log.timestamp))
        holder.appNameTimestamp.text = "${log.appName} $timeString"

        if (log.appName == "Anti-Ad") {
            holder.statusText.text = "Service"
            holder.statusText.setTextColor(holder.defaultStatusTextColor)
        } else if (log.isAd) {
            holder.statusText.text = "Advertisement Detected"
            holder.statusText.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_green_light))
        } else {
            holder.statusText.text = "Clean"
            holder.statusText.setTextColor(holder.defaultStatusTextColor)
        }

        holder.logTitle.text = "Title: ${log.title}"
        holder.logText.text = "Text: ${log.text}"
        holder.logSubtext.text = "SubText: ${log.subText}"

        if (!log.matchedText.isNullOrEmpty()) {
            holder.matchedText.visibility = View.VISIBLE
            holder.matchedText.text = "Checked Text: ${log.matchedText}"
        } else {
            holder.matchedText.visibility = View.GONE
        }

        if (log.isExpanded) {
            holder.detailsContainer.visibility = View.VISIBLE
            holder.expandIcon.rotation = 180f
        } else {
            holder.detailsContainer.visibility = View.GONE
            holder.expandIcon.rotation = 0f
        }

        return view
    }

    class LogViewHolder(view: View) {
        val appIcon: ImageView = view.findViewById(R.id.app_icon)
        val appNameTimestamp: TextView = view.findViewById(R.id.app_name_timestamp)
        val statusText: TextView = view.findViewById(R.id.status_text)
        val defaultStatusTextColor: Int = statusText.currentTextColor
        val expandIcon: ImageView = view.findViewById(R.id.expand_icon)
        val detailsContainer: LinearLayout = view.findViewById(R.id.details_container)
        val logTitle: TextView = view.findViewById(R.id.log_title)
        val logText: TextView = view.findViewById(R.id.log_text)
        val logSubtext: TextView = view.findViewById(R.id.log_subtext)
        val matchedText: TextView = view.findViewById(R.id.matched_text)
    }

    fun updateLogs(newLogs: List<LogEntry>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}