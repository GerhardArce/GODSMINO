package com.example.godsmino.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.godsmino.util.GameStatsManager

@Composable
fun StatisticsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val stats = GameStatsManager.getStats(context)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414).copy(alpha = 0.95f)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ESTADISTICAS",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFD4AF37)
                )

                Spacer(modifier = Modifier.height(8.dp))

                StatRow(label = "Partidas Jugadas", value = stats.totalMatches.toString())
                StatRow(label = "Victorias", value = stats.wins.toString())
                StatRow(label = "Derrotas", value = stats.losses.toString())
                StatRow(label = "Puntaje Maximo", value = stats.highScore.toString())

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Volver al Lobby", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(10.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = Color(0xFFD4AF37), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
