package com.example.aitemplate.client.ui.screen.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aitemplate.client.data.model.ConversationInfo
import com.example.aitemplate.client.data.model.ModelInfo
import com.example.aitemplate.client.data.model.SkillInfo
import com.example.aitemplate.client.data.model.ToolInfo
import com.example.aitemplate.client.ui.component.ModelSelector
import com.example.aitemplate.client.ui.component.MultiSelector

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
            Text("Stream Mode", style = MaterialTheme.typography.labelMedium)
            Switch(
                checked = streamMode,
                onCheckedChange = onStreamModeChanged
            )
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
            TextButton(onClick = onNewConversation) {
                Text("+ New")
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
                    Text(
                        text = conv.conversationId.takeLast(12),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { onDeleteConversation(conv.conversationId) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("X", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
