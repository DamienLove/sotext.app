package com.sotext.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sotext.data.emergency.EmergencyLocationRepository
import com.sotext.domain.model.EmergencyLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class EmergencyMapViewModel @Inject constructor(
    private val repository: EmergencyLocationRepository
) : ViewModel() {

    private val _locations = MutableStateFlow<List<EmergencyLocation>>(emptyList())
    val locations: StateFlow<List<EmergencyLocation>> = _locations.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeActive().collect { items ->
                _locations.value = items.filter { it.clearedAt == null && it.severity == EmergencyLocationRepository.SEVERITY_EMERGENCY }
            }
        }
    }

    fun clearLocation(id: String) {
        viewModelScope.launch {
            repository.clearLocation(id)
        }
    }
}
