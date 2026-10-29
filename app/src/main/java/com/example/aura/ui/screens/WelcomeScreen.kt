package com.example.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraPrimaryPurple
import com.example.ui.theme.AuraPrimaryPurpleLight
import com.example.ui.theme.AuraTextSecondary

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .padding(24.dp)
            .testTag("welcome_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Screen title "AURA" matching Mockup 01
            Text(
                text = "AURA",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    fontSize = 32.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Glowing circular music note badge
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = CircleShape,
                        spotColor = AuraPrimaryPurple,
                        ambientColor = AuraPrimaryPurple
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AuraPrimaryPurple.copy(alpha = 0.35f),
                                Color(0xFF1B1E38)
                            )
                        )
                    )
                    .border(3.dp, AuraPrimaryPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "AURA Music Logo",
                    tint = AuraPrimaryPurpleLight,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Audio Sin Pérdida & Reproductor Local",
                style = MaterialTheme.typography.bodyMedium,
                color = AuraTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(70.dp))

            // "Iniciar" button - filled pill
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(52.dp)
                    .testTag("welcome_iniciar_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuraPrimaryPurple,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Iniciar",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Registrarse" button - outlined pill
            OutlinedButton(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(52.dp)
                    .testTag("welcome_registrarse_button"),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, AuraPrimaryPurple),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Registrarse",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}
