package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Piece
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.ui.theme.*

@Composable
fun CapturedPiecesView(
    capturedPieces: List<Piece>,
    capturedByColor: PieceColor,
    otherCapturedPieces: List<Piece>,
    modifier: Modifier = Modifier
) {
    // Calculate total material value captured
    val myVal = capturedPieces.sumOf { it.type.value }
    val oppVal = otherCapturedPieces.sumOf { it.type.value }
    val diff = myVal - oppVal

    // Group captured pieces by piece type, ordered from highest value to lowest
    val groupedPieces = capturedPieces
        .groupBy { it.type }
        .toList()
        .sortedByDescending { it.first.value }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SlateDarkCard)
            .border(1.dp, GeoBorderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (capturedPieces.isEmpty()) {
            Text(
                text = "No captures",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GeoTextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        } else {
            LazyRow(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(groupedPieces) { (type, piecesGroup) ->
                    val samplePiece = piecesGroup.first()
                    // Captured piece color
                    val isCapturedPieceWhite = samplePiece.color == PieceColor.WHITE
                    
                    val pieceTextColor = if (isCapturedPieceWhite) {
                        Color(0xFFE8ECE1) // Bright off-white for captured white pieces
                    } else {
                        Color(0xFF232B1E) // Dark charcoal for captured black pieces
                    }

                    val pieceBgColor = if (isCapturedPieceWhite) {
                        Color(0xFF55624C) // Sage background chip for white pieces
                    } else {
                        Color(0xFFD2D8C6) // Light background chip for black pieces
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-4).dp)
                    ) {
                        // Display pieces of same type stacked/adjacent
                        piecesGroup.forEach { piece ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(pieceBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = piece.displaySymbol,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pieceTextColor
                                )
                            }
                        }
                    }
                }
            }
        }

        if (diff > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmberAccent)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$diff",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF121410)
                )
            }
        }
    }
}

