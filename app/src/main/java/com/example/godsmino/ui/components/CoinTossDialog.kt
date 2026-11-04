package com.example.godsmino.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.godsmino.data.model.Player
import kotlinx.coroutines.delay

@Composable
fun CoinTossDialog(
    onTossFinished: (String) -> Unit,
    players: List<Player>
) {
    var isTossing by remember { mutableStateOf(true) }
    var resultText by remember { mutableStateOf("🪙 ¡Lanzando moneda al aire para elegir quién inicia! 🪙") }
    var winningPlayerName by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "coin_toss")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coin_angle"
    )

    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coin_scale"
    )

    LaunchedEffect(Unit) {
        delay(3200)
        isTossing = false
        val winner = players.randomOrNull() ?: players.firstOrNull()
        winningPlayerName = winner?.name?.ifBlank { "Jugador 1" } ?: "Jugador 1"
        resultText = "✨ ¡Cara! $winningPlayerName ganó el volado e inicia la partida. ✨"
        delay(2000)
        onTossFinished(winner?.id ?: "p1")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .shadow(28.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "VOLADO DIVINO",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFFFFD700),
                    letterSpacing = 2.sp
                )

                Text(
                    text = resultText,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(if (isTossing) scaleAnim else 1.2f)
                        .rotate(if (isTossing) angle else 0f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFDAA520), Color(0xFF8B6508))
                            ),
                            shape = CircleShape
                        )
                        .border(5.dp, Color(0xFFFFF8DC), CircleShape)
                        .shadow(16.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTossing) "🪙" else "👑",
                        fontSize = 48.sp
                    )
                }

                if (isTossing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFD700),
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 3.dp
                        )
                        Text(text = "Girando moneda...", fontSize = 14.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(text = "¡Iniciando con $winningPlayerName!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
