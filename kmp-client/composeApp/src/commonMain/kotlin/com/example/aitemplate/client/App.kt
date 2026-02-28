package com.example.aitemplate.client

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.SlideTransition
import com.example.aitemplate.client.data.remote.MetadataApi
import com.example.aitemplate.client.di.appModule
import com.example.aitemplate.client.di.networkModule
import com.example.aitemplate.client.i18n.*
import com.example.aitemplate.client.ui.screen.auth.LoginScreen
import com.example.aitemplate.client.ui.screen.chat.ChatScreen
import com.example.aitemplate.client.ui.theme.*
import com.russhwolf.settings.Settings
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

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

    // Language preference
    var language by remember {
        mutableStateOf(
            when (settings.getStringOrNull("language")) {
                "EN" -> Language.EN
                else -> Language.ZH
            }
        )
    }

    KoinApplication(application = {
        modules(networkModule, appModule)
    }) {
        CompositionLocalProvider(
            LocalThemeMode provides themeMode,
            LocalSetThemeMode provides { mode ->
                themeMode = mode
                settings.putString("theme_mode", mode.name)
            },
            LocalLanguage provides language,
            LocalSetLanguage provides { lang ->
                language = lang
                settings.putString("language", lang.name)
            },
            LocalStrings provides getStrings(language)
        ) {
            AppTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StartNavigator(settings)
                }
            }
        }
    }
}

/**
 * Determines the start screen by checking:
 * 1. If server_url is not configured → LoginScreen (user must set it up)
 * 2. If access_token is already saved → ChatScreen
 * 3. Otherwise, fetch GET /api/config:
 *    - authEnabled=false → ChatScreen (dev mode, no login needed)
 *    - authEnabled=true  → LoginScreen
 *    - fetch error       → LoginScreen (safe default)
 */
@OptIn(ExperimentalVoyagerApi::class)
@Composable
private fun StartNavigator(settings: Settings) {
    val metadataApi = koinInject<MetadataApi>()
    var startScreen by remember { mutableStateOf<Screen?>(null) }

    LaunchedEffect(Unit) {
        val serverUrl = settings.getStringOrNull("server_url")?.trimEnd('/')
        val hasToken  = settings.getStringOrNull("access_token") != null

        startScreen = when {
            serverUrl == null -> LoginScreen()
            hasToken          -> ChatScreen()
            else              -> try {
                val config = metadataApi.fetchConfig(serverUrl)
                if (!config.authEnabled) ChatScreen() else LoginScreen()
            } catch (_: Exception) {
                LoginScreen()
            }
        }
    }

    if (startScreen == null) {
        // Brief loading while we check /api/config
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Navigator(
            screen = startScreen!!,
            disposeBehavior = NavigatorDisposeBehavior(disposeSteps = false)
        ) { navigator ->
            SlideTransition(
                navigator = navigator,
                disposeScreenAfterTransitionEnd = true
            )
        }
    }
}
