package com.example.aitemplate.client.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aitemplate.client.ui.screen.chat.StreamState

@Composable
fun StreamStatusIndicator(
    state: StreamState,
    modifier: Modifier = Modifier
) {
    val (dotColor, label) = when (state) {
        StreamState.CONNECTING -> Color(0xFFFFA726) to "connecting"
        StreamState.STREAMING  -> Color(0xFF52C41A) to "streaming"
        StreamState.ERROR      -> Color(0xFFFF4D4F) to "error"
        StreamState.IDLE       -> return
    }

    // Pulsing scale for streaming state
    val dotScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = if (state == StreamState.STREAMING) 0.7f else 1f,
        targetValue  = if (state == StreamState.STREAMING) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            tween(600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(dotColor.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size((8 * dotScale).dp)
                .background(dotColor, CircleShape)
        )
        Text(
            text = "● $label",
            fontSize = 12.sp,
            color = dotColor
        )
    }
}
