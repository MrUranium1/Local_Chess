package com.example.model

enum class GameStatus {
    WHITE_TURN,
    BLACK_TURN,
    WHITE_WIN_CHECKMATE,
    BLACK_WIN_CHECKMATE,
    WHITE_WIN_TIMEOUT,
    BLACK_WIN_TIMEOUT,
    DRAW_STALEMATE,
    DRAW_INSUFFICIENT_MATERIAL,
    DRAW_FIFTY_MOVES,
    DRAW_AGREEMENT,
    WHITE_RESIGNED,
    BLACK_RESIGNED
}

enum class GameMode {
    LOCAL_PASS_PLAY,
    VS_BOT,
    BLUETOOTH,
    WIFI
}

enum class BotLevel {
    EASY,
    MEDIUM,
    HARD
}

enum class BoardTheme {
    WOOD,
    SLATE,
    EMERALD,
    OCEAN,
    AMETHYST,
    CORAL,
    SAND,
    CYBER
}

data class TimeControl(
    val initialMinutes: Int = 10,
    val incrementSeconds: Int = 0,
    val displayName: String = if (initialMinutes == 0) "Unlimited" else "$initialMinutes min"
)

data class ChessBoardState(
    val board: Array<Array<Piece?>> = Array(8) { Array(8) { null } },
    val activeColor: PieceColor = PieceColor.WHITE,
    val status: GameStatus = GameStatus.WHITE_TURN,
    val moveHistory: List<Move> = emptyList(),
    val capturedWhitePieces: List<Piece> = emptyList(), // Pieces captured BY white (black pieces)
    val capturedBlackPieces: List<Piece> = emptyList(), // Pieces captured BY black (white pieces)
    val whiteTimeMs: Long = 10 * 60 * 1000L,
    val blackTimeMs: Long = 10 * 60 * 1000L,
    val timeControl: TimeControl = TimeControl(10, 0),
    val isInCheck: Boolean = false,
    val checkedKingPos: Position? = null,
    val lastMove: Move? = null,
    val whiteCanCastleKingside: Boolean = true,
    val whiteCanCastleQueenside: Boolean = true,
    val blackCanCastleKingside: Boolean = true,
    val blackCanCastleQueenside: Boolean = true,
    val enPassantTarget: Position? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val drawOfferedBy: PieceColor? = null
) {
    fun getPiece(pos: Position): Piece? {
        if (!pos.isValid()) return null
        return board[pos.row][pos.col]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChessBoardState) return false
        return status == other.status &&
                activeColor == other.activeColor &&
                moveHistory.size == other.moveHistory.size &&
                whiteTimeMs == other.whiteTimeMs &&
                blackTimeMs == other.blackTimeMs
    }

    override fun hashCode(): Int {
        var result = activeColor.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + moveHistory.size
        return result
    }

    companion object {
        fun createInitialBoard(): Array<Array<Piece?>> {
            val b = Array(8) { Array<Piece?>(8) { null } }

            // Black pieces (rows 0, 1)
            b[0][0] = Piece(PieceType.ROOK, PieceColor.BLACK)
            b[0][1] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
            b[0][2] = Piece(PieceType.BISHOP, PieceColor.BLACK)
            b[0][3] = Piece(PieceType.QUEEN, PieceColor.BLACK)
            b[0][4] = Piece(PieceType.KING, PieceColor.BLACK)
            b[0][5] = Piece(PieceType.BISHOP, PieceColor.BLACK)
            b[0][6] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
            b[0][7] = Piece(PieceType.ROOK, PieceColor.BLACK)
            for (col in 0..7) {
                b[1][col] = Piece(PieceType.PAWN, PieceColor.BLACK)
            }

            // White pieces (rows 6, 7)
            for (col in 0..7) {
                b[6][col] = Piece(PieceType.PAWN, PieceColor.WHITE)
            }
            b[7][0] = Piece(PieceType.ROOK, PieceColor.WHITE)
            b[7][1] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
            b[7][2] = Piece(PieceType.BISHOP, PieceColor.WHITE)
            b[7][3] = Piece(PieceType.QUEEN, PieceColor.WHITE)
            b[7][4] = Piece(PieceType.KING, PieceColor.WHITE)
            b[7][5] = Piece(PieceType.BISHOP, PieceColor.WHITE)
            b[7][6] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
            b[7][7] = Piece(PieceType.ROOK, PieceColor.WHITE)

            return b
        }
    }
}
