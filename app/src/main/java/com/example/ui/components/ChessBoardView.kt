package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun ChessBoardView(
    boardState: ChessBoardState,
    boardTheme: BoardTheme = BoardTheme.WOOD,
    isFlipped: Boolean = false,
    selectedPos: Position?,
    legalMoves: List<Move>,
    onSquareClick: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    val (lightSquareColor, darkSquareColor) = when (boardTheme) {
        BoardTheme.WOOD -> Pair(WoodLightSquare, WoodDarkSquare)
        BoardTheme.SLATE -> Pair(SlateLightSquare, SlateDarkSquare)
        BoardTheme.EMERALD -> Pair(EmeraldLightSquare, EmeraldDarkSquare)
        BoardTheme.OCEAN -> Pair(OceanLightSquare, OceanDarkSquare)
        BoardTheme.AMETHYST -> Pair(AmethystLightSquare, AmethystDarkSquare)
        BoardTheme.CORAL -> Pair(CoralLightSquare, CoralDarkSquare)
        BoardTheme.SAND -> Pair(SandLightSquare, SandDarkSquare)
        BoardTheme.CYBER -> Pair(CyberLightSquare, CyberDarkSquare)
    }

    val rows = if (isFlipped) (7 downTo 0).toList() else (0..7).toList()
    val cols = if (isFlipped) (7 downTo 0).toList() else (0..7).toList()

    val legalMoveToPositions = legalMoves.map { it.to }.toSet()

    // Pulse animation for check aura
    val infiniteTransition = rememberInfiniteTransition(label = "checkPulse")
    val checkPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "checkPulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(SlateDarkCard)
            .border(3.dp, BoardFrameBorder, RoundedCornerShape(16.dp))
            .padding(3.dp)
            .testTag("chess_board")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val squareSize = maxWidth / 8

            Column(modifier = Modifier.fillMaxSize()) {
                for (r in rows) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        for (c in cols) {
                            val pos = Position(r, c)
                            val piece = boardState.getPiece(pos)
                            val isLightSquare = (r + c) % 2 == 0

                            val isSelected = selectedPos == pos
                            val isLegalMove = legalMoveToPositions.contains(pos)
                            val isLastMoveFrom = boardState.lastMove?.from == pos
                            val isLastMoveTo = boardState.lastMove?.to == pos
                            val isCheckedKing = boardState.isInCheck && boardState.checkedKingPos == pos

                            val baseColor = if (isLightSquare) lightSquareColor else darkSquareColor
                            val squareBgColor by animateColorAsState(
                                targetValue = when {
                                    isCheckedKing -> CrimsonError.copy(alpha = checkPulseAlpha)
                                    isSelected -> HighlightSelectedSquare
                                    isLastMoveFrom || isLastMoveTo -> HighlightLastMove
                                    else -> baseColor
                                },
                                animationSpec = tween(150),
                                label = "squareColor"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(squareBgColor)
                                    .clickable { onSquareClick(pos) }
                                    .testTag("square_${r}_${c}"),
                                contentAlignment = Alignment.Center
                            ) {
                                // Subtle inner square gradient/border effect
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(2.5.dp, GoldPrimary, RoundedCornerShape(2.dp))
                                    )
                                } else if (isLastMoveTo || isLastMoveFrom) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(1.5.dp, Color(0xFFFACC15).copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                    )
                                }

                                // Rank number on left edge (file 0 or 7 depending on flip)
                                if (c == (if (isFlipped) 7 else 0)) {
                                    Text(
                                        text = "${8 - r}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isLightSquare) darkSquareColor.copy(alpha = 0.85f) else lightSquareColor.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 2.dp, top = 1.dp)
                                    )
                                }

                                // File letter on bottom edge (row 7 or 0 depending on flip)
                                if (r == (if (isFlipped) 0 else 7)) {
                                    Text(
                                        text = "${('a' + c)}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isLightSquare) darkSquareColor.copy(alpha = 0.85f) else lightSquareColor.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 2.dp, bottom = 1.dp)
                                    )
                                }

                                // Piece
                                if (piece != null) {
                                    ChessPieceView(
                                        piece = piece,
                                        sizeDp = squareSize,
                                        isSelected = isSelected
                                    )
                                }

                                // Legal move indicator
                                if (isLegalMove) {
                                    if (piece != null) {
                                        // Capture Target Ring
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.88f)
                                                .border(3.5.dp, CrimsonError.copy(alpha = 0.85f), CircleShape)
                                        )
                                    } else {
                                        // Empty Square Move Dot
                                        Box(
                                            modifier = Modifier
                                                .size(squareSize * 0.32f)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        listOf(
                                                            HighlightLegalMoveDot,
                                                            HighlightLegalMoveDot.copy(alpha = 0.7f)
                                                        )
                                                    )
                                                )
                                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
