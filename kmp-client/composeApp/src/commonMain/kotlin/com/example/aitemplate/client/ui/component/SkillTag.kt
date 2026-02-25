package com.example.aitemplate.client.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aitemplate.client.data.model.SkillApplyInfo
import com.example.aitemplate.client.ui.theme.AmberDark

// Gold tag: matches frontend .skill-applied-row tags (#fff8e1 bg, #d48806 text)
private val GoldBg = Color(0xFFFFF8E1)

@Composable
fun SkillTag(
    skill: SkillApplyInfo,
    modifier: Modifier = Modifier
) {
    // "team/kb/qa@1.0" → "qa" or "qa (1.0)"
    val shortName = skill.name.split("/").last().split("@").first()
    val label = if (skill.version.isNotBlank()) "$shortName (${skill.version})" else shortName

    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = AmberDark,
        modifier = modifier
            .background(GoldBg, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}
