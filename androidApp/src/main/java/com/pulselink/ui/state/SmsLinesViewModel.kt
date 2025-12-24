package com.pulselink.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulselink.data.sms.SmsLineRepository
import com.pulselink.domain.model.LineInboxMode
import com.pulselink.domain.model.LineSendPreference
import com.pulselink.domain.model.SmsLine
import com.pulselink.domain.model.SmsLineDevice
import com.pulselink.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class SmsLinesViewModel @Inject constructor(
    private val smsLineRepository: SmsLineRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _lines = MutableStateFlow<List<SmsLine>>(emptyList())
    val lines: StateFlow<List<SmsLine>> = _lines
    private val _devices = MutableStateFlow<List<SmsLineDevice>>(emptyList())
    val devices: StateFlow<List<SmsLineDevice>> = _devices
    private val _deviceLineId = MutableStateFlow("")
    val deviceLineId: StateFlow<String> = _deviceLineId
    private val _activeLineId = MutableStateFlow<String?>(null)
    val activeLineId: StateFlow<String?> = _activeLineId
    private val _inboxMode = MutableStateFlow(LineInboxMode.COMBINED)
    val inboxMode: StateFlow<LineInboxMode> = _inboxMode
    private val _defaultSendLineId = MutableStateFlow<String?>(null)
    val defaultSendLineId: StateFlow<String?> = _defaultSendLineId
    private val _lineSendPreference = MutableStateFlow(LineSendPreference.LAST_USED)
    val lineSendPreference: StateFlow<LineSendPreference> = _lineSendPreference
    private val _devicePhoneNumber = MutableStateFlow<String?>(null)
    val devicePhoneNumber: StateFlow<String?> = _devicePhoneNumber
    private val _threadLineOverrides = MutableStateFlow<Map<String, String>>(emptyMap())
    val threadLineOverrides: StateFlow<Map<String, String>> = _threadLineOverrides

    init {
        smsLineRepository.observeLines()
            .onEach { _lines.value = it.filter { line -> !line.disabled } }
            .launchIn(viewModelScope)

        smsLineRepository.observeDevices()
            .onEach { _devices.value = it }
            .launchIn(viewModelScope)

        var lastRegisteredPhone: String? = null
        var lastRegisteredDeviceId: String? = null
        settingsRepository.settings
            .onEach { settings ->
                _deviceLineId.value = settings.deviceId
                _activeLineId.value = settings.activeLineId ?: settings.deviceId
                _inboxMode.value = settings.lineInboxMode
                _defaultSendLineId.value = settings.defaultSendLineId
                _lineSendPreference.value = settings.lineSendPreference
                _devicePhoneNumber.value = settings.devicePhoneNumber
                _threadLineOverrides.value = settings.threadLineOverrides
                val phone = settings.devicePhoneNumber
                    ?: settingsRepository.getLastKnownPhone()
                val deviceId = settings.deviceId
                if (deviceId.isNotBlank() && deviceId != lastRegisteredDeviceId) {
                    val ensured = smsLineRepository.ensureDeviceLine(phone)
                    if (ensured != null) {
                        lastRegisteredDeviceId = deviceId
                        if (!phone.isNullOrBlank()) {
                            lastRegisteredPhone = phone
                        }
                    }
                } else if (!phone.isNullOrBlank() && phone != lastRegisteredPhone) {
                    val ensured = smsLineRepository.ensureDeviceLine(phone)
                    if (ensured != null) {
                        lastRegisteredPhone = phone
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun setInboxMode(mode: LineInboxMode) {
        viewModelScope.launch {
            settingsRepository.setLineInboxMode(mode, chosen = true)
        }
    }

    fun setActiveLineId(lineId: String?) {
        viewModelScope.launch {
            settingsRepository.setActiveLineId(lineId)
        }
    }

    fun setDefaultSendLineId(lineId: String?) {
        viewModelScope.launch {
            settingsRepository.setDefaultSendLineId(lineId)
        }
    }

    fun setLineSendPreference(preference: LineSendPreference) {
        viewModelScope.launch {
            settingsRepository.setLineSendPreference(preference)
        }
    }

    fun setDevicePhoneNumber(phone: String?) {
        viewModelScope.launch {
            settingsRepository.setDevicePhoneNumber(phone)
            smsLineRepository.ensureDeviceLine(phone)
        }
    }

    fun setThreadLineOverride(threadKey: String, lineId: String?) {
        viewModelScope.launch {
            settingsRepository.setThreadLineOverride(threadKey, lineId)
        }
    }

    fun touchPresence(activeLineId: String?) {
        viewModelScope.launch {
            val phone = _devicePhoneNumber.value
            smsLineRepository.updateDevicePresence(activeLineId, phone)
        }
    }

    fun disableLine(lineId: String) {
        viewModelScope.launch {
            smsLineRepository.setLineDisabled(lineId, true)
        }
    }
}
