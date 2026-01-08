package com.pulselink.beacon.ui

import com.pulselink.beacon.data.SmsMessageItem

sealed class ThreadUiItem {
    data class Message(val message: SmsMessageItem) : ThreadUiItem()
    data class DateHeader(val date: String) : ThreadUiItem()
}
