package com.example.aitemplate.client.ui.screen.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aitemplate.client.data.model.ToolCallInfo
import com.example.aitemplate.client.ui.theme.*

fun getToolAccentColor(toolName: String): Color {
    val lower = toolName.lowercase()
    return when {
        lower.contains("shell") || lower.contains("exec") || lower.contains("run") -> Amber500
        lower.contains("web")   || lower.contains("search") ||
        lower.contains("http")  || lower.contains("fetch")                         -> BrandPrimary
        lower.contains("read")  || lower.contains("skill") ||
        lower.contains("memory")                                                    -> Purple600
        lower.contains("write") || lower.contains("save") ||
        lower.contains("create")                                                    -> AccentGreen
        else                                                                        -> Cyan500
    }
}

@Composable
fun ToolCallCard(
    toolCalls: List<ToolCallInfo>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Tool Calls",
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            // Completed badge
            Box(
                modifier = Modifier
                    .background(AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "${toolCalls.size} completed",
                    fontSize = 11.sp,
                    color = AccentGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Individual tool calls ────────────────────────────────────────────
        toolCalls.forEachIndexed { idx, call ->
            SingleToolCallRow(
                call = call,
                defaultExpanded = idx == 0 || toolCalls.size <= 2
            )
            if (idx < toolCalls.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun SingleToolCallRow(
    call: ToolCallInfo,
    defaultExpanded: Boolean
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    val accent = getToolAccentColor(call.toolName)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Left border accent — 3px like the frontend
            .drawBehind {
                drawRect(
                    color = accent,
                    topLeft = Offset(0f, 0f),
                    size = size.copy(width = 3.dp.toPx())
                )
            }
    ) {
        // ── Collapsible header ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status icon — CheckCircle when expanded, ChevronRight when collapsed
            Icon(
                if (expanded) Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = accent
            )

            // Tool name
            Text(
                text = call.toolName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = accent,
                modifier = Modifier.weight(1f)
            )

            // Duration tag
            if (call.durationMs > 0) {
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${call.durationMs}ms",
                        fontSize = 11.sp,
                        color = accent
                    )
                }
            }
        }

        // ── Expandable body ─────────────────────────────────────────────────
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .padding(start = 15.dp, end = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (call.input.isNotBlank()) {
                    SectionLabel("INPUT")
                    DarkCodeBlock(call.input.take(1000))
                }
                if (call.output.isNotBlank()) {
                    SectionLabel("OUTPUT")
                    DarkCodeBlock(call.output.take(1000))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Dark monospace pre block — matches frontend .tc-pre */
@Composable
private fun DarkCodeBlock(content: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(CodeBg)
            .heightIn(max = 200.dp)
    ) {
        val scrollState = rememberScrollState()
        Text(
            text = content,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = CodeText,
            lineHeight = 18.sp,
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(10.dp)
        )
    }
}
