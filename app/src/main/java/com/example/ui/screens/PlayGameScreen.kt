package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayGameScreen(
    gameState: ChessBoardState,
    gameMode: GameMode,
    myAssignedColor: PieceColor,
    playerName: String,
    opponentName: String,
    boardTheme: BoardTheme,
    selectedPosition: Position?,
    legalMoves: List<Move>,
    pendingPromotionMove: Pair<Position, Position>?,
    drawOfferReceived: Boolean,
    chatMessages: List<Pair<String, String>>,
    onSquareClick: (Position) -> Unit,
    onCompletePromotion: (PieceType) -> Unit,
    onOfferDraw: () -> Unit,
    onRespondDrawOffer: (Boolean) -> Unit,
    onResign: () -> Unit,
    onUndoMove: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onNewGame: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isBoardFlippedManual by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showResignConfirmDialog by remember { mutableStateOf(false) }

    // Auto-flip board if user plays as Black
    val isFlipped = if (gameMode == GameMode.BLUETOOTH || gameMode == GameMode.WIFI) {
        (myAssignedColor == PieceColor.BLACK) xor isBoardFlippedManual
    } else {
        isBoardFlippedManual
    }

    val isGameOver = gameState.status != GameStatus.WHITE_TURN && gameState.status != GameStatus.BLACK_TURN

    val topPlayerColor = if (isFlipped) PieceColor.WHITE else PieceColor.BLACK
    val bottomPlayerColor = if (isFlipped) PieceColor.BLACK else PieceColor.WHITE

    val topPlayerName = if (topPlayerColor == myAssignedColor) playerName else opponentName
    val bottomPlayerName = if (bottomPlayerColor == myAssignedColor) playerName else opponentName

    val topTimeMs = if (topPlayerColor == PieceColor.WHITE) gameState.whiteTimeMs else gameState.blackTimeMs
    val bottomTimeMs = if (bottomPlayerColor == PieceColor.WHITE) gameState.whiteTimeMs else gameState.blackTimeMs

    val topCaptured = if (topPlayerColor == PieceColor.WHITE) gameState.capturedWhitePieces else gameState.capturedBlackPieces
    val bottomCaptured = if (bottomPlayerColor == PieceColor.WHITE) gameState.capturedWhitePieces else gameState.capturedBlackPieces

    val showToolbarAtTop = (gameMode == GameMode.LOCAL_PASS_PLAY) && (gameState.activeColor == topPlayerColor) && !isGameOver

    val modeTitle = when (gameMode) {
        GameMode.LOCAL_PASS_PLAY -> "Pass & Play"
        GameMode.VS_BOT -> "VS Bot"
        GameMode.BLUETOOTH -> "Bluetooth Match"
        GameMode.WIFI -> "Wi-Fi Match"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = modeTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        if (gameState.isInCheck && !isGameOver) {
                            Text(
                                text = "⚠️ CHECK!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonError
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = GeoTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isBoardFlippedManual = !isBoardFlippedManual }) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Flip Board",
                            tint = GeoTextPrimary
                        )
                    }
                    if (gameMode == GameMode.BLUETOOTH || gameMode == GameMode.WIFI) {
                        IconButton(onClick = { showChatDialog = true }) {
                            BadgedBox(
                                badge = {
                                    if (chatMessages.isNotEmpty()) {
                                        Badge { Text("${chatMessages.size}") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Chat",
                                    tint = GoldPrimary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP PLAYER (Opponent or Top Player)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedVisibility(
                    visible = showToolbarAtTop,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    GameActionToolbar(
                        gameMode = gameMode,
                        gameState = gameState,
                        isGameOver = isGameOver,
                        onUndoMove = onUndoMove,
                        onOfferDraw = onOfferDraw,
                        onResignConfirm = { showResignConfirmDialog = true },
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .graphicsLayer { rotationZ = 180f }
                    )
                }

                ChessClockView(
                    playerName = topPlayerName,
                    playerColor = topPlayerColor,
                    timeMs = topTimeMs,
                    isActiveTurn = gameState.activeColor == topPlayerColor && !isGameOver,
                    isUnlimited = gameState.timeControl.initialMinutes == 0
                )
                CapturedPiecesView(
                    capturedPieces = topCaptured,
                    capturedByColor = topPlayerColor,
                    otherCapturedPieces = bottomCaptured
                )
            }

            // MOVE HISTORY LOG STRIP
            MoveHistoryView(
                moves = gameState.moveHistory,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // CHESS BOARD CANVAS
            ChessBoardView(
                boardState = gameState,
                boardTheme = boardTheme,
                isFlipped = isFlipped,
                selectedPos = selectedPosition,
                legalMoves = legalMoves,
                onSquareClick = onSquareClick,
                modifier = Modifier.fillMaxWidth()
            )

            // BOTTOM PLAYER (User or Bottom Player)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CapturedPiecesView(
                    capturedPieces = bottomCaptured,
                    capturedByColor = bottomPlayerColor,
                    otherCapturedPieces = topCaptured
                )
                ChessClockView(
                    playerName = bottomPlayerName,
                    playerColor = bottomPlayerColor,
                    timeMs = bottomTimeMs,
                    isActiveTurn = gameState.activeColor == bottomPlayerColor && !isGameOver,
                    isUnlimited = gameState.timeControl.initialMinutes == 0
                )

                AnimatedVisibility(
                    visible = !showToolbarAtTop,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    GameActionToolbar(
                        gameMode = gameMode,
                        gameState = gameState,
                        isGameOver = isGameOver,
                        onUndoMove = onUndoMove,
                        onOfferDraw = onOfferDraw,
                        onResignConfirm = { showResignConfirmDialog = true },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    // PAWN PROMOTION MODAL
    if (pendingPromotionMove != null) {
        val promoColor = gameState.activeColor
        PawnPromotionDialog(
            color = promoColor,
            onSelectPromotion = onCompletePromotion
        )
    }

    // QUICK CHAT MODAL
    if (showChatDialog) {
        QuickChatDialog(
            onSendChat = onSendChatMessage,
            onDismiss = { showChatDialog = false }
        )
    }

    // DRAW OFFER DIALOG
    if (drawOfferReceived) {
        AlertDialog(
            onDismissRequest = { onRespondDrawOffer(false) },
            title = { Text("Draw Offered", color = GoldPrimary) },
            text = { Text("Your opponent has offered a draw. Accept?", color = GeoTextPrimary) },
            confirmButton = {
                Button(
                    onClick = { onRespondDrawOffer(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("Accept Draw", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onRespondDrawOffer(false) }) {
                    Text("Decline", color = GeoTextPrimary)
                }
            },
            containerColor = SlateDarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // RESIGN CONFIRMATION DIALOG
    if (showResignConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResignConfirmDialog = false },
            title = { Text("Resign Match?", color = CrimsonError) },
            text = { Text("Are you sure you want to forfeit this match?", color = GeoTextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        showResignConfirmDialog = false
                        onResign()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError)
                ) {
                    Text("Resign", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResignConfirmDialog = false }) {
                    Text("Cancel", color = GeoTextPrimary)
                }
            },
            containerColor = SlateDarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // GAME OVER SUMMARY MODAL
    if (isGameOver) {
        val resultMessage = when (gameState.status) {
            GameStatus.WHITE_WIN_CHECKMATE -> "White won by Checkmate! 👑"
            GameStatus.BLACK_WIN_CHECKMATE -> "Black won by Checkmate! 👑"
            GameStatus.WHITE_WIN_TIMEOUT -> "White won on Time! ⏱️"
            GameStatus.BLACK_WIN_TIMEOUT -> "Black won on Time! ⏱️"
            GameStatus.DRAW_STALEMATE -> "Game ended in Stalemate! 🤝"
            GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "Draw by Insufficient Material 🤝"
            GameStatus.DRAW_AGREEMENT -> "Draw by Mutual Agreement 🤝"
            GameStatus.WHITE_RESIGNED -> "Black won (White Resigned) 🚩"
            GameStatus.BLACK_RESIGNED -> "White won (Black Resigned) 🚩"
            else -> "Game Over"
        }

        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "MATCH ENDED",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = resultMessage,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Total Moves: ${gameState.moveHistory.size}",
                        fontSize = 13.sp,
                        color = GeoTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onNewGame,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Play Again", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Exit to Menu", color = GeoTextPrimary)
                }
            },
            containerColor = SlateDarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun GameActionToolbar(
    gameMode: GameMode,
    gameState: ChessBoardState,
    isGameOver: Boolean,
    onUndoMove: () -> Unit,
    onOfferDraw: () -> Unit,
    onResignConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SlateDarkCard)
            .border(1.dp, GeoBorderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (gameMode == GameMode.LOCAL_PASS_PLAY || gameMode == GameMode.VS_BOT) {
            OutlinedButton(
                onClick = onUndoMove,
                enabled = gameState.moveHistory.isNotEmpty() && !isGameOver,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GoldPrimary,
                    disabledContentColor = GeoTextSecondary.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (gameState.moveHistory.isNotEmpty() && !isGameOver) GoldPrimary.copy(alpha = 0.6f) else GeoBorderColor.copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Undo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }

        OutlinedButton(
            onClick = onOfferDraw,
            enabled = !isGameOver,
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = GeoTextPrimary,
                disabledContentColor = GeoTextSecondary.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, if (!isGameOver) GeoBorderColor else GeoBorderColor.copy(alpha = 0.4f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (!isGameOver) GoldPrimary else GeoTextSecondary.copy(alpha = 0.5f))
                Spacer(Modifier.width(3.dp))
                Text("Draw", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }

        Button(
            onClick = onResignConfirm,
            enabled = !isGameOver,
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CrimsonError,
                disabledContainerColor = CrimsonError.copy(alpha = 0.3f),
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(Modifier.width(3.dp))
                Text("Resign", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
            }
        }
    }
}

