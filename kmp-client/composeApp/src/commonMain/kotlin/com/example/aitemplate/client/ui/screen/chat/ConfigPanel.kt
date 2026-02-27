package com.example.aitemplate.client.ui.screen.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aitemplate.client.data.model.ConversationInfo
import com.example.aitemplate.client.data.model.ModelInfo
import com.example.aitemplate.client.data.model.SkillInfo
import com.example.aitemplate.client.data.model.ToolInfo
import com.example.aitemplate.client.ui.component.ModelSelector
import com.example.aitemplate.client.ui.theme.*
import kotlinx.datetime.*

private enum class DrawerTab { Settings, History }

@Composable
fun ConfigPanel(
    models: List<ModelInfo>,
    tools: List<ToolInfo>,
    skills: List<SkillInfo>,
    selectedModelId: String,
    selectedTools: Set<String>,
    selectedSkills: Set<String>,
    streamMode: Boolean,
    conversations: List<ConversationInfo>,
    currentConversationId: String,
    onModelSelected: (String) -> Unit,
    onToolsChanged: (Set<String>) -> Unit,
    onSkillsChanged: (Set<String>) -> Unit,
    onStreamModeChanged: (Boolean) -> Unit,
    onNewConversation: () -> Unit,
    onSwitchConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DrawerTab.Settings) }

    Column(modifier = modifier.fillMaxSize()) {
        // ── Tab Switcher ─────────────────────────────────────────────────────────
        TabSwitcher(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // ── Tab Content with Crossfade Animation ─────────────────────────────────
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            modifier = Modifier.weight(1f)
        ) { tab ->
            when (tab) {
                DrawerTab.Settings -> SettingsContent(
                    models = models,
                    tools = tools,
                    skills = skills,
                    selectedModelId = selectedModelId,
                    selectedTools = selectedTools,
                    selectedSkills = selectedSkills,
                    streamMode = streamMode,
                    onModelSelected = onModelSelected,
                    onToolsChanged = onToolsChanged,
                    onSkillsChanged = onSkillsChanged,
                    onStreamModeChanged = onStreamModeChanged
                )
                DrawerTab.History -> HistoryContent(
                    conversations = conversations,
                    currentConversationId = currentConversationId,
                    onSwitchConversation = onSwitchConversation,
                    onDeleteConversation = onDeleteConversation,
                    onNewConversation = onNewConversation
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Tab Switcher with animated indicator
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun TabSwitcher(
    selectedTab: DrawerTab,
    onTabSelected: (DrawerTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DrawerTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val animatedWeight by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                )

                Surface(
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surface
                           else Color.Transparent,
                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            else null,
                    modifier = Modifier.weight(animatedWeight)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Settings Tab Content
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun SettingsContent(
    models: List<ModelInfo>,
    tools: List<ToolInfo>,
    skills: List<SkillInfo>,
    selectedModelId: String,
    selectedTools: Set<String>,
    selectedSkills: Set<String>,
    streamMode: Boolean,
    onModelSelected: (String) -> Unit,
    onToolsChanged: (Set<String>) -> Unit,
    onSkillsChanged: (Set<String>) -> Unit,
    onStreamModeChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── MODEL Section ────────────────────────────────────────────────────────
        SectionLabel("MODEL")
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            ModelSelector(
                models = models,
                selectedModelId = selectedModelId,
                onModelSelected = onModelSelected,
                modifier = Modifier.padding(4.dp)
            )
        }
        
        Text(
            text = "Select the underlying inference model. Changing models will start a new session.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            lineHeight = 16.sp
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        // ── BEHAVIOR Section ─────────────────────────────────────────────────────
        SectionLabel("BEHAVIOR")
        
        // Stream Responses Toggle
        SettingsToggleItem(
            title = "Stream Responses",
            subtitle = "Typewriter effect for tokens",
            checked = streamMode,
            onCheckedChange = onStreamModeChanged
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        // ── ACTIVE TOOLS Section ─────────────────────────────────────────────────
        if (tools.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("ACTIVE TOOLS", Modifier)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${selectedTools.size} Enabled",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            ChipSelector(
                items = tools.map { it.toolName },
                selectedItems = selectedTools,
                onSelectionChanged = onToolsChanged,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(16.dp))
        }

        // ── SKILLS Section ───────────────────────────────────────────────────────
        if (skills.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("SKILLS", Modifier)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${selectedSkills.size} Enabled",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            ChipSelector(
                items = skills.map { "${it.skillName}@${it.version}" },
                selectedItems = selectedSkills,
                onSelectionChanged = onSkillsChanged,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(16.dp))
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        // ── Server Status ────────────────────────────────────────────────────────
        ServerStatusCard()

        // ── Theme Selection ──────────────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        SectionLabel("THEME")
        ThemeSelector()

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SectionLabel(title: String, modifier: Modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.W600,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSelector(
    items: List<String>,
    selectedItems: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val isSelected = item in selectedItems
            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.surface,
                animationSpec = tween(200)
            )

            Surface(
                onClick = {
                    onSelectionChanged(
                        if (isSelected) selectedItems - item else selectedItems + item
                    )
                },
                shape = RoundedCornerShape(10.dp),
                color = animatedColor,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                              else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerStatusCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Connected to Localhost",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "http://localhost:8080",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Pulsing indicator
                    val infiniteTransition = rememberInfiniteTransition()
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                Color(0xFF10A27E).copy(alpha = alpha),
                                CircleShape
                            )
                    )
                    Text(
                        text = "Online",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10A27E)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSelector() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeModeButton(
            mode = ThemeMode.LIGHT,
            icon = Icons.Default.LightMode,
            label = "Light",
            modifier = Modifier.weight(1f)
        )
        ThemeModeButton(
            mode = ThemeMode.SYSTEM,
            icon = Icons.Default.SettingsBrightness,
            label = "System",
            modifier = Modifier.weight(1f)
        )
        ThemeModeButton(
            mode = ThemeMode.DARK,
            icon = Icons.Default.DarkMode,
            label = "Dark",
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeButton(
    mode: ThemeMode,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val currentMode = LocalThemeMode.current
    val setMode = LocalSetThemeMode.current
    val isSelected = currentMode == mode

    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                     else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200)
    )

    Surface(
        onClick = { setMode(mode) },
        shape = RoundedCornerShape(10.dp),
        color = animatedColor,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// History Tab Content
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun HistoryContent(
    conversations: List<ConversationInfo>,
    currentConversationId: String,
    onSwitchConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onNewConversation: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (conversations.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Forum,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No conversations yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Start a new chat to begin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            // Group conversations by date
            val grouped = groupConversationsByDate(conversations)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                grouped.forEach { (dateLabel, convs) ->
                    // Date header
                    item {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.W600,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }

                    // Conversation items
                    items(convs, key = { it.conversationId }) { conv ->
                        val isCurrent = conv.conversationId == currentConversationId

                        ConversationHistoryItem(
                            conversation = conv,
                            isCurrent = isCurrent,
                            onClick = { onSwitchConversation(conv.conversationId) },
                            onDelete = { onDeleteConversation(conv.conversationId) }
                        )
                    }
                }
            }
        }

        // Floating New Chat button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp,
            onClick = onNewConversation
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "New Chat",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun ConversationHistoryItem(
    conversation: ConversationInfo,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200)
    )

    Surface(
        onClick = onClick,
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
               else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .drawLeftBorder(animatedBorderColor, 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isCurrent) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isCurrent) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Conversation ${conversation.conversationId.takeLast(8)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
                    )
                }

                // Subtitle - removed since lastMessage doesn't exist
                /*
                Text(
                    text = conversation.lastMessage ?: "No messages yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                */
            }

            // Delete button (only for non-current)
            if (!isCurrent) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ── Helper: Draw left border ─────────────────────────────────────────────────
private fun Modifier.drawLeftBorder(color: Color, width: Dp) =
    this.drawBehind {
        drawRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(width.toPx(), size.height)
        )
    }

// ── Helper: Group conversations by date ──────────────────────────────────────
private fun groupConversationsByDate(conversations: List<ConversationInfo>): List<Pair<String, List<ConversationInfo>>> {
    // Since ConversationInfo doesn't have date fields, just return all as single group
    return if (conversations.isNotEmpty()) {
        listOf("CONVERSATIONS" to conversations)
    } else {
        emptyList()
    }
}
