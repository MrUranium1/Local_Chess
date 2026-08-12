package com.example.model

data class Position(val row: Int, val col: Int) {
    val algebraic: String
        get() {
            val fileChar = ('a' + col)
            val rankNum = 8 - row
            return "$fileChar$rankNum"
        }

    fun isValid(): Boolean = row in 0..7 && col in 0..7

    companion object {
        fun fromAlgebraic(alg: String): Position? {
            if (alg.length != 2) return null
            val col = alg[0] - 'a'
            val rankNum = alg[1].digitToIntOrNull() ?: return null
            val row = 8 - rankNum
            val pos = Position(row, col)
            return if (pos.isValid()) pos else null
        }
    }
}
