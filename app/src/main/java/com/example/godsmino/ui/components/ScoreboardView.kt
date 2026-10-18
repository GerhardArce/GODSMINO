package com.example.godsmino.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.godsmino.data.model.Player

@Composable
fun ScoreboardView(
    players: List<Player>,
    currentTurnPlayerId: String,
    roundNumber: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ronda: $roundNumber",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        players.forEach { player ->
            val isCurrentTurn = player.id == currentTurnPlayerId
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = player.name + if (isCurrentTurn) " (Turno)" else "",
                    fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrentTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Puntos: ${player.score} | Fichas: ${player.tiles.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
