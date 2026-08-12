package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SlateDarkCard
import com.example.ui.theme.SlateDarkSurface

@Composable
fun PawnPromotionDialog(
    color: PieceColor,
    onSelectPromotion: (PieceType) -> Unit
) {
    val options = listOf(
        PieceType.QUEEN,
        PieceType.ROOK,
        PieceType.BISHOP,
        PieceType.KNIGHT
    )

    AlertDialog(
        onDismissRequest = { /* Force selection */ },
        title = {
            Text(
                text = "Promote Pawn",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEach { pieceType ->
                    val piece = Piece(pieceType, color)
                    Card(
                        modifier = Modifier
                            .size(60.dp)
                            .clickable { onSelectPromotion(pieceType) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateDarkCard)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = piece.unicodeSymbol,
                                fontSize = 32.sp,
                                color = if (color == PieceColor.WHITE) Color.White else Color.LightGray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = SlateDarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
