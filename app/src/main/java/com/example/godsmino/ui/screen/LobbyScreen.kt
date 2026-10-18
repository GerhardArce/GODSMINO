package com.example.godsmino.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun LobbyScreen(
    onStartGame: (String, Boolean) -> Unit,
    onOpenStats: () -> Unit
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var roomCodeInput by remember { mutableStateOf("") }
    var generatedRoomCode by remember { mutableStateOf("") }

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
                    text = "GODSMINO",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFD4AF37)
                )

                Text(
                    text = "Domino estrategico con poderes divinos y modo en linea",
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = { onStartGame("room_offline_bot", true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Partida Rapida vs Bot", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = {
                        val code = "GDS" + (100..999).random()
                        generatedRoomCode = code
                        showCreateDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Crear Sala Online", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                OutlinedButton(
                    onClick = { showJoinDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37))
                ) {
                    Text(text = "Unirse a Sala Online", fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = onOpenStats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text(text = "Historial y Estadisticas", fontSize = 15.sp)
                }
            }
        }
    }

    // Dialog: Create Room
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF1C1C1C),
            title = { Text("Sala Online Creada", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Comparte este codigo con tu oponente para jugar en red:", color = Color.LightGray, fontSize = 13.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = generatedRoomCode, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateDialog = false
                        onStartGame(generatedRoomCode, true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text("Entrar a la Sala", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            }
        )
    }

    // Dialog: Join Room
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            containerColor = Color(0xFF1C1C1C),
            title = { Text("Unirse a Sala Online", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Introduce el codigo de la sala:", color = Color.LightGray, fontSize = 13.sp)
                    OutlinedTextField(
                        value = roomCodeInput,
                        onValueChange = { roomCodeInput = it.uppercase() },
                        placeholder = { Text("Ej: GDS123", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color(0xFF555555),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roomCodeInput.isNotBlank()) {
                            val code = roomCodeInput
                            showJoinDialog = false
                            onStartGame(code, false)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text("Conectar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            }
        )
    }
}
