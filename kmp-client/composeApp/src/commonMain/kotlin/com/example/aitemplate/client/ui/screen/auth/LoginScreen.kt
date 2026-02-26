package com.example.aitemplate.client.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.aitemplate.client.ui.screen.chat.ChatScreen
import com.example.aitemplate.client.ui.theme.*
import kotlinx.coroutines.launch

class LoginScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<LoginScreenModel>()
        val navigator = LocalNavigator.currentOrThrow
        
        val username by screenModel.username.collectAsState()
        val password by screenModel.password.collectAsState()
        val serverUrl by screenModel.serverUrl.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val errorMessage by screenModel.errorMessage.collectAsState()
        val loginSuccess by screenModel.loginSuccess.collectAsState()
        
        var passwordVisible by remember { mutableStateOf(false) }
        var showServerConfig by remember { mutableStateOf(serverUrl.isBlank()) }

        // Auto-navigate on login success
        LaunchedEffect(loginSuccess) {
            if (loginSuccess) {
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
                    CompactThemeButton(ThemeMode.LIGHT, Icons.Default.LightMode, "Light")
                    CompactThemeButton(ThemeMode.SYSTEM, Icons.Default.SettingsBrightness, "System")
                    CompactThemeButton(ThemeMode.DARK, Icons.Default.DarkMode, "Dark")
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
                        "AI Template",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "用户登录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(32.dp))

                    // Error message
                    errorMessage?.let { error ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    error,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Server URL configuration
                    if (showServerConfig || serverUrl.isBlank()) {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { screenModel.updateServerUrl(it) },
                            label = { Text("服务器地址") },
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
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null)
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    } else {
                        // Show configured server URL with edit option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "服务器: $serverUrl",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                            TextButton(
                                onClick = { showServerConfig = true },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("修改", fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Username input
                    OutlinedTextField(
                        value = username,
                        onValueChange = { screenModel.updateUsername(it) },
                        label = { Text("用户名") },
                        placeholder = { Text("请输入用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Password input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { screenModel.updatePassword(it) },
                        label = { Text("密码") },
                        placeholder = { Text("请输入密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(Modifier.height(24.dp))

                    // Login button
                    Button(
                        onClick = { screenModel.login() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading && username.isNotBlank() && password.isNotBlank() && serverUrl.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("登录", fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Demo account hint
                    Text(
                        "默认账号: admin / admin123",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    val currentMode = LocalThemeMode.current
    val setMode = LocalSetThemeMode.current
    val isSelected = currentMode == mode

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
