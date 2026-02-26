package com.example.aitemplate.client.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.aitemplate.client.ui.screen.auth.LoginScreen
import com.example.aitemplate.client.ui.theme.*

class ProfileScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ProfileScreenModel>()
        val navigator = LocalNavigator.currentOrThrow
        val user by screenModel.user.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val message by screenModel.message.collectAsState()
        val logoutSuccess by screenModel.logoutSuccess.collectAsState()

        var showLogoutConfirm by remember { mutableStateOf(false) }
        var showPasswordDialog by remember { mutableStateOf(false) }
        var showEditDialog by remember { mutableStateOf(false) }

        // Navigate to login on logout
        LaunchedEffect(logoutSuccess) {
            if (logoutSuccess) {
                navigator.replaceAll(LoginScreen())
            }
        }

        // Show snackbar message
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(message) {
            message?.let {
                snackbarHostState.showSnackbar(it)
                screenModel.clearMessage()
            }
        }

        LaunchedEffect(Unit) {
            screenModel.loadUser()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("User Profile") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isLoading && user == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    user?.let { userInfo ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // User header card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = userInfo.name.firstOrNull()?.uppercase() ?: "?",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    Text(
                                        text = userInfo.name,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )

                                    Text(
                                        text = "@${userInfo.username}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    // Roles chips
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        userInfo.roles.forEach { role ->
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(role.roleName) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Verified,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // User info card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        "Personal Information",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    InfoRow(Icons.Default.Email, "Email", userInfo.email ?: "Not set")
                                    InfoRow(Icons.Default.Phone, "Phone", userInfo.phone ?: "Not set")
                                    InfoRow(
                                        Icons.Default.Person,
                                        "Gender",
                                        when (userInfo.gender) {
                                            1 -> "Male"
                                            2 -> "Female"
                                            else -> "Not set"
                                        }
                                    )
                                }
                            }

                            // Actions card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    ListItem(
                                        headlineContent = { Text("Edit Profile") },
                                        leadingContent = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        },
                                        modifier = Modifier.clickable { showEditDialog = true }
                                    )

                                    ListItem(
                                        headlineContent = { Text("Change Password") },
                                        leadingContent = {
                                            Icon(Icons.Default.Lock, contentDescription = null)
                                        },
                                        modifier = Modifier.clickable { showPasswordDialog = true }
                                    )

                                    HorizontalDivider()

                                    ListItem(
                                        headlineContent = { 
                                            Text(
                                                "Logout",
                                                color = MaterialTheme.colorScheme.error
                                            ) 
                                        },
                                        leadingContent = {
                                            Icon(
                                                Icons.Default.Logout,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        modifier = Modifier.clickable { showLogoutConfirm = true }
                                    )
                                }
                            }
                        }
                    }
                }

                // Logout confirmation dialog
                if (showLogoutConfirm) {
                    AlertDialog(
                        onDismissRequest = { showLogoutConfirm = false },
                        title = { Text("Confirm Logout") },
                        text = { Text("Are you sure you want to logout?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showLogoutConfirm = false
                                    screenModel.logout()
                                }
                            ) {
                                Text("Logout", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogoutConfirm = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Password change dialog
                if (showPasswordDialog) {
                    PasswordChangeDialog(
                        onDismiss = { showPasswordDialog = false },
                        onConfirm = { oldPwd, newPwd ->
                            screenModel.changePassword(oldPwd, newPwd)
                            showPasswordDialog = false
                        }
                    )
                }

                // Edit profile dialog
                if (showEditDialog && user != null) {
                    EditProfileDialog(
                        user = user!!,
                        onDismiss = { showEditDialog = false },
                        onConfirm = { name, email, phone, gender ->
                            screenModel.updateProfile(name, email, phone, gender)
                            showEditDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PasswordChangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (oldPassword: String, newPassword: String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }

    val passwordMatch = newPassword == confirmPassword
    val canConfirm = oldPassword.isNotBlank() && newPassword.isNotBlank() && passwordMatch

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = if (showOldPassword) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showOldPassword = !showOldPassword }) {
                            Icon(
                                if (showOldPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = if (showNewPassword) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = if (showNewPassword) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    isError = confirmPassword.isNotBlank() && !passwordMatch,
                    supportingText = {
                        if (confirmPassword.isNotBlank() && !passwordMatch) {
                            Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(oldPassword, newPassword) },
                enabled = canConfirm
            ) {
                Text("Change")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    user: com.example.aitemplate.client.data.model.UserInfo,
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String?, phone: String?, gender: Int?) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email ?: "") }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var gender by remember { mutableIntStateOf(user.gender ?: 0) }
    var expanded by remember { mutableStateOf(false) }

    val genderOptions = listOf("Not set" to 0, "Male" to 1, "Female" to 2)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = genderOptions.find { it.second == gender }?.first ?: "Not set",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        genderOptions.forEach { (label, value) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    gender = value
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        email.takeIf { it.isNotBlank() },
                        phone.takeIf { it.isNotBlank() },
                        gender.takeIf { it != 0 }
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
