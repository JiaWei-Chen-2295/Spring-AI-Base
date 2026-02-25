package com.example.aitemplate.client.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.aitemplate.client.ui.component.StreamStatusIndicator
import com.example.aitemplate.client.ui.screen.settings.SettingsScreen
import com.example.aitemplate.client.ui.theme.*

class ChatScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ChatScreenModel>()
        val navigator = LocalNavigator.currentOrThrow
        val streamState by screenModel.streamState.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.loadMetadata()
            screenModel.loadConversations()
        }

        MainLayout(screenModel, streamState, onSettings = { navigator.push(SettingsScreen()) })
    }
}

// ── Unified Layout: single column + overlay drawer ────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainLayout(screenModel: ChatScreenModel, streamState: StreamState, onSettings: () -> Unit) {
    var showDrawer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = screenModel.selectedModelId.ifBlank { "Clean Slate AI" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (streamState.name != "IDLE" && streamState.name != "STOPPED") {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.secondary, androidx.compose.foundation.shape.CircleShape))
                                    Text(
                                        text = streamState.name,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showDrawer = true }) {
                            Text("☰", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        IconButton(onClick = onSettings) { Text("⚙", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface) }
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
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            
            // Center chat column
            Column(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp)) {
                ChatArea(screenModel, streamState, Modifier.weight(1f))
            }

            // Drawer overlay
            if (showDrawer) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxHeight().width(300.dp),
                        shadowElevation = 16.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Configuration",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(onClick = { showDrawer = false }) {
                                    Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider()
                            ConfigPanel(screenModel, onClose = { showDrawer = false })
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(alpha = 0.2f)).clickable { showDrawer = false })
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
        StatCard("Total",     total.toString(),     Color(0xFF1677FF), Modifier.weight(1f))
        StatCard("You",       userCount.toString(),  Color(0xFF52C41A), Modifier.weight(1f))
        StatCard("Assistant", aiCount.toString(),    Color(0xFF722ED1), Modifier.weight(1f))
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
private fun ChatArea(screenModel: ChatScreenModel, streamState: StreamState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    // Auto-scroll on new content
    LaunchedEffect(screenModel.messages.size, screenModel.messages.lastOrNull()?.content) {
        if (screenModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(screenModel.messages.lastIndex)
        }
    }

    Column(modifier = modifier) {
        // Top bar: stream indicator
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

        // Error alert
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
                TextButton(onClick = { screenModel.dismissError() }) { Text("✕") }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
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
                        sending  = screenModel.sending
                    )
                }
            }
        }

        // Input
        ChatInput(
            sending   = screenModel.sending,
            streamMode = screenModel.streamMode,
            onSend    = { screenModel.sendMessage(it) },
            onStop    = { screenModel.stopStream() }
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = MaterialTheme.colorScheme.onPrimary, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "CLEAN SLATE AI",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        // Quick prompts grid (2 cols)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.widthIn(max = 600.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PromptCard("📝 Summarize Text", "Please summarize the key points.", enabled, Modifier.weight(1f), onSelect)
                PromptCard("💻 Explain Code", "Please explain step by step.", enabled, Modifier.weight(1f), onSelect)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PromptCard("🐛 Debug JSON", "Debug the following JSON.", enabled, Modifier.weight(1f), onSelect)
                PromptCard("🗄️ Generate SQL", "Generate SQL for...", enabled, Modifier.weight(1f), onSelect)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptCard(label: String, prompt: String, enabled: Boolean, modifier: Modifier, onSelect: (String) -> Unit) {
    Surface(
        onClick = { if (enabled) onSelect(prompt) },
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.height(64.dp)
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
        Text(
            "Execution Snapshot",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider()

        SnapRow("Model",    screenModel.selectedModelId.ifBlank { "—" })
        SnapRow("Conv",     "…${screenModel.conversationId.takeLast(8)}")
        SnapRow("Mode",     if (screenModel.streamMode) "⚡ SSE" else "📬 Single")
        SnapRow("Tools",    screenModel.selectedTools.joinToString("\n").ifBlank { "none" })
        SnapRow("Skills",   screenModel.selectedSkills.joinToString("\n").ifBlank { "none" })
    }
}

@Composable
private fun SnapRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.W600)
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
        models               = screenModel.models,
        tools                = screenModel.tools,
        skills               = screenModel.skills,
        selectedModelId      = screenModel.selectedModelId,
        selectedTools        = screenModel.selectedTools,
        selectedSkills       = screenModel.selectedSkills,
        streamMode           = screenModel.streamMode,
        conversations        = screenModel.conversations,
        currentConversationId= screenModel.conversationId,
        onModelSelected      = { screenModel.selectedModelId = it },
        onToolsChanged       = { screenModel.selectedTools = it },
        onSkillsChanged      = { screenModel.selectedSkills = it },
        onStreamModeChanged  = { screenModel.streamMode = it },
        onNewConversation    = { screenModel.newConversation() },
        onSwitchConversation = { screenModel.switchConversation(it); onClose?.invoke() },
        onDeleteConversation = { screenModel.deleteConversation(it) }
    )
}
