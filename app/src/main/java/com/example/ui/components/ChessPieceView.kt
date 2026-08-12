package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Piece
import com.example.model.PieceColor

@Composable
fun ChessPieceView(
    piece: Piece,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 36.dp,
    isSelected: Boolean = false
) {
    val scaleFactor by animateFloatAsState(
        targetValue = if (isSelected) 1.16f else 1.0f,
        animationSpec = tween(durationMillis = 180),
        label = "pieceScale"
    )

    val isWhite = piece.color == PieceColor.WHITE
    val mainColor = if (isWhite) Color(0xFFFFFDF5) else Color(0xFF1E2638)
    val outlineColor = if (isWhite) Color(0xFF1B2430) else Color(0xFFD4AF37) // Gold outline for black pieces, dark outline for white pieces

    val fontSize = (sizeDp.value * 0.74f).sp

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(scaleFactor)
            .testTag("piece_${piece.color.name}_${piece.type.name}"),
        contentAlignment = Alignment.Center
    ) {
        val glyph = piece.displaySymbol

        // 1. Subtle outline / background layer for contrast on all board square colors
        Text(
            text = glyph,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = outlineColor.copy(alpha = if (isWhite) 0.85f else 0.95f),
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(x = 0.8.dp, y = 0.8.dp)
        )
        Text(
            text = glyph,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = outlineColor.copy(alpha = if (isWhite) 0.85f else 0.95f),
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(x = (-0.8).dp, y = (-0.8).dp)
        )

        // 2. Main Piece Body with Drop Shadow
        Text(
            text = glyph,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = mainColor,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.45f),
                    blurRadius = 6f
                )
            )
        )
    }
}
