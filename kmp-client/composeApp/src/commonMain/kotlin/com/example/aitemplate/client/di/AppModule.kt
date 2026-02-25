package com.example.aitemplate.client.di

import com.example.aitemplate.client.data.repository.ChatRepository
import com.example.aitemplate.client.data.repository.MetadataRepository
import com.example.aitemplate.client.ui.screen.chat.ChatScreenModel
import com.example.aitemplate.client.ui.screen.settings.SettingsScreenModel
import com.russhwolf.settings.Settings
import org.koin.dsl.module

val appModule = module {
    single { Settings() }
    single { MetadataRepository(get()) }
    single { ChatRepository(get(), get(), get()) }
    factory { ChatScreenModel(get(), get(), get()) }
    factory { SettingsScreenModel(get()) }
}
