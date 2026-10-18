package com.example.godsmino.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF181818))
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gold vertical accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .background(Color(0xFFD4AF37), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = event.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFFD4AF37)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.description,
                        fontSize = 12.sp,
                        color = Color(0xFFCCCCCC)
                    )
                }
            }
        }
    }
}
