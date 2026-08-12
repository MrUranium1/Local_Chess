package com.example.network

import com.example.model.PieceType
import com.example.model.Position
import org.json.JSONObject

enum class MessageType {
    JOIN,           // Client sends name & password
    START_GAME,     // Host sends initial game config (assigned color, timer)
    AUTH_REJECT,    // Host rejects client due to wrong/missing password
    MOVE,           // Player sends move
    DRAW_OFFER,     // Player offers draw
    DRAW_RESPONSE,  // Accepted or declined
    RESIGN,         // Player resigns
    CHAT,           // Player sends quick chat message
    REMATCH_REQUEST,
    REMATCH_RESPONSE,
    PING,
    PONG
}

data class NetworkMessage(
    val type: MessageType,
    val senderName: String = "",
    val password: String = "",
    val rejectReason: String = "",
    val fromRow: Int = -1,
    val fromCol: Int = -1,
    val toRow: Int = -1,
    val toCol: Int = -1,
    val promotion: PieceType? = null,
    val san: String = "",
    val chatText: String = "",
    val assignedColor: String = "WHITE", // "WHITE" or "BLACK"
    val timeMinutes: Int = 10,
    val accepted: Boolean = false
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("type", type.name)
        json.put("senderName", senderName)
        json.put("password", password)
        json.put("rejectReason", rejectReason)
        json.put("fromRow", fromRow)
        json.put("fromCol", fromCol)
        json.put("toRow", toRow)
        json.put("toCol", toCol)
        if (promotion != null) json.put("promotion", promotion.name)
        json.put("san", san)
        json.put("chatText", chatText)
        json.put("assignedColor", assignedColor)
        json.put("timeMinutes", timeMinutes)
        json.put("accepted", accepted)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): NetworkMessage? {
            return try {
                val json = JSONObject(jsonStr)
                val type = MessageType.valueOf(json.getString("type"))
                val senderName = json.optString("senderName", "")
                val password = json.optString("password", "")
                val rejectReason = json.optString("rejectReason", "")
                val fromRow = json.optInt("fromRow", -1)
                val fromCol = json.optInt("fromCol", -1)
                val toRow = json.optInt("toRow", -1)
                val toCol = json.optInt("toCol", -1)
                val promoStr = json.optString("promotion", "")
                val promotion = if (promoStr.isNotEmpty()) PieceType.valueOf(promoStr) else null
                val san = json.optString("san", "")
                val chatText = json.optString("chatText", "")
                val assignedColor = json.optString("assignedColor", "WHITE")
                val timeMinutes = json.optInt("timeMinutes", 10)
                val accepted = json.optBoolean("accepted", false)

                NetworkMessage(
                    type = type,
                    senderName = senderName,
                    password = password,
                    rejectReason = rejectReason,
                    fromRow = fromRow,
                    fromCol = fromCol,
                    toRow = toRow,
                    toCol = toCol,
                    promotion = promotion,
                    san = san,
                    chatText = chatText,
                    assignedColor = assignedColor,
                    timeMinutes = timeMinutes,
                    accepted = accepted
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
