package com.example.engine

import com.example.model.*
import kotlin.random.Random

object ChessBot {

    fun selectMove(state: ChessBoardState, botLevel: BotLevel): Move? {
        val legalMoves = ChessEngine.getAllLegalMoves(state, state.activeColor)
        if (legalMoves.isEmpty()) return null

        return when (botLevel) {
            BotLevel.EASY -> {
                // Random move with slight preference for captures
                val captures = legalMoves.filter { it.capturedPiece != null }
                if (captures.isNotEmpty() && Random.nextFloat() < 0.6f) {
                    captures.random()
                } else {
                    legalMoves.random()
                }
            }
            BotLevel.MEDIUM -> {
                // Minimax depth 1 with position evaluation
                getBestMoveShallow(state, legalMoves)
            }
            BotLevel.HARD -> {
                // Minimax depth 2 with material + position square tables
                getBestMoveMinimax(state, legalMoves, depth = 2)
            }
        }
    }

    private fun getBestMoveShallow(state: ChessBoardState, moves: List<Move>): Move {
        val botColor = state.activeColor
        var bestMove = moves.random()
        var bestScore = Int.MIN_VALUE

        for (move in moves.shuffled()) {
            val simBoard = ChessEngine.simulateMove(state.board, move)
            val score = evaluateBoard(simBoard, botColor)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun getBestMoveMinimax(state: ChessBoardState, moves: List<Move>, depth: Int): Move {
        val botColor = state.activeColor
        var bestMove = moves.random()
        var bestScore = Int.MIN_VALUE

        for (move in moves.shuffled()) {
            val simBoard = ChessEngine.simulateMove(state.board, move)
            val nextState = state.copy(board = simBoard, activeColor = botColor.opposite())
            val score = minimax(nextState, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, false, botColor)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimax(
        state: ChessBoardState,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        botColor: PieceColor
    ): Int {
        if (depth == 0) {
            return evaluateBoard(state.board, botColor)
        }

        var alphaVar = alpha
        var betaVar = beta

        val currentColor = state.activeColor
        val moves = ChessEngine.getAllLegalMoves(state, currentColor)

        if (moves.isEmpty()) {
            if (ChessEngine.isInCheck(state.board, currentColor)) {
                return if (isMaximizing) -10000 + depth else 10000 - depth
            }
            return 0 // Stalemate
        }

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in moves) {
                val simBoard = ChessEngine.simulateMove(state.board, move)
                val nextState = state.copy(board = simBoard, activeColor = currentColor.opposite())
                val eval = minimax(nextState, depth - 1, alphaVar, betaVar, false, botColor)
                maxEval = maxOf(maxEval, eval)
                alphaVar = maxOf(alphaVar, eval)
                if (betaVar <= alphaVar) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in moves) {
                val simBoard = ChessEngine.simulateMove(state.board, move)
                val nextState = state.copy(board = simBoard, activeColor = currentColor.opposite())
                val eval = minimax(nextState, depth - 1, alphaVar, betaVar, true, botColor)
                minEval = minOf(minEval, eval)
                betaVar = minOf(betaVar, eval)
                if (betaVar <= alphaVar) break
            }
            return minEval
        }
    }

    private fun evaluateBoard(board: Array<Array<Piece?>>, botColor: PieceColor): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c] ?: continue
                val valPiece = when (p.type) {
                    PieceType.PAWN -> 100 + pawnSquareTable[r][c]
                    PieceType.KNIGHT -> 320 + knightSquareTable[r][c]
                    PieceType.BISHOP -> 330
                    PieceType.ROOK -> 500
                    PieceType.QUEEN -> 900
                    PieceType.KING -> 20000
                }

                if (p.color == botColor) {
                    score += valPiece
                } else {
                    score -= valPiece
                }
            }
        }
        return score
    }

    private val pawnSquareTable = arrayOf(
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(5,  5, 10, 25, 25, 10,  5,  5),
        intArrayOf(0,  0,  0, 20, 20,  0,  0,  0),
        intArrayOf(5, -5,-10,  0,  0,-10, -5,  5),
        intArrayOf(5, 10, 10,-20,-20, 10, 10,  5),
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0)
    )

    private val knightSquareTable = arrayOf(
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50),
        intArrayOf(-40,-20,  0,  0,  0,  0,-20,-40),
        intArrayOf(-30,  0, 10, 15, 15, 10,  0,-30),
        intArrayOf(-30,  5, 15, 20, 20, 15,  5,-30),
        intArrayOf(-30,  0, 15, 20, 20, 15,  0,-30),
        intArrayOf(-30,  5, 10, 15, 15, 10,  5,-30),
        intArrayOf(-40,-20,  0,  5,  5,  0,-20,-40),
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50)
    )
}
