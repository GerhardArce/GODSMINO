package com.example.godsmino.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.godsmino.data.model.PowerType
import com.example.godsmino.util.SoundManager

@Composable
fun PowerControlsVerticalView(
    powers: Map<PowerType, Int>,
    selectedPower: PowerType?,
    onPowerSelected: (PowerType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PowerType.entries.forEach { power ->
            val count = powers[power] ?: 0
            val isSelected = selectedPower == power

            Button(
                onClick = {
                    SoundManager.playPowerSound()
                    onPowerSelected(power)
                },
                enabled = count > 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFFFFD700) else Color(0xFF37474F),
                    disabledContainerColor = Color(0xFF212121)
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    text = "${power.name.take(3)}\n($count)",
                    fontSize = 10.sp,
                    color = if (isSelected) Color.Black else Color.White,
                    maxLines = 2
                )
            }
        }
    }
}
