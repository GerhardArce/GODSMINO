package com.example.godsmino.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.godsmino.data.model.PowerType

@Composable
fun PowerControlsView(
    powers: Map<PowerType, Int>,
    selectedPower: PowerType?,
    onPowerSelected: (PowerType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PowerType.entries.forEach { power ->
            val count = powers[power] ?: 0
            val isSelected = selectedPower == power

            Button(
                onClick = { onPowerSelected(power) },
                enabled = count > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = "${power.name} ($count)",
                    fontSize = 12.sp
                )
            }
        }
    }
}
