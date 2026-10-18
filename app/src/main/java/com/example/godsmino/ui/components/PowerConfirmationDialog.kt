package com.example.godsmino.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.godsmino.data.model.PowerType
import com.example.godsmino.data.model.Tile

@Composable
fun PowerConfirmationDialog(
    powerType: PowerType,
    playerTiles: List<Tile>,
    selectedSwapTile: Tile?,
    onSelectSwapTile: (Tile) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = powerType.icon,
                    contentDescription = powerType.displayName,
                    modifier = Modifier.size(26.dp),
                    tint = Color(0xFFD4AF37)
                )
                Text(
                    text = powerType.displayName,
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = powerType.description,
                    color = Color.LightGray,
                    fontSize = 13.sp
                )

                if (powerType == PowerType.SWAP) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selecciona la ficha de tu mano que deseas entregar al rival:",
                        color = Color(0xFFD4AF37),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(playerTiles) { tile ->
                            val isSelected = selectedSwapTile == tile
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFD4AF37) else Color.Gray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectSwapTile(tile) }
                                    .padding(4.dp)
                            ) {
                                DominoTileView(
                                    tile = tile,
                                    isSelected = isSelected,
                                    isDraggable = false
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Usar Poder", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = Color.LightGray)
            }
        }
    )
}
