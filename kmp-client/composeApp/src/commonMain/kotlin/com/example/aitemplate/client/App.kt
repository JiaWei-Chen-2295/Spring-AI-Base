package com.example.aitemplate.client

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import com.example.aitemplate.client.di.appModule
import com.example.aitemplate.client.di.networkModule
import com.example.aitemplate.client.ui.screen.settings.SettingsScreen
import com.example.aitemplate.client.ui.theme.AppTheme
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = {
        modules(networkModule, appModule)
    }) {
        AppTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Navigator(SettingsScreen())
            }
        }
    }
}
