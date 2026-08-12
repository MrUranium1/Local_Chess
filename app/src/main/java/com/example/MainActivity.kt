/*
 * Copyright (c) 2026 MrUranium1
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.GameMode
import com.example.model.PieceColor
import com.example.ui.components.AppBackgroundWrapper
import com.example.ui.screens.*
import com.example.ui.theme.ChessAppTheme
import com.example.viewmodel.ChessViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            ChessAppTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChessAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ChessAppContent(viewModel: ChessViewModel) {
    var currentScreen by remember { mutableStateOf("home") }
    var lobbyInitialMode by remember { mutableStateOf(GameMode.BLUETOOTH) }

    val playerName by viewModel.playerName.collectAsStateWithLifecycle()
    val opponentName by viewModel.opponentName.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val gameMode by viewModel.gameMode.collectAsStateWithLifecycle()
    val myAssignedColor by viewModel.myAssignedColor.collectAsStateWithLifecycle()
    val boardTheme by viewModel.boardTheme.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val timeControl by viewModel.timeControl.collectAsStateWithLifecycle()
    val botLevel by viewModel.botLevel.collectAsStateWithLifecycle()
    val selectedPos by viewModel.selectedPosition.collectAsStateWithLifecycle()
    val legalMoves by viewModel.legalMoves.collectAsStateWithLifecycle()
    val pendingPromo by viewModel.pendingPromotionMove.collectAsStateWithLifecycle()
    val drawOfferReceived by viewModel.drawOfferReceived.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
    val customBgImageUri by viewModel.customBackgroundImageUri.collectAsStateWithLifecycle()
    val backgroundDimOpacity by viewModel.backgroundDimOpacity.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.networkGameStartedEvent.collect {
            currentScreen = "play"
        }
    }

    AppBackgroundWrapper(
        bgImageUri = customBgImageUri,
        dimOpacity = backgroundDimOpacity
    ) {
        when (currentScreen) {
            "home" -> {
                HomeScreen(
                    playerName = playerName,
                    onUpdatePlayerName = { viewModel.setPlayerName(it) },
                    onSelectMode = { mode ->
                        when (mode) {
                            GameMode.LOCAL_PASS_PLAY -> {
                                viewModel.startNewGame(GameMode.LOCAL_PASS_PLAY, PieceColor.WHITE, "Player 2")
                                currentScreen = "play"
                            }
                            GameMode.VS_BOT -> {
                                viewModel.startNewGame(GameMode.VS_BOT, PieceColor.WHITE, "Chess Bot (${botLevel.name})")
                                currentScreen = "play"
                            }
                            GameMode.BLUETOOTH, GameMode.WIFI -> {
                                lobbyInitialMode = mode
                                currentScreen = "lobby"
                            }
                        }
                    },
                    onOpenHistory = { currentScreen = "history" },
                    onOpenSettings = { currentScreen = "settings" }
                )
            }

            "lobby" -> {
                MultiplayerLobbyScreen(
                    initialMode = lobbyInitialMode,
                    bluetoothManager = viewModel.bluetoothManager,
                    wifiManager = viewModel.wifiManager,
                    playerName = playerName,
                    viewModel = viewModel,
                    onStartGame = { mode, color, peerName ->
                        viewModel.startNewGame(mode, color, peerName)
                        currentScreen = "play"
                    },
                    onBack = { currentScreen = "home" }
                )
            }

            "play" -> {
                PlayGameScreen(
                    gameState = gameState,
                    gameMode = gameMode,
                    myAssignedColor = myAssignedColor,
                    playerName = playerName,
                    opponentName = opponentName,
                    boardTheme = boardTheme,
                    selectedPosition = selectedPos,
                    legalMoves = legalMoves,
                    pendingPromotionMove = pendingPromo,
                    drawOfferReceived = drawOfferReceived,
                    chatMessages = chatMessages,
                    onSquareClick = { viewModel.selectSquare(it) },
                    onCompletePromotion = { viewModel.completePromotion(it) },
                    onOfferDraw = { viewModel.offerDraw() },
                    onRespondDrawOffer = { viewModel.respondDrawOffer(it) },
                    onResign = { viewModel.resignGame() },
                    onUndoMove = { viewModel.undoMove() },
                    onSendChatMessage = { viewModel.sendChatMessage(it) },
                    onNewGame = {
                        val opp = if (gameMode == GameMode.VS_BOT) "Chess Bot (${botLevel.name})" else opponentName
                        viewModel.startNewGame(gameMode, myAssignedColor, opp)
                    },
                    onBackToHome = { currentScreen = "home" }
                )
            }

            "history" -> {
                MatchHistoryScreen(
                    historyList = matchHistory,
                    onClearHistory = { viewModel.clearHistory() },
                    onBack = { currentScreen = "home" }
                )
            }

            "settings" -> {
                SettingsScreen(
                    currentBoardTheme = boardTheme,
                    currentTimeControl = timeControl,
                    currentBotLevel = botLevel,
                    isDarkMode = isDarkMode,
                    currentBgImageUri = customBgImageUri,
                    currentBgDimOpacity = backgroundDimOpacity,
                    onSelectBoardTheme = { viewModel.setBoardTheme(it) },
                    onSelectTimeControl = { viewModel.setTimeControl(it) },
                    onSelectBotLevel = { viewModel.setBotLevel(it) },
                    onToggleDarkMode = { viewModel.setDarkMode(it) },
                    onSelectBgImageUri = { viewModel.setCustomBackgroundImageUri(it) },
                    onChangeBgDimOpacity = { viewModel.setBackgroundDimOpacity(it) },
                    onBack = { currentScreen = "home" }
                )
            }
        }
    }
}
