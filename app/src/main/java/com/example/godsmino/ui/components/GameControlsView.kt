package com.example.godsmino.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GameControlsView(
    boneyardSize: Int,
    onDrawTile: () -> Unit,
    onPlayLeft: () -> Unit,
    onPlayRight: () -> Unit,
    onRestart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onDrawTile) {
                Text(text = "Robar del Pozo ($boneyardSize)")
            }
            OutlinedButton(onClick = onRestart) {
                Text(text = "Reiniciar")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onPlayLeft) {
                Text(text = "Jugar Izquierda")
            }
            Button(onClick = onPlayRight) {
                Text(text = "Jugar Derecha")
            }
        }
    }
}
