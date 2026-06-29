package com.pictofly.ui.screens.consent

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pictofly.R
import com.pictofly.ui.theme.PictoFlyTheme
import androidx.compose.ui.unit.sp

@Composable
fun ConsentScreen(
    onAccept: () -> Unit
) {
    PictoFlyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.rocket_welcome),
                        contentDescription = " bienvenida a PictoVoice",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Text(
                    text = "¡Bienvenido a PictoVoice!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 28.sp,  // Tamaño reducido
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Para los tutores:",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 20.sp,  // Tamaño reducido
                                color = MaterialTheme.colorScheme.primary
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        KidFriendlyInfoPoint(
                            emoji = "📞",
                            title = "Es fácil y seguro",
                            description = "La app guarda todo solo en este dispositivo, nada se comparte."
                        )

                        KidFriendlyInfoPoint(
                            emoji = "🔑",
                            title = "Sin cuentas complicadas",
                            description = "No necesita registro, email o contraseñas."
                        )

                        KidFriendlyInfoPoint(
                            emoji = "💾",
                            title = "Recuerda las preferencias",
                            description = "Guarda los ajustes del niño para que siempre estén listos."
                        )

                        KidFriendlyInfoPoint(
                            emoji = "👩‍🏫",
                            title = "Importante saber",
                            description = "Si borras la app, los ajustes guardados desaparecerán."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Text(
                        text = "¡EMPEZAR LA AVENTURA!",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun KidFriendlyInfoPoint(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 24.sp),
            modifier = Modifier
                .width(48.dp)
                .padding(top = 4.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                ),
                lineHeight = 20.sp,
            )
        }
    }
}