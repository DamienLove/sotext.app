package com.pulselink.beacon.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulselink.beacon.data.InboxIconVariant
import com.pulselink.beacon.data.ThemeFont
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.data.ThemePreferencesRepository
import com.pulselink.beacon.data.ThemeState
import com.pulselink.beacon.data.ThemeTarget
import kotlinx.coroutines.launch

class ThemeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ThemePreferencesRepository(app.applicationContext)

    var themeState by mutableStateOf(ThemeState())
        private set

    init {
        viewModelScope.launch {
            repo.flow.collect { themeState = it }
        }
    }

    fun applyColor(target: ThemeTarget, color: Color, address: String?) {
        val base = themeState.forAddress(address)
        val updated = base.withTarget(target, color)
        persist(address, updated)
    }

    fun applyFont(font: ThemeFont, address: String?) {
        val base = themeState.forAddress(address)
        persist(address, base.copy(font = font))
    }

    fun applyRadius(radius: Float, address: String?) {
        val base = themeState.forAddress(address)
        persist(address, base.copy(bubbleRadius = radius.coerceIn(4f, 28f)))
    }

    fun applyPreset(preset: ThemePalette, address: String?) {
        persist(address, preset)
    }

    fun resetContact(address: String) {
        viewModelScope.launch { repo.saveContact(address, null) }
    }

    fun updateIconVariant(variant: InboxIconVariant) {
        val updated = themeState.global.copy(iconVariant = variant)
        persist(null, updated)
    }

    private fun persist(address: String?, updated: ThemePalette) {
        viewModelScope.launch {
            if (address.isNullOrBlank()) {
                repo.saveGlobal(updated)
            } else {
                repo.saveContact(address, updated)
            }
        }
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ThemeViewModel(app) as T
            }
        }
    }
}
