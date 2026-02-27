package com.example.aitemplate.client.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.aitemplate.client.i18n.LocalStrings
import com.example.aitemplate.client.ui.screen.chat.ChatScreen
import com.example.aitemplate.client.ui.theme.*
import kotlinx.coroutines.launch

class SettingsScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<SettingsScreenModel>()
        val navigator = LocalNavigator.currentOrThrow
        val serverUrl by screenModel.serverUrl.collectAsState()
        val testResult by screenModel.testResult.collectAsState()
        val scope = rememberCoroutineScope()

        // Auto-navigate if a URL was previously saved
        LaunchedEffect(Unit) {
            if (screenModel.hasSavedUrl()) {
                navigator.replace(ChatScreen())
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Theme toggle row — top-right corner
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val strings = LocalStrings.current
                    CompactThemeButton(ThemeMode.LIGHT,  Icons.Default.LightMode,         strings.light)
                    CompactThemeButton(ThemeMode.SYSTEM, Icons.Default.SettingsBrightness, strings.system)
                    CompactThemeButton(ThemeMode.DARK,   Icons.Default.DarkMode,           strings.dark)
                }

                // Center card
                Column(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Logo icon
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        LocalStrings.current.settingsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        LocalStrings.current.settingsSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(32.dp))

                    // URL input
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { screenModel.updateServerUrl(it) },
                        placeholder = {
                            Text(
                                "http://localhost:8080",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Connection test result
                    when (val result = testResult) {
                        is TestResult.Idle    -> {}
                        is TestResult.Testing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Text(
                                    LocalStrings.current.settingsTestingConnection,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is TestResult.Success -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    LocalStrings.current.settingsConnected(result.modelCount),
                                    color = AccentGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is TestResult.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    result.message,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Connect button
                    Button(
                        onClick = {
                            screenModel.saveAndTest()
                            navigator.replace(ChatScreen())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = serverUrl.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(LocalStrings.current.settingsConnect, fontWeight = FontWeight.Medium)
                    }

                    Spacer(Modifier.height(4.dp))

                    // Test connection link
                    TextButton(
                        onClick = {
                            scope.launch {
                                screenModel.updateServerUrl(serverUrl.trimEnd('/'))
                                screenModel.testConnection()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = serverUrl.isNotBlank() && testResult !is TestResult.Testing
                    ) {
                        Text(
                            LocalStrings.current.settingsTestConnection,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    Text(
                        "v1.0.2-alpha",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactThemeButton(
    mode: ThemeMode,
    icon: ImageVector,
    label: String
) {
    val currentMode = LocalThemeMode.current
    val setMode     = LocalSetThemeMode.current
    val isSelected  = currentMode == mode

    IconButton(
        onClick = { setMode(mode) },
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        )
    }
}
