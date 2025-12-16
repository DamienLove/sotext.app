package com.pulselink.beacon.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.data.SmsRepository
import com.pulselink.beacon.data.SmsThreadItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SmsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SmsRepository(app.applicationContext)

    var threads by mutableStateOf<List<SmsThreadItem>>(emptyList())
        private set
    var messages by mutableStateOf<List<SmsMessageItem>>(emptyList())
        private set
    var currentThreadId by mutableStateOf<Long?>(null)
        private set
    var currentAddress by mutableStateOf("")
        private set

    init {
        refreshThreads()
        viewModelScope.launch {
            repo.changes().collectLatest {
                refreshThreads()
                currentThreadId?.let { refreshThread(it, refreshRead = false) }
            }
        }
    }

    fun refreshThreads() {
        threads = runCatching { repo.listThreads() }.getOrElse { emptyList() }
    }

    fun openThread(threadId: Long, address: String) {
        currentThreadId = threadId
        currentAddress = address
        refreshThread(threadId, refreshRead = true)
    }

    private fun refreshThread(threadId: Long, refreshRead: Boolean) {
        messages = runCatching { repo.messagesForThread(threadId) }.getOrElse { emptyList() }
        if (refreshRead) runCatching { repo.markThreadRead(threadId) }
    }

    fun sendMessage(body: String): Boolean {
        val addr = currentAddress.ifBlank { messages.lastOrNull()?.address.orEmpty() }
        if (addr.isBlank()) return false
        val ok = runCatching { repo.sendSms(addr, body) }.getOrDefault(false)
        if (ok) currentThreadId?.let { refreshThread(it, refreshRead = true) }
        return ok
    }

    fun deleteThread(threadId: Long) {
        repo.deleteThread(threadId)
        if (currentThreadId == threadId) {
            currentThreadId = null
            messages = emptyList()
        }
        refreshThreads()
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SmsViewModel(app) as T
            }
        }
    }
}
