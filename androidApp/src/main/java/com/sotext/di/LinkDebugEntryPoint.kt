package com.sotext.di

import com.sotext.data.link.ContactLinkManager
import com.sotext.domain.repository.ContactRepository
import com.sotext.domain.repository.MessageRepository
import com.sotext.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LinkDebugEntryPoint {
    fun contactRepository(): ContactRepository
    fun linkManager(): ContactLinkManager
    fun settingsRepository(): SettingsRepository
    fun messageRepository(): MessageRepository
}
