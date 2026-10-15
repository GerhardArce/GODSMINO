package com.example.godsmino.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.godsmino.data.model.RandomEvent

@Composable
fun RandomEventBanner(
    event: RandomEvent?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = event != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxWidth()
    ) {
        if (event != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFB300), Color(0xFFFF8F00), Color(0xFFE65100))
                            )
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = event.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = event.description,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
