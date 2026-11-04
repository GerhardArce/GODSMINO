package com.example.godsmino.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414).copy(alpha = 0.95f)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "HISTORIAL DE PARTIDAS",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFD4AF37)
                )

                // LoL-style Profile Summary Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${stats.winrate}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        Text(text = "Winrate", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stats.totalMatches.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Partidas", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${stats.wins}V / ${stats.losses}D", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                        Text(text = "V / D", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stats.highScore.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        Text(text = "Récord", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Text(
                    text = "Partidas Recientes",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.Start)
                )

                if (stats.history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no hay partidas registradas. ¡Juega una partida para ver tu historial!",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(stats.history) { match ->
                            LoLMatchCard(match = match)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
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
fun LoLMatchCard(match: GameStatsManager.MatchRecord) {
    var isExpanded by remember { mutableStateOf(false) }

    val accentColor = if (match.isWin) Color(0xFF2E7D32) else Color(0xFFB71C1C)
    val resultTitle = if (match.isWin) "VICTORIA" else "DERROTA"
    val resultColor = if (match.isWin) Color(0xFF81C784) else Color(0xFFE57373)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1B)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Collapsed Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(36.dp)
                            .background(accentColor, RoundedCornerShape(3.dp))
                    ) {
                    }
                    Column {
                        Text(
                            text = resultTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = resultColor
                        )
                        Text(
                            text = match.getFormattedTime(),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "vs ${match.opponentName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "+${match.score} pts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expandir",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded Details Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141414), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 1.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Rondas jugadas:", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "${match.rounds}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Puntaje final:", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "${match.score} puntos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Oponente:", fontSize = 12.sp, color = Color.Gray)
                        Text(text = match.opponentName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
