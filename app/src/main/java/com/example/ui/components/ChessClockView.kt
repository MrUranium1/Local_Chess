package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PieceColor
import com.example.ui.theme.*

@Composable
fun ChessClockView(
    playerName: String,
    playerColor: PieceColor,
    timeMs: Long,
    isActiveTurn: Boolean,
    isUnlimited: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    val isLowTime = totalSeconds in 1..30 && !isUnlimited

    val cardBgColor by animateColorAsState(
        targetValue = when {
            isActiveTurn -> AmberAccent
            else -> SlateDarkCard
        },
        label = "clockBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isLowTime && isActiveTurn -> CrimsonError
            isActiveTurn -> GoldPrimary
            else -> GeoBorderColor
        },
        label = "clockBorder"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("clock_${playerColor.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = if (isActiveTurn) Color(0xFF121410) else GeoTextSecondary,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = playerName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActiveTurn) Color(0xFF121410) else GeoTextPrimary
                )
                Text(
                    text = if (playerColor == PieceColor.WHITE) "White" else "Black",
                    fontSize = 11.sp,
                    color = if (isActiveTurn) Color(0xFF2E3A27) else GeoTextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isActiveTurn) GoldPrimary else SlateDarkSurface)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = if (isLowTime) CrimsonError else if (isActiveTurn) Color.White else GeoTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isUnlimited) "∞" else formattedTime,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isLowTime) CrimsonError else if (isActiveTurn) Color.White else GeoTextPrimary
            )
        }
    }
}
