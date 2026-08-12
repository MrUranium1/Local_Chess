package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChessDatabase
import com.example.data.MatchHistoryEntity
import com.example.data.MatchHistoryRepository
import com.example.engine.ChessBot
import com.example.engine.ChessEngine
import com.example.model.*
import com.example.network.BluetoothManager
import com.example.network.ConnectionState
import com.example.network.MessageType
import com.example.network.NetworkMessage
import com.example.network.WifiManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MatchHistoryRepository
    val bluetoothManager: BluetoothManager = BluetoothManager(application)
    val wifiManager: WifiManager = WifiManager(application)

    init {
        val db = ChessDatabase.getDatabase(application)
        repository = MatchHistoryRepository(db.matchHistoryDao())
    }

    val matchHistory: StateFlow<List<MatchHistoryEntity>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val prefs = application.getSharedPreferences("chess_app_settings", Context.MODE_PRIVATE)

    private val _customBackgroundImageUri = MutableStateFlow<String?>(
        prefs.getString("custom_bg_uri", null)
    )
    val customBackgroundImageUri: StateFlow<String?> = _customBackgroundImageUri

    private val _backgroundDimOpacity = MutableStateFlow(
        prefs.getFloat("bg_dim_opacity", 0.45f)
    )
    val backgroundDimOpacity: StateFlow<Float> = _backgroundDimOpacity

    fun setCustomBackgroundImageUri(uri: String?) {
        _customBackgroundImageUri.value = uri
        if (uri == null) {
            prefs.edit().remove("custom_bg_uri").apply()
        } else {
            prefs.edit().putString("custom_bg_uri", uri).apply()
        }
    }

    fun setBackgroundDimOpacity(opacity: Float) {
        _backgroundDimOpacity.value = opacity
        prefs.edit().putFloat("bg_dim_opacity", opacity).apply()
    }

    // Game configuration state
    private val _gameMode = MutableStateFlow(GameMode.LOCAL_PASS_PLAY)
    val gameMode: StateFlow<GameMode> = _gameMode

    private val _myAssignedColor = MutableStateFlow(PieceColor.WHITE)
    val myAssignedColor: StateFlow<PieceColor> = _myAssignedColor

    private val _botLevel = MutableStateFlow(BotLevel.MEDIUM)
    val botLevel: StateFlow<BotLevel> = _botLevel

    private val _boardTheme = MutableStateFlow(BoardTheme.WOOD)
    val boardTheme: StateFlow<BoardTheme> = _boardTheme

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _timeControl = MutableStateFlow(TimeControl(10, 0))
    val timeControl: StateFlow<TimeControl> = _timeControl

    private val _selectedPosition = MutableStateFlow<Position?>(null)
    val selectedPosition: StateFlow<Position?> = _selectedPosition

    private val _legalMoves = MutableStateFlow<List<Move>>(emptyList())
    val legalMoves: StateFlow<List<Move>> = _legalMoves

    private val _gameState = MutableStateFlow(ChessEngine.getInitialState(TimeControl(10, 0)))
    val gameState: StateFlow<ChessBoardState> = _gameState

    private val _pendingPromotionMove = MutableStateFlow<Pair<Position, Position>?>(null)
    val pendingPromotionMove: StateFlow<Pair<Position, Position>?> = _pendingPromotionMove

    private val _chatMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // (senderName, text)
    val chatMessages: StateFlow<List<Pair<String, String>>> = _chatMessages

    private val _drawOfferReceived = MutableStateFlow(false)
    val drawOfferReceived: StateFlow<Boolean> = _drawOfferReceived

    private val _playerName = MutableStateFlow("Player 1")
    val playerName: StateFlow<String> = _playerName

    private val _opponentName = MutableStateFlow("Player 2")
    val opponentName: StateFlow<String> = _opponentName

    private val _networkGameStartedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val networkGameStartedEvent = _networkGameStartedEvent.asSharedFlow()

    private val _authRejectedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authRejectedEvent = _authRejectedEvent.asSharedFlow()

    private var hostPasswordProtection = false
    private var hostPasswordString = ""
    private var clientJoinPassword = ""
    private var hostPreferredColor: PieceColor = PieceColor.WHITE

    fun setHostPassword(protected: Boolean, password: String) {
        hostPasswordProtection = protected
        hostPasswordString = password
    }

    fun setHostPreferredColor(color: PieceColor) {
        hostPreferredColor = color
    }

    fun setClientJoinPassword(password: String) {
        clientJoinPassword = password
    }

    fun sendJoinRequest(mode: GameMode) {
        _gameMode.value = mode
        val msg = NetworkMessage(
            type = MessageType.JOIN,
            senderName = _playerName.value,
            password = clientJoinPassword
        )
        if (mode == GameMode.BLUETOOTH) {
            bluetoothManager.sendMessage(msg)
        } else if (mode == GameMode.WIFI) {
            wifiManager.sendMessage(msg)
        }
    }

    private var timerJob: Job? = null
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
    }

    init {
        // Observe Bluetooth messages
        viewModelScope.launch {
            bluetoothManager.incomingMessages.collect { msg ->
                handleNetworkMessage(msg)
            }
        }

        // Observe Wi-Fi messages
        viewModelScope.launch {
            wifiManager.incomingMessages.collect { msg ->
                handleNetworkMessage(msg)
            }
        }
    }

    fun setPlayerName(name: String) {
        _playerName.value = name
    }

    fun setBoardTheme(theme: BoardTheme) {
        _boardTheme.value = theme
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
    }

    fun setTimeControl(tc: TimeControl) {
        _timeControl.value = tc
    }

    fun setBotLevel(level: BotLevel) {
        _botLevel.value = level
    }

    fun startNewGame(mode: GameMode, myColor: PieceColor = PieceColor.WHITE, oppName: String = "Opponent") {
        _gameMode.value = mode
        _myAssignedColor.value = myColor
        _opponentName.value = oppName
        _selectedPosition.value = null
        _legalMoves.value = emptyList()
        _chatMessages.value = emptyList()
        _drawOfferReceived.value = false

        val initial = ChessEngine.getInitialState(_timeControl.value)
        _gameState.value = initial

        startTimer()

        // Send START_GAME payload to client if we are host in a network game
        val isNetwork = mode == GameMode.BLUETOOTH || mode == GameMode.WIFI
        val isHost = if (mode == GameMode.BLUETOOTH) bluetoothManager.isHost else if (mode == GameMode.WIFI) wifiManager.isHost else true
        if (isNetwork && isHost) {
            val netMsg = NetworkMessage(
                type = MessageType.START_GAME,
                senderName = _playerName.value,
                assignedColor = myColor.opposite().name,
                timeMinutes = _timeControl.value.initialMinutes
            )
            if (mode == GameMode.BLUETOOTH) {
                bluetoothManager.sendMessage(netMsg)
            } else {
                wifiManager.sendMessage(netMsg)
            }
            _networkGameStartedEvent.tryEmit(Unit)
        }

        // If VS Bot and Bot is White, trigger Bot move immediately
        if (mode == GameMode.VS_BOT && myColor == PieceColor.BLACK) {
            triggerBotMove()
        }
    }

    fun selectSquare(pos: Position) {
        val currentState = _gameState.value

        // Check if game is over
        if (currentState.status != GameStatus.WHITE_TURN && currentState.status != GameStatus.BLACK_TURN) {
            return
        }

        // Check if it's my turn in network or bot mode
        val mode = _gameMode.value
        val currentTurn = currentState.activeColor

        if (mode == GameMode.BLUETOOTH || mode == GameMode.WIFI) {
            if (currentTurn != _myAssignedColor.value) return
        } else if (mode == GameMode.VS_BOT) {
            if (currentTurn != _myAssignedColor.value) return
        }

        val currentSelected = _selectedPosition.value

        if (currentSelected == null) {
            // Select piece if it belongs to active color
            val piece = currentState.getPiece(pos)
            if (piece != null && piece.color == currentTurn) {
                _selectedPosition.value = pos
                _legalMoves.value = ChessEngine.getLegalMoves(currentState, pos)
            }
        } else {
            // Attempt move
            val matchingMove = _legalMoves.value.firstOrNull { it.to == pos }
            if (matchingMove != null) {
                // Check for pawn promotion requirement
                if (matchingMove.piece.type == PieceType.PAWN && (pos.row == 0 || pos.row == 7) && matchingMove.promotion == null) {
                    _pendingPromotionMove.value = Pair(currentSelected, pos)
                } else {
                    executeMove(matchingMove)
                }
            } else {
                // Switch selection to another piece of active color or unselect
                val piece = currentState.getPiece(pos)
                if (piece != null && piece.color == currentTurn) {
                    _selectedPosition.value = pos
                    _legalMoves.value = ChessEngine.getLegalMoves(currentState, pos)
                } else {
                    _selectedPosition.value = null
                    _legalMoves.value = emptyList()
                }
            }
        }
    }

    fun completePromotion(promotionType: PieceType) {
        val promoPair = _pendingPromotionMove.value ?: return
        val from = promoPair.first
        val to = promoPair.second
        _pendingPromotionMove.value = null

        val currentState = _gameState.value
        val piece = currentState.getPiece(from) ?: return
        val targetPiece = currentState.getPiece(to)

        val move = Move(
            from = from,
            to = to,
            piece = piece,
            capturedPiece = targetPiece,
            promotion = promotionType
        )

        executeMove(move)
    }

    private fun executeMove(move: Move) {
        _selectedPosition.value = null
        _legalMoves.value = emptyList()

        val prevState = _gameState.value
        val nextState = ChessEngine.makeMove(prevState, move)
        _gameState.value = nextState

        vibrateFeedback(if (nextState.isInCheck) 120L else 40L)

        // Send move to network peer if multiplayer
        if (_gameMode.value == GameMode.BLUETOOTH) {
            val netMsg = NetworkMessage(
                type = MessageType.MOVE,
                senderName = _playerName.value,
                fromRow = move.from.row,
                fromCol = move.from.col,
                toRow = move.to.row,
                toCol = move.to.col,
                promotion = move.promotion,
                san = move.san
            )
            bluetoothManager.sendMessage(netMsg)
        } else if (_gameMode.value == GameMode.WIFI) {
            val netMsg = NetworkMessage(
                type = MessageType.MOVE,
                senderName = _playerName.value,
                fromRow = move.from.row,
                fromCol = move.from.col,
                toRow = move.to.row,
                toCol = move.to.col,
                promotion = move.promotion,
                san = move.san
            )
            wifiManager.sendMessage(netMsg)
        }

        checkGameEnd(nextState)

        // Trigger Bot response if VS Bot
        if (_gameMode.value == GameMode.VS_BOT && (nextState.status == GameStatus.WHITE_TURN || nextState.status == GameStatus.BLACK_TURN)) {
            if (nextState.activeColor != _myAssignedColor.value) {
                triggerBotMove()
            }
        }
    }

    private fun triggerBotMove() {
        viewModelScope.launch {
            delay(500) // Realistic bot think delay
            val currentState = _gameState.value
            val botMove = ChessBot.selectMove(currentState, _botLevel.value)
            if (botMove != null) {
                executeMove(botMove)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        if (_timeControl.value.initialMinutes == 0) return // Unlimited time

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _gameState.value
                if (state.status != GameStatus.WHITE_TURN && state.status != GameStatus.BLACK_TURN) {
                    break
                }

                if (state.activeColor == PieceColor.WHITE) {
                    val newTime = (state.whiteTimeMs - 1000L).coerceAtLeast(0)
                    if (newTime == 0L) {
                        val endState = state.copy(status = GameStatus.BLACK_WIN_TIMEOUT, whiteTimeMs = 0)
                        _gameState.value = endState
                        checkGameEnd(endState)
                        break
                    } else {
                        _gameState.value = state.copy(whiteTimeMs = newTime)
                    }
                } else {
                    val newTime = (state.blackTimeMs - 1000L).coerceAtLeast(0)
                    if (newTime == 0L) {
                        val endState = state.copy(status = GameStatus.WHITE_WIN_TIMEOUT, blackTimeMs = 0)
                        _gameState.value = endState
                        checkGameEnd(endState)
                        break
                    } else {
                        _gameState.value = state.copy(blackTimeMs = newTime)
                    }
                }
            }
        }
    }

    fun offerDraw() {
        _drawOfferReceived.value = false
        if (_gameMode.value == GameMode.BLUETOOTH) {
            bluetoothManager.sendMessage(NetworkMessage(type = MessageType.DRAW_OFFER, senderName = _playerName.value))
        } else if (_gameMode.value == GameMode.WIFI) {
            wifiManager.sendMessage(NetworkMessage(type = MessageType.DRAW_OFFER, senderName = _playerName.value))
        } else {
            // Local mode: Draw agreement directly
            val endState = _gameState.value.copy(status = GameStatus.DRAW_AGREEMENT)
            _gameState.value = endState
            checkGameEnd(endState)
        }
    }

    fun respondDrawOffer(accept: Boolean) {
        _drawOfferReceived.value = false
        if (accept) {
            val endState = _gameState.value.copy(status = GameStatus.DRAW_AGREEMENT)
            _gameState.value = endState
            checkGameEnd(endState)
        }

        val msg = NetworkMessage(type = MessageType.DRAW_RESPONSE, accepted = accept, senderName = _playerName.value)
        if (_gameMode.value == GameMode.BLUETOOTH) {
            bluetoothManager.sendMessage(msg)
        } else if (_gameMode.value == GameMode.WIFI) {
            wifiManager.sendMessage(msg)
        }
    }

    fun resignGame() {
        val current = _gameState.value
        val resColor = if (_gameMode.value == GameMode.LOCAL_PASS_PLAY) current.activeColor else _myAssignedColor.value
        val status = if (resColor == PieceColor.WHITE) GameStatus.WHITE_RESIGNED else GameStatus.BLACK_RESIGNED

        val endState = current.copy(status = status)
        _gameState.value = endState

        if (_gameMode.value == GameMode.BLUETOOTH) {
            bluetoothManager.sendMessage(NetworkMessage(type = MessageType.RESIGN, senderName = _playerName.value))
        } else if (_gameMode.value == GameMode.WIFI) {
            wifiManager.sendMessage(NetworkMessage(type = MessageType.RESIGN, senderName = _playerName.value))
        }

        checkGameEnd(endState)
    }

    fun undoMove() {
        val state = _gameState.value
        if (state.moveHistory.isEmpty()) return
        if (_gameMode.value == GameMode.BLUETOOTH || _gameMode.value == GameMode.WIFI) return // Undo disabled in online multiplayer

        // If vs Bot, undo both Bot's move and player's move
        val stepsToUndo = if (_gameMode.value == GameMode.VS_BOT && state.moveHistory.size >= 2) 2 else 1
        var replayedState = ChessEngine.getInitialState(_timeControl.value)

        val remainingMoves = state.moveHistory.dropLast(stepsToUndo)
        for (m in remainingMoves) {
            replayedState = ChessEngine.makeMove(replayedState, m)
        }

        _gameState.value = replayedState
        _selectedPosition.value = null
        _legalMoves.value = emptyList()
    }

    fun sendChatMessage(text: String) {
        val msgList = _chatMessages.value.toMutableList()
        msgList.add(Pair(_playerName.value, text))
        _chatMessages.value = msgList

        val netMsg = NetworkMessage(type = MessageType.CHAT, senderName = _playerName.value, chatText = text)
        if (_gameMode.value == GameMode.BLUETOOTH) {
            bluetoothManager.sendMessage(netMsg)
        } else if (_gameMode.value == GameMode.WIFI) {
            wifiManager.sendMessage(netMsg)
        }
    }

    private fun handleNetworkMessage(msg: NetworkMessage) {
        when (msg.type) {
            MessageType.JOIN -> {
                val isBtHost = bluetoothManager.isHost
                val isWifiHost = wifiManager.isHost
                val isHost = isBtHost || isWifiHost
                val mode = if (isBtHost) GameMode.BLUETOOTH else GameMode.WIFI
                if (isHost) {
                    if (hostPasswordProtection && msg.password != hostPasswordString) {
                        val rejectMsg = NetworkMessage(
                            type = MessageType.AUTH_REJECT,
                            rejectReason = if (msg.password.isBlank()) "This host requires a password to join." else "Incorrect password! Please try again."
                        )
                        if (mode == GameMode.BLUETOOTH) {
                            bluetoothManager.sendMessage(rejectMsg)
                        } else {
                            wifiManager.sendMessage(rejectMsg)
                        }
                    } else {
                        // Password is valid or not required. Start match!
                        startNewGame(mode, myColor = hostPreferredColor, oppName = msg.senderName)
                    }
                }
            }

            MessageType.AUTH_REJECT -> {
                val mode = _gameMode.value
                if (mode == GameMode.BLUETOOTH) {
                    bluetoothManager.disconnect()
                } else if (mode == GameMode.WIFI) {
                    wifiManager.disconnect()
                }
                _authRejectedEvent.tryEmit(msg.rejectReason.ifBlank { "Password authentication failed." })
            }

            MessageType.START_GAME -> {
                val assigned = if (msg.assignedColor == "WHITE") PieceColor.WHITE else PieceColor.BLACK
                val timeMins = msg.timeMinutes
                setTimeControl(TimeControl(timeMins, 0))
                
                _myAssignedColor.value = assigned
                _opponentName.value = msg.senderName
                _selectedPosition.value = null
                _legalMoves.value = emptyList()
                _chatMessages.value = emptyList()
                _drawOfferReceived.value = false

                val initial = ChessEngine.getInitialState(_timeControl.value)
                _gameState.value = initial

                startTimer()
                _networkGameStartedEvent.tryEmit(Unit)
            }

            MessageType.MOVE -> {
                val from = Position(msg.fromRow, msg.fromCol)
                val to = Position(msg.toRow, msg.toCol)
                val piece = _gameState.value.getPiece(from)
                val capPiece = _gameState.value.getPiece(to)

                if (piece != null) {
                    val move = Move(
                        from = from,
                        to = to,
                        piece = piece,
                        capturedPiece = capPiece,
                        promotion = msg.promotion
                    )
                    executeMove(move)
                }
            }

            MessageType.DRAW_OFFER -> {
                _drawOfferReceived.value = true
            }

            MessageType.DRAW_RESPONSE -> {
                if (msg.accepted) {
                    val endState = _gameState.value.copy(status = GameStatus.DRAW_AGREEMENT)
                    _gameState.value = endState
                    checkGameEnd(endState)
                }
            }

            MessageType.RESIGN -> {
                val oppColor = _myAssignedColor.value.opposite()
                val status = if (oppColor == PieceColor.WHITE) GameStatus.WHITE_RESIGNED else GameStatus.BLACK_RESIGNED
                val endState = _gameState.value.copy(status = status)
                _gameState.value = endState
                checkGameEnd(endState)
            }

            MessageType.CHAT -> {
                val msgList = _chatMessages.value.toMutableList()
                msgList.add(Pair(msg.senderName, msg.chatText))
                _chatMessages.value = msgList
            }

            else -> {}
        }
    }

    private fun checkGameEnd(state: ChessBoardState) {
        if (state.status == GameStatus.WHITE_TURN || state.status == GameStatus.BLACK_TURN) return

        timerJob?.cancel()

        val resultStr = when (state.status) {
            GameStatus.WHITE_WIN_CHECKMATE -> "White Won by Checkmate"
            GameStatus.BLACK_WIN_CHECKMATE -> "Black Won by Checkmate"
            GameStatus.WHITE_WIN_TIMEOUT -> "White Won on Time"
            GameStatus.BLACK_WIN_TIMEOUT -> "Black Won on Time"
            GameStatus.DRAW_STALEMATE -> "Draw by Stalemate"
            GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "Draw by Insufficient Material"
            GameStatus.DRAW_FIFTY_MOVES -> "Draw by 50-Move Rule"
            GameStatus.DRAW_AGREEMENT -> "Draw by Mutual Agreement"
            GameStatus.WHITE_RESIGNED -> "Black Won (White Resigned)"
            GameStatus.BLACK_RESIGNED -> "White Won (Black Resigned)"
            else -> "Game Over"
        }

        val pgnString = state.moveHistory.mapIndexed { idx, m ->
            if (idx % 2 == 0) "${(idx / 2) + 1}. ${m.san}" else m.san
        }.joinToString(" ")

        viewModelScope.launch {
            repository.insertMatch(
                MatchHistoryEntity(
                    mode = _gameMode.value.name,
                    opponentName = _opponentName.value,
                    result = resultStr,
                    userColor = _myAssignedColor.value.name,
                    movesCount = state.moveHistory.size,
                    pgn = pgnString
                )
            )
        }

        vibrateFeedback(300L)
    }

    private fun vibrateFeedback(ms: Long) {
        try {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(ms, DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(ms)
                }
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
