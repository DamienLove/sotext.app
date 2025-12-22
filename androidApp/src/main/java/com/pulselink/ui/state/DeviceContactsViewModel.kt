package com.pulselink.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulselink.data.contacts.DeviceContact
import com.pulselink.data.contacts.DeviceContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DeviceContactsViewModel @Inject constructor(
    private val deviceContactsRepository: DeviceContactsRepository
) : ViewModel() {
    private val _contacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val contacts: StateFlow<List<DeviceContact>> = _contacts

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _contacts.value = deviceContactsRepository.listPhoneContacts()
        }
    }
}
