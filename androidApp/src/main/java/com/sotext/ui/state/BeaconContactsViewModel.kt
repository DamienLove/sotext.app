package com.sotext.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sotext.data.contacts.DeviceContact
import com.sotext.data.contacts.DeviceContactsRepository
import com.sotext.domain.repository.ContactRepository
import com.sotext.domain.model.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BeaconContactsViewModel @Inject constructor(
    private val deviceContactsRepository: DeviceContactsRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val contacts: StateFlow<List<DeviceContact>> = _contacts

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _contacts.value = deviceContactsRepository.listPhoneContacts()
        }
    }

    fun resolveContactAndNavigate(
        deviceContact: DeviceContact,
        onNavigate: (Long) -> Unit
    ) {
        viewModelScope.launch {
            // Check if we already have this contact in our PulseLink DB
            val existing = contactRepository.getByPhone(deviceContact.phoneNumber)
            if (existing != null) {
                onNavigate(existing.id)
            } else {
                // Create a temporary or permanent contact record
                val newContact = Contact(
                    displayName = deviceContact.displayName,
                    phoneNumber = deviceContact.phoneNumber
                )
                contactRepository.upsert(newContact)
                val persisted = contactRepository.getByPhone(deviceContact.phoneNumber)
                persisted?.let { onNavigate(it.id) }
            }
        }
    }
}
