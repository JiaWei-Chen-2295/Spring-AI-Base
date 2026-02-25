package com.example.aitemplate.client.ui.screen.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aitemplate.client.ui.component.SkillTag
import com.example.aitemplate.client.ui.theme.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun MessageBubble(
    message: ChatMessage,
    isLatest: Boolean = false,
    sending: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI avatar
            AvatarBadge(label = "AI", bg = AccentGreen)
            Spacer(Modifier.width(10.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            // Metadata row: role + time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isUser) "You" else "Assistant",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W600,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(4.dp))

            // Applied skills row  (yellow bar — only for assistant)
            if (!isUser && message.appliedSkills.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SkillRowBg, RoundedCornerShape(6.dp))
                        .border(1.dp, SkillRowBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("📚", fontSize = 13.sp)
                    message.appliedSkills.forEach { skill ->
                        SkillTag(skill)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Message bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp, topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 3.dp,
                            bottomEnd   = if (isUser) 3.dp else 12.dp
                        )
                    )
                    .background(if (isUser) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                    .then(
                        if (!isUser) Modifier.border(
                            1.dp, MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 3.dp)
                        ) else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                when {
                    // Thinking animation while awaiting first token
                    isLatest && sending && message.content.isEmpty() && !isUser ->
                        ThinkingIndicator()

                    message.content.isNotBlank() ->
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                }
            }

            // Tool calls below bubble
            if (message.toolCalls.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                ToolCallCard(toolCalls = message.toolCalls)
            }
        }

        if (isUser) {
            Spacer(Modifier.width(10.dp))
            AvatarBadge(label = "U", bg = BrandPrimary)
        }
    }
}

@Composable
private fun AvatarBadge(label: String, bg: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (bg == AccentGreen) Color.White else MaterialTheme.colorScheme.onPrimary
        )
    }
}

/**
 * Three-dot bounce animation matching the frontend's thinking-bounce CSS keyframe.
 * Each dot animates with a 0.2s stagger: translateY -6dp, opacity 0.4→1→0.4, 1.2s cycle.
 */
@Composable
private fun ThinkingIndicator() {
    val dotCount = 3
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")

    // Each dot has its own offset phase
    val offsets = (0 until dotCount).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1200
                    0f at (i * 167)                          // rest start
                    (-6f) at (i * 167 + 300) with EaseInOut // rise peak
                    0f at (i * 167 + 600)                   // come back
                    0f at 1200                               // full cycle rest
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot_$i"
        )
    }

    val alphas = (0 until dotCount).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1200
                    0.4f at (i * 167)
                    1.0f at (i * 167 + 300) with LinearEasing
                    0.4f at (i * 167 + 600)
                    0.4f at 1200
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha_$i"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.height(24.dp)
    ) {
        (0 until dotCount).forEach { i ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .offset(y = offsets[i].value.dp)
                    .background(
                        BrandPrimary.copy(alpha = alphas[i].value),
                        CircleShape
                    )
            )
        }
    }
}

private fun formatTimestamp(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
