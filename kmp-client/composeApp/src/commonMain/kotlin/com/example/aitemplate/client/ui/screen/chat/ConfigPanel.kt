package com.example.aitemplate.client.ui.screen.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aitemplate.client.data.model.ConversationInfo
import com.example.aitemplate.client.data.model.ModelInfo
import com.example.aitemplate.client.data.model.SkillInfo
import com.example.aitemplate.client.data.model.ToolInfo
import com.example.aitemplate.client.ui.component.ModelSelector
import com.example.aitemplate.client.ui.component.MultiSelector
import com.example.aitemplate.client.ui.theme.*

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
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(260.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Model selector
        ModelSelector(
            models = models,
            selectedModelId = selectedModelId,
            onModelSelected = onModelSelected
        )

        HorizontalDivider()

        // Stream mode toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    "Stream Responses",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Typewriter effect for tokens",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = streamMode,
                onCheckedChange = onStreamModeChanged
            )
        }

        HorizontalDivider()

        // Appearance / theme selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "APPEARANCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemeModeButton(
                    mode     = ThemeMode.LIGHT,
                    icon     = Icons.Default.LightMode,
                    label    = "Light",
                    modifier = Modifier.weight(1f)
                )
                ThemeModeButton(
                    mode     = ThemeMode.SYSTEM,
                    icon     = Icons.Default.SettingsBrightness,
                    label    = "System",
                    modifier = Modifier.weight(1f)
                )
                ThemeModeButton(
                    mode     = ThemeMode.DARK,
                    icon     = Icons.Default.DarkMode,
                    label    = "Dark",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider()

        // Tool selection
        if (tools.isNotEmpty()) {
            MultiSelector(
                label = "Tools (${selectedTools.size})",
                items = tools.map { "${it.toolName} [${it.riskLevel}]" },
                selectedItems = selectedTools.map { name ->
                    tools.find { it.toolName == name }?.let { "${it.toolName} [${it.riskLevel}]" } ?: name
                }.toSet(),
                onSelectionChanged = { selected ->
                    onToolsChanged(
                        selected.map { it.substringBefore(" [") }.toSet()
                    )
                }
            )
        }

        // Skill selection
        if (skills.isNotEmpty()) {
            MultiSelector(
                label = "Skills (${selectedSkills.size})",
                items = skills.map { "${it.skillName}@${it.version}" },
                selectedItems = selectedSkills,
                onSelectionChanged = onSkillsChanged
            )
        }

        HorizontalDivider()

        // Conversation management
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Conversations", style = MaterialTheme.typography.labelMedium)
            TextButton(
                onClick = onNewConversation,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New conversation",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("New", style = MaterialTheme.typography.labelSmall)
            }
        }

        conversations.forEach { conv ->
            val isCurrent = conv.conversationId == currentConversationId
            Card(
                onClick = { onSwitchConversation(conv.conversationId) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        if (isCurrent) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isCurrent)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = conv.conversationId.takeLast(12),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onDeleteConversation(conv.conversationId) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// ── Theme mode button (Light / System / Dark) ─────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeButton(
    mode: ThemeMode,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val currentMode = LocalThemeMode.current
    val setMode     = LocalSetThemeMode.current
    val isSelected  = currentMode == mode

    Surface(
        onClick   = { setMode(mode) },
        shape     = RoundedCornerShape(8.dp),
        color     = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
        border    = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        ),
        modifier  = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                label,
                fontSize = 10.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
