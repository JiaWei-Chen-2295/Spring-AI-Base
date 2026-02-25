package com.example.aitemplate.client.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.aitemplate.client.ui.screen.chat.ChatScreen
import kotlinx.coroutines.launch

class SettingsScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<SettingsScreenModel>()
        val navigator = LocalNavigator.currentOrThrow
        val serverUrl by screenModel.serverUrl.collectAsState()
        val testResult by screenModel.testResult.collectAsState()
        val scope = rememberCoroutineScope()

        // Auto-navigate immediately if a URL was previously saved (don't block on connection test)
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
                Card(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "🤖",
                            fontSize = 40.sp
                        )
                        Text(
                            text = "AI Template Client",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Enter your Spring AI backend server URL",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { screenModel.updateServerUrl(it) },
                            label = { Text("Server URL") },
                            placeholder = { Text("http://localhost:8080") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Connection test result
                        when (val result = testResult) {
                            is TestResult.Idle -> {}
                            is TestResult.Testing -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Testing connection...", fontSize = 13.sp)
                                }
                            }
                            is TestResult.Success -> {
                                Text(
                                    "✓ Connected — ${result.modelCount} model(s) available",
                                    color = Color(0xFF52C41A),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            is TestResult.Error -> {
                                Text(
                                    "⚠ ${result.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Test only
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        screenModel.updateServerUrl(serverUrl.trimEnd('/'))
                                        screenModel.testConnection()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = serverUrl.isNotBlank() && testResult !is TestResult.Testing
                            ) {
                                Text("Test")
                            }

                            // Save + navigate (always allowed if URL non-blank)
                            Button(
                                onClick = {
                                    screenModel.saveAndTest()
                                    navigator.replace(ChatScreen())
                                },
                                modifier = Modifier.weight(1f),
                                enabled = serverUrl.isNotBlank()
                            ) {
                                Text("Connect →")
                            }
                        }

                        Text(
                            "Default: http://localhost:8080",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
