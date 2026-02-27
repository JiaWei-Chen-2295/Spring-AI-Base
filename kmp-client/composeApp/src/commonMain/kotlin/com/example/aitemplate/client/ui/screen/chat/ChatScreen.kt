package com.example.aitemplate.client.ui.screen.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.aitemplate.client.i18n.LocalStrings
import com.example.aitemplate.client.ui.component.StreamStatusIndicator
import com.example.aitemplate.client.ui.screen.auth.LoginScreen
import com.example.aitemplate.client.ui.screen.profile.ProfileScreen
import com.example.aitemplate.client.ui.screen.settings.SettingsScreen
import com.example.aitemplate.client.ui.theme.*
import kotlinx.coroutines.delay

class ChatScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ChatScreenModel>()
        val navigator = LocalNavigator.currentOrThrow
        val streamState by screenModel.streamState.collectAsState()
        val username by screenModel.username.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.loadMetadata()
            screenModel.loadConversations()
        }

        // Check if logged out (username becomes null)
        LaunchedEffect(username) {
            if (username == null) {
                navigator.replaceAll(LoginScreen())
            }
        }

        MainLayout(screenModel, streamState, onSettings = { navigator.push(SettingsScreen()) })
    }
}

// ── Unified Layout: single column + overlay drawer ────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainLayout(
    screenModel: ChatScreenModel,
    streamState: StreamState,
    onSettings: () -> Unit
) {
    val navigator = LocalNavigator.currentOrThrow
    var showConfigDrawer by remember { mutableStateOf(false) }
    var showUserDrawer by remember { mutableStateOf(false) }
    val username by screenModel.username.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = screenModel.selectedModelId.ifBlank { LocalStrings.current.chatDefaultTitle },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (streamState != StreamState.IDLE) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                    )
                                    Text(
                                        text = streamState.name.lowercase(),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showConfigDrawer = true }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = LocalStrings.current.chatMenu,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // User avatar button
                        IconButton(onClick = { showUserDrawer = true }) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = username?.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            // Center chat column
            Column(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp)) {
                ChatArea(screenModel, streamState, Modifier.weight(1f))
            }

            // Scrim overlay for both drawers
            AnimatedVisibility(
                visible = showConfigDrawer || showUserDrawer,
                enter = fadeIn(tween(250)),
                exit  = fadeOut(tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .clickable {
                            showConfigDrawer = false
                            showUserDrawer = false
                        }
                )
            }

            // Left drawer — Configuration panel
            AnimatedVisibility(
                visible = showConfigDrawer,
                modifier = Modifier.align(Alignment.CenterStart),
                enter = slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)),
                exit  = slideOutHorizontally(tween(250)) { -it } + fadeOut(tween(200))
            ) {
                Surface(
                    modifier = Modifier.fillMaxHeight().width(340.dp),
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                LocalStrings.current.chatConfiguration,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { showConfigDrawer = false }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = LocalStrings.current.close,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                        ConfigPanel(screenModel, onClose = { showConfigDrawer = false })
                    }
                }
            }

            // Right drawer — User panel
            AnimatedVisibility(
                visible = showUserDrawer,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)),
                exit  = slideOutHorizontally(tween(250)) { it } + fadeOut(tween(200))
            ) {
                Surface(
                    modifier = Modifier.fillMaxHeight().width(280.dp),
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        // Header with close button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                LocalStrings.current.chatAccount,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { showUserDrawer = false }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = LocalStrings.current.close,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()

                        // User info section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = username?.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            // Username
                            Text(
                                text = username ?: LocalStrings.current.unknown,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Menu items
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            // Profile
                            ListItem(
                                headlineContent = { Text(LocalStrings.current.profile) },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier.clickable {
                                    showUserDrawer = false
                                    navigator.push(ProfileScreen())
                                }
                            )

                            // Settings
                            ListItem(
                                headlineContent = { Text(LocalStrings.current.settings) },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier.clickable {
                                    showUserDrawer = false
                                    onSettings()
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Logout
                        ListItem(
                            headlineContent = {
                                Text(
                                    LocalStrings.current.logout,
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
                            modifier = Modifier
                                .clickable {
                                    showUserDrawer = false
                                    screenModel.logout()
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Stats row (total | user | assistant counts) ───────────────────────────────
@Composable
private fun StatsRow(screenModel: ChatScreenModel) {
    val total     = screenModel.messages.size
    val userCount = screenModel.messages.count { it.role == "user" }
    val aiCount   = screenModel.messages.count { it.role == "assistant" }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(LocalStrings.current.chatTotal,     total.toString(),     Color(0xFF1677FF), Modifier.weight(1f))
        StatCard(LocalStrings.current.chatYou,       userCount.toString(),  Color(0xFF52C41A), Modifier.weight(1f))
        StatCard(LocalStrings.current.chatAssistant, aiCount.toString(),    Color(0xFF722ED1), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(label, fontSize = 11.sp, color = accent.copy(alpha = 0.7f))
        }
    }
}

// ── Chat area: messages + input ───────────────────────────────────────────────
@Composable
private fun ChatArea(
    screenModel: ChatScreenModel,
    streamState: StreamState,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll on new content — debounced to avoid animation interruption
    LaunchedEffect(screenModel.messages.size, screenModel.messages.lastOrNull()?.content) {
        if (screenModel.messages.isNotEmpty()) {
            delay(50) // tiny debounce to batch rapid SSE updates
            listState.animateScrollToItem(screenModel.messages.lastIndex)
        }
    }

    Column(modifier = modifier) {
        // Top bar: conversation id + stream indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "conversation: …${screenModel.conversationId.takeLast(8)}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            StreamStatusIndicator(state = streamState)
        }

        // Error alert — animated
        AnimatedVisibility(
            visible = screenModel.error != null,
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit  = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            screenModel.error?.let { errMsg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        errMsg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { screenModel.dismissError() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = LocalStrings.current.chatDismiss,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Messages
        SelectionContainer(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (screenModel.messages.isEmpty()) {
                    item {
                        EmptyState(
                            enabled = !screenModel.sending,
                            onSelect = { screenModel.sendMessage(it) }
                        )
                    }
                } else {
                    itemsIndexed(screenModel.messages) { idx, message ->
                        val isLatest = idx == screenModel.messages.lastIndex
                        MessageBubble(
                            message  = message,
                            isLatest = isLatest,
                            sending  = screenModel.sending,
                            onQuote  = { screenModel.setQuote(it) }
                        )
                    }
                }
            }
        }

        // Input
        ChatInput(
            sending       = screenModel.sending,
            streamMode    = screenModel.streamMode,
            onSend        = { screenModel.sendMessage(it) },
            onStop        = { screenModel.stopStream() },
            quotedMessage = screenModel.quotedMessage,
            onClearQuote  = { screenModel.clearQuote() }
        )

        // Footer hint
        val toolCount  = screenModel.selectedTools.size
        val skillCount = screenModel.selectedSkills.size
        if (screenModel.selectedModelId.isNotBlank()) {
            Text(
                text = "${screenModel.selectedModelId} | tools:$toolCount | skills:$skillCount",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.padding(start = 14.dp, bottom = 6.dp)
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyState(enabled: Boolean, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(48.dp)
    ) {
        // Ghost logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                LocalStrings.current.chatEmptyTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        // Quick prompts grid (2 cols)
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 600.dp)
        ) {
            val strings = LocalStrings.current
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PromptCard(
                    icon    = Icons.Default.Description,
                    label   = strings.promptSummarize,
                    prompt  = strings.promptSummarizeText,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
                PromptCard(
                    icon    = Icons.Default.Code,
                    label   = strings.promptExplainCode,
                    prompt  = strings.promptExplainCodeText,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PromptCard(
                    icon    = Icons.Default.BugReport,
                    label   = strings.promptDebugJson,
                    prompt  = strings.promptDebugJsonText,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
                PromptCard(
                    icon    = Icons.Default.Storage,
                    label   = strings.promptGenerateSql,
                    prompt  = strings.promptGenerateSqlText,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onSelect = onSelect
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptCard(
    icon: ImageVector,
    label: String,
    prompt: String,
    enabled: Boolean,
    modifier: Modifier,
    onSelect: (String) -> Unit
) {
    Surface(
        onClick = { if (enabled) onSelect(prompt) },
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Snapshot panel ────────────────────────────────────────────────────────────
@Composable
private fun SnapshotPanel(screenModel: ChatScreenModel) {
    Column(
        modifier = Modifier.padding(14.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val strings = LocalStrings.current
        Text(
            strings.chatExecutionSnapshot,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider()
        SnapRow(strings.chatModel,  screenModel.selectedModelId.ifBlank { "—" })
        SnapRow(strings.chatConv,   "…${screenModel.conversationId.takeLast(8)}")
        SnapRow(strings.chatMode,   if (screenModel.streamMode) strings.chatSseStream else strings.chatSingle)
        SnapRow(strings.chatTools,  screenModel.selectedTools.joinToString("\n").ifBlank { strings.chatNone })
        SnapRow(strings.chatSkills, screenModel.selectedSkills.joinToString("\n").ifBlank { strings.chatNone })
    }
}

@Composable
private fun SnapRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.W600
        )
        Text(
            value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── ConfigPanel forwarding helper ─────────────────────────────────────────────
@Composable
private fun ConfigPanel(screenModel: ChatScreenModel, onClose: (() -> Unit)? = null) {
    ConfigPanel(
        models                = screenModel.models,
        tools                 = screenModel.tools,
        skills                = screenModel.skills,
        selectedModelId       = screenModel.selectedModelId,
        selectedTools         = screenModel.selectedTools,
        selectedSkills        = screenModel.selectedSkills,
        streamMode            = screenModel.streamMode,
        conversations         = screenModel.conversations,
        currentConversationId = screenModel.conversationId,
        onModelSelected       = { screenModel.selectedModelId = it },
        onToolsChanged        = { screenModel.selectedTools = it },
        onSkillsChanged       = { screenModel.selectedSkills = it },
        onStreamModeChanged   = { screenModel.streamMode = it },
        onNewConversation     = { screenModel.newConversation() },
        onSwitchConversation  = { screenModel.switchConversation(it); onClose?.invoke() },
        onDeleteConversation  = { screenModel.deleteConversation(it) }
    )
}

// UserMenu has been integrated into MainLayout as a right-side drawer panel
