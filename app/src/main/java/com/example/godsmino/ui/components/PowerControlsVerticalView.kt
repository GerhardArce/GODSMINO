package com.example.godsmino.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    containerColor = if (isSelected) Color(0xFFD4AF37) else Color(0xFF2C2C2C),
                    disabledContainerColor = Color(0xFF161616)
                ),
                contentPadding = PaddingValues(6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = power.displayName.split(" ").first(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "($count)",
                        fontSize = 10.sp,
                        color = if (isSelected) Color.DarkGray else Color(0xFFD4AF37)
                    )
                }
            }
        }
    }
}
