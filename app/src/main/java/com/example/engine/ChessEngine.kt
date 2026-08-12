package com.example.engine

import com.example.model.*
import kotlin.math.abs

object ChessEngine {

    fun getInitialState(timeControl: TimeControl = TimeControl(10, 0)): ChessBoardState {
        val initialBoard = ChessBoardState.createInitialBoard()
        val timeMs = if (timeControl.initialMinutes == 0) 0L else timeControl.initialMinutes * 60 * 1000L
        return ChessBoardState(
            board = initialBoard,
            activeColor = PieceColor.WHITE,
            status = GameStatus.WHITE_TURN,
            whiteTimeMs = timeMs,
            blackTimeMs = timeMs,
            timeControl = timeControl
        )
    }

    fun findKingPosition(board: Array<Array<Piece?>>, color: PieceColor): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    return Position(r, c)
                }
            }
        }
        return null
    }

    fun isSquareAttacked(board: Array<Array<Piece?>>, pos: Position, byColor: PieceColor): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == byColor) {
                    if (canPieceAttackSquare(board, Position(r, c), p, pos)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun canPieceAttackSquare(
        board: Array<Array<Piece?>>,
        from: Position,
        piece: Piece,
        target: Position
    ): Boolean {
        val rowDiff = target.row - from.row
        val colDiff = target.col - from.col

        return when (piece.type) {
            PieceType.PAWN -> {
                val forward = if (piece.color == PieceColor.WHITE) -1 else 1
                rowDiff == forward && abs(colDiff) == 1
            }
            PieceType.KNIGHT -> {
                (abs(rowDiff) == 2 && abs(colDiff) == 1) || (abs(rowDiff) == 1 && abs(colDiff) == 2)
            }
            PieceType.BISHOP -> {
                abs(rowDiff) == abs(colDiff) && rowDiff != 0 && isPathClear(board, from, target)
            }
            PieceType.ROOK -> {
                (rowDiff == 0 || colDiff == 0) && (rowDiff != 0 || colDiff != 0) && isPathClear(board, from, target)
            }
            PieceType.QUEEN -> {
                ((abs(rowDiff) == abs(colDiff)) || (rowDiff == 0 || colDiff == 0)) &&
                        (rowDiff != 0 || colDiff != 0) && isPathClear(board, from, target)
            }
            PieceType.KING -> {
                abs(rowDiff) <= 1 && abs(colDiff) <= 1 && (rowDiff != 0 || colDiff != 0)
            }
        }
    }

    private fun isPathClear(board: Array<Array<Piece?>>, from: Position, to: Position): Boolean {
        val rowStep = (to.row - from.row).coerceIn(-1, 1)
        val colStep = (to.col - from.col).coerceIn(-1, 1)

        var currRow = from.row + rowStep
        var currCol = from.col + colStep

        while (currRow != to.row || currCol != to.col) {
            if (board[currRow][currCol] != null) return false
            currRow += rowStep
            currCol += colStep
        }
        return true
    }

    fun isInCheck(board: Array<Array<Piece?>>, color: PieceColor): Boolean {
        val kingPos = findKingPosition(board, color) ?: return false
        return isSquareAttacked(board, kingPos, color.opposite())
    }

    fun getLegalMoves(state: ChessBoardState, from: Position): List<Move> {
        val piece = state.getPiece(from) ?: return emptyList()
        if (piece.color != state.activeColor) return emptyList()

        val pseudoMoves = getPseudoLegalMoves(state, from, piece)
        val legalMoves = mutableListOf<Move>()

        for (move in pseudoMoves) {
            val simulatedBoard = simulateMove(state.board, move)
            if (!isInCheck(simulatedBoard, piece.color)) {
                legalMoves.add(move)
            }
        }

        return legalMoves
    }

    fun getAllLegalMoves(state: ChessBoardState, color: PieceColor): List<Move> {
        val allMoves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c]
                if (p != null && p.color == color) {
                    val pos = Position(r, c)
                    val pseudoMoves = getPseudoLegalMoves(state, pos, p)
                    for (m in pseudoMoves) {
                        val simBoard = simulateMove(state.board, m)
                        if (!isInCheck(simBoard, color)) {
                            allMoves.add(m)
                        }
                    }
                }
            }
        }
        return allMoves
    }

    private fun getPseudoLegalMoves(state: ChessBoardState, from: Position, piece: Piece): List<Move> {
        val moves = mutableListOf<Move>()
        val board = state.board

        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) -1 else 1
                val startRow = if (piece.color == PieceColor.WHITE) 6 else 1

                // 1 step forward
                val f1 = Position(from.row + dir, from.col)
                if (f1.isValid() && board[f1.row][f1.col] == null) {
                    val isPromotion = (f1.row == 0 || f1.row == 7)
                    if (isPromotion) {
                        listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { promo ->
                            moves.add(Move(from, f1, piece, promotion = promo))
                        }
                    } else {
                        moves.add(Move(from, f1, piece))
                    }

                    // 2 steps forward
                    if (from.row == startRow) {
                        val f2 = Position(from.row + 2 * dir, from.col)
                        if (f2.isValid() && board[f2.row][f2.col] == null) {
                            moves.add(Move(from, f2, piece))
                        }
                    }
                }

                // Normal captures
                for (dCol in listOf(-1, 1)) {
                    val capPos = Position(from.row + dir, from.col + dCol)
                    if (capPos.isValid()) {
                        val targetPiece = board[capPos.row][capPos.col]
                        if (targetPiece != null && targetPiece.color != piece.color) {
                            val isPromotion = (capPos.row == 0 || capPos.row == 7)
                            if (isPromotion) {
                                listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { promo ->
                                    moves.add(Move(from, capPos, piece, capturedPiece = targetPiece, promotion = promo))
                                }
                            } else {
                                moves.add(Move(from, capPos, piece, capturedPiece = targetPiece))
                            }
                        } else if (capPos == state.enPassantTarget) {
                            // En Passant capture
                            val epCapturedPiece = board[from.row][from.col + dCol]
                            moves.add(Move(from, capPos, piece, capturedPiece = epCapturedPiece, isEnPassant = true))
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                val offsets = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for ((dr, dc) in offsets) {
                    val target = Position(from.row + dr, from.col + dc)
                    if (target.isValid()) {
                        val targetPiece = board[target.row][target.col]
                        if (targetPiece == null || targetPiece.color != piece.color) {
                            moves.add(Move(from, target, piece, capturedPiece = targetPiece))
                        }
                    }
                }
            }

            PieceType.BISHOP -> addRayMoves(board, from, piece, listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)), moves)
            PieceType.ROOK -> addRayMoves(board, from, piece, listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)), moves)
            PieceType.QUEEN -> addRayMoves(board, from, piece, listOf(
                Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
                Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
            ), moves)

            PieceType.KING -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val target = Position(from.row + dr, from.col + dc)
                        if (target.isValid()) {
                            val targetPiece = board[target.row][target.col]
                            if (targetPiece == null || targetPiece.color != piece.color) {
                                moves.add(Move(from, target, piece, capturedPiece = targetPiece))
                            }
                        }
                    }
                }

                // Castling
                if (!isInCheck(board, piece.color)) {
                    if (piece.color == PieceColor.WHITE && from == Position(7, 4)) {
                        // Kingside White
                        if (state.whiteCanCastleKingside &&
                            board[7][5] == null && board[7][6] == null &&
                            !isSquareAttacked(board, Position(7, 5), PieceColor.BLACK) &&
                            !isSquareAttacked(board, Position(7, 6), PieceColor.BLACK)
                        ) {
                            moves.add(Move(from, Position(7, 6), piece, isCastling = true))
                        }
                        // Queenside White
                        if (state.whiteCanCastleQueenside &&
                            board[7][3] == null && board[7][2] == null && board[7][1] == null &&
                            !isSquareAttacked(board, Position(7, 3), PieceColor.BLACK) &&
                            !isSquareAttacked(board, Position(7, 2), PieceColor.BLACK)
                        ) {
                            moves.add(Move(from, Position(7, 2), piece, isCastling = true))
                        }
                    } else if (piece.color == PieceColor.BLACK && from == Position(0, 4)) {
                        // Kingside Black
                        if (state.blackCanCastleKingside &&
                            board[0][5] == null && board[0][6] == null &&
                            !isSquareAttacked(board, Position(0, 5), PieceColor.WHITE) &&
                            !isSquareAttacked(board, Position(0, 6), PieceColor.WHITE)
                        ) {
                            moves.add(Move(from, Position(0, 6), piece, isCastling = true))
                        }
                        // Queenside Black
                        if (state.blackCanCastleQueenside &&
                            board[0][3] == null && board[0][2] == null && board[0][1] == null &&
                            !isSquareAttacked(board, Position(0, 3), PieceColor.WHITE) &&
                            !isSquareAttacked(board, Position(0, 2), PieceColor.WHITE)
                        ) {
                            moves.add(Move(from, Position(0, 2), piece, isCastling = true))
                        }
                    }
                }
            }
        }

        return moves
    }

    private fun addRayMoves(
        board: Array<Array<Piece?>>,
        from: Position,
        piece: Piece,
        directions: List<Pair<Int, Int>>,
        moves: MutableList<Move>
    ) {
        for ((dr, dc) in directions) {
            var currRow = from.row + dr
            var currCol = from.col + dc
            while (currRow in 0..7 && currCol in 0..7) {
                val target = Position(currRow, currCol)
                val targetPiece = board[currRow][currCol]
                if (targetPiece == null) {
                    moves.add(Move(from, target, piece))
                } else {
                    if (targetPiece.color != piece.color) {
                        moves.add(Move(from, target, piece, capturedPiece = targetPiece))
                    }
                    break
                }
                currRow += dr
                currCol += dc
            }
        }
    }

    fun simulateMove(board: Array<Array<Piece?>>, move: Move): Array<Array<Piece?>> {
        val newBoard = Array(8) { r -> Array(8) { c -> board[r][c] } }

        // Move the main piece
        val pieceToMove = if (move.promotion != null) {
            Piece(move.promotion, move.piece.color)
        } else {
            move.piece
        }

        newBoard[move.from.row][move.from.col] = null
        newBoard[move.to.row][move.to.col] = pieceToMove

        // Handle En Passant extra clearing
        if (move.isEnPassant) {
            val epRow = move.from.row
            val epCol = move.to.col
            newBoard[epRow][epCol] = null
        }

        // Handle Castling rook placement
        if (move.isCastling) {
            if (move.to == Position(7, 6)) { // White Kingside
                newBoard[7][7] = null
                newBoard[7][5] = Piece(PieceType.ROOK, PieceColor.WHITE)
            } else if (move.to == Position(7, 2)) { // White Queenside
                newBoard[7][0] = null
                newBoard[7][3] = Piece(PieceType.ROOK, PieceColor.WHITE)
            } else if (move.to == Position(0, 6)) { // Black Kingside
                newBoard[0][7] = null
                newBoard[0][5] = Piece(PieceType.ROOK, PieceColor.BLACK)
            } else if (move.to == Position(0, 2)) { // Black Queenside
                newBoard[0][0] = null
                newBoard[0][3] = Piece(PieceType.ROOK, PieceColor.BLACK)
            }
        }

        return newBoard
    }

    fun makeMove(state: ChessBoardState, move: Move): ChessBoardState {
        val newBoard = simulateMove(state.board, move)
        val nextColor = state.activeColor.opposite()

        // Update castling rights
        var wK = state.whiteCanCastleKingside
        var wQ = state.whiteCanCastleQueenside
        var bK = state.blackCanCastleKingside
        var bQ = state.blackCanCastleQueenside

        if (move.piece.type == PieceType.KING) {
            if (move.piece.color == PieceColor.WHITE) {
                wK = false; wQ = false
            } else {
                bK = false; bQ = false
            }
        }
        if (move.piece.type == PieceType.ROOK) {
            if (move.from == Position(7, 7)) wK = false
            if (move.from == Position(7, 0)) wQ = false
            if (move.from == Position(0, 7)) bK = false
            if (move.from == Position(0, 0)) bQ = false
        }

        // En Passant target
        var newEpTarget: Position? = null
        if (move.piece.type == PieceType.PAWN && abs(move.to.row - move.from.row) == 2) {
            val midRow = (move.from.row + move.to.row) / 2
            newEpTarget = Position(midRow, move.from.col)
        }

        // Captured pieces list
        val newCapWhite = state.capturedWhitePieces.toMutableList()
        val newCapBlack = state.capturedBlackPieces.toMutableList()

        if (move.capturedPiece != null) {
            if (state.activeColor == PieceColor.WHITE) {
                newCapWhite.add(move.capturedPiece)
            } else {
                newCapBlack.add(move.capturedPiece)
            }
        }

        // Apply time increment
        var wTime = state.whiteTimeMs
        var bTime = state.blackTimeMs
        val incMs = state.timeControl.incrementSeconds * 1000L
        if (state.activeColor == PieceColor.WHITE && wTime > 0) {
            wTime += incMs
        } else if (state.activeColor == PieceColor.BLACK && bTime > 0) {
            bTime += incMs
        }

        // Generate SAN
        val moveWithSan = move.copy(san = generateSan(state, move, newBoard, nextColor))

        // Check for Checkmate / Stalemate / Insufficient Material
        val nextLegalMoves = getAllLegalMoves(state.copy(board = newBoard, activeColor = nextColor), nextColor)
        val inCheck = isInCheck(newBoard, nextColor)
        val kingPos = if (inCheck) findKingPosition(newBoard, nextColor) else null

        val status = when {
            nextLegalMoves.isEmpty() && inCheck -> {
                if (nextColor == PieceColor.WHITE) GameStatus.BLACK_WIN_CHECKMATE else GameStatus.WHITE_WIN_CHECKMATE
            }
            nextLegalMoves.isEmpty() && !inCheck -> GameStatus.DRAW_STALEMATE
            isInsufficientMaterial(newBoard) -> GameStatus.DRAW_INSUFFICIENT_MATERIAL
            else -> if (nextColor == PieceColor.WHITE) GameStatus.WHITE_TURN else GameStatus.BLACK_TURN
        }

        return state.copy(
            board = newBoard,
            activeColor = nextColor,
            status = status,
            moveHistory = state.moveHistory + moveWithSan,
            capturedWhitePieces = newCapWhite,
            capturedBlackPieces = newCapBlack,
            whiteTimeMs = wTime,
            blackTimeMs = bTime,
            isInCheck = inCheck,
            checkedKingPos = kingPos,
            lastMove = moveWithSan,
            whiteCanCastleKingside = wK,
            whiteCanCastleQueenside = wQ,
            blackCanCastleKingside = bK,
            blackCanCastleQueenside = bQ,
            enPassantTarget = newEpTarget,
            fullMoveNumber = if (state.activeColor == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber
        )
    }

    private fun generateSan(state: ChessBoardState, move: Move, nextBoard: Array<Array<Piece?>>, nextColor: PieceColor): String {
        if (move.isCastling) {
            return if (move.to.col == 6) "O-O" else "O-O-O"
        }

        val sb = StringBuilder()
        if (move.piece.type != PieceType.PAWN) {
            sb.append(move.piece.type.symbol)
        }

        // Disambiguation if needed
        val isCapture = move.capturedPiece != null
        if (move.piece.type == PieceType.PAWN && isCapture) {
            sb.append(('a' + move.from.col))
        }

        if (isCapture) {
            sb.append("x")
        }

        sb.append(move.to.algebraic)

        if (move.promotion != null) {
            sb.append("=").append(move.promotion.symbol)
        }

        val inCheck = isInCheck(nextBoard, nextColor)
        val nextMoves = getAllLegalMoves(state.copy(board = nextBoard, activeColor = nextColor), nextColor)
        if (inCheck) {
            if (nextMoves.isEmpty()) {
                sb.append("#")
            } else {
                sb.append("+")
            }
        }

        return sb.toString()
    }

    private fun isInsufficientMaterial(board: Array<Array<Piece?>>): Boolean {
        val pieces = mutableListOf<Piece>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null) pieces.add(p)
            }
        }

        // King vs King
        if (pieces.size == 2) return true

        // King + Bishop vs King OR King + Knight vs King
        if (pieces.size == 3) {
            val nonKing = pieces.firstOrNull { it.type != PieceType.KING }
            if (nonKing != null && (nonKing.type == PieceType.BISHOP || nonKing.type == PieceType.KNIGHT)) {
                return true
            }
        }

        return false
    }
}
