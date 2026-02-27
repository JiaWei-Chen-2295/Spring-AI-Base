package com.example.aitemplate.client

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.SlideTransition
import com.example.aitemplate.client.di.appModule
import com.example.aitemplate.client.di.networkModule
import com.example.aitemplate.client.ui.screen.auth.LoginScreen
import com.example.aitemplate.client.ui.screen.chat.ChatScreen
import com.example.aitemplate.client.ui.theme.*
import com.russhwolf.settings.Settings
import org.koin.compose.KoinApplication

@OptIn(ExperimentalVoyagerApi::class)
@Composable
fun App() {
    val settings = remember { Settings() }
    var themeMode by remember {
        mutableStateOf(
            when (settings.getStringOrNull("theme_mode")) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK"  -> ThemeMode.DARK
                else    -> ThemeMode.SYSTEM
            }
        )
    }

    // Check if user is already logged in
    val hasSavedAuth = remember {
        settings.getStringOrNull("access_token") != null &&
        settings.getStringOrNull("server_url") != null
    }

    KoinApplication(application = {
        modules(networkModule, appModule)
    }) {
        CompositionLocalProvider(
            LocalThemeMode provides themeMode,
            LocalSetThemeMode provides { mode ->
                themeMode = mode
                settings.putString("theme_mode", mode.name)
            }
        ) {
            AppTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val startScreen = if (hasSavedAuth) ChatScreen() else LoginScreen()
                    Navigator(
                        screen = startScreen,
                        disposeBehavior = NavigatorDisposeBehavior(disposeSteps = false)
                    ) { navigator ->
                        SlideTransition(
                            navigator = navigator,
                            disposeScreenAfterTransitionEnd = true
                        )
                    }
                }
            }
        }
    }
}
