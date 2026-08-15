package com.sotext.util

import android.content.Context
import android.text.format.DateFormat
import java.util.Date
import com.sotext.domain.model.TimeFormat

fun formatTimestamp(context: Context, timestamp: Long, preference: TimeFormat): String {
    val date = Date(timestamp)
    return when (preference) {
        TimeFormat.TWENTY_FOUR_HOUR -> java.text.SimpleDateFormat("HH:mm, MMM d").format(date)
        TimeFormat.TWELVE_HOUR -> java.text.SimpleDateFormat("h:mm a, MMM d").format(date)
        TimeFormat.AUTO -> {
            val df = DateFormat.getMediumDateFormat(context)
            val tf = DateFormat.getTimeFormat(context)
            "${df.format(date)} ${tf.format(date)}"
        }
    }
}
