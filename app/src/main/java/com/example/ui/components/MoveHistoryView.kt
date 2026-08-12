package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Move
import com.example.ui.theme.*

@Composable
fun MoveHistoryView(
    moves: List<Move>,
    modifier: Modifier = Modifier
) {
    // Group moves into pairs (1. White SAN, Black SAN)
    val movePairs = mutableListOf<Pair<Int, Pair<String, String?>>>()
    var index = 0
    var moveNumber = 1

    while (index < moves.size) {
        val whiteSan = moves[index].san
        val blackSan = if (index + 1 < moves.size) moves[index + 1].san else null
        movePairs.add(Pair(moveNumber, Pair(whiteSan, blackSan)))
        index += 2
        moveNumber++
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SlateDarkCard)
            .border(1.dp, GeoBorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (movePairs.isEmpty()) {
            Text(
                text = "Move history will appear here",
                fontSize = 11.sp,
                color = GeoTextSecondary,
                modifier = Modifier.padding(4.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(movePairs) { idx, (num, pair) ->
                    val (wSan, bSan) = pair
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (idx == movePairs.lastIndex) AmberAccent else SlateDarkSurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$num.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (idx == movePairs.lastIndex) Color(0xFF121410) else GoldPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = wSan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (idx == movePairs.lastIndex) Color(0xFF121410) else GeoTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        if (bSan != null) {
                            Text(
                                text = bSan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (idx == movePairs.lastIndex) Color(0xFF121410) else GeoTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
