package com.example.frontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.data.local.Borrador
import com.example.frontend.ui.theme.AppColors
import kotlinx.coroutines.delay

// Conectividad eventual: escribir y publicar una reseña nunca necesita red. Lo que se escribe se guarda
// como borrador en un archivo y "Post Review" la deja en la cola de Room hasta que haya conexion y sesion.
@Composable
fun PantallaEscribirResena(
    placeName: String,
    alPublicar: (estrellas: Int, texto: String) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
    hayConexion: Boolean = true,
    conSesion: Boolean = true,
    cargarBorrador: suspend () -> Borrador? = { null },
    guardarBorrador: suspend (estrellas: Int, texto: String) -> Unit = { _, _ -> },
) {
    var rating by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    // Hasta no leer el archivo no se guarda nada: si no, el texto vacio inicial pisaria el borrador.
    var borradorLeido by remember { mutableStateOf(false) }
    var borradorGuardado by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cargarBorrador()?.let {
            rating = it.estrellas
            reviewText = it.texto
            borradorGuardado = true
        }
        borradorLeido = true
    }
    // Se guarda medio segundo despues de la ultima tecla: no se escribe el archivo con cada letra.
    LaunchedEffect(rating, reviewText, borradorLeido) {
        if (!borradorLeido) return@LaunchedEffect
        borradorGuardado = false
        delay(500)
        guardarBorrador(rating, reviewText)
        borradorGuardado = rating > 0 || reviewText.isNotBlank()
    }

    Scaffold(
        modifier = modifier,
        containerColor = AppColors.cream,
        bottomBar = {
            Button(
                onClick = { alPublicar(rating, reviewText.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.tomato),
                shape = RoundedCornerShape(28.dp),
                enabled = rating > 0 && reviewText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(52.dp),
            ) {
                Text(if (hayConexion && conSesion) "Post Review" else "Save & post later", fontWeight = FontWeight.SemiBold)
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = alVolver) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.espresso,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text("Write a Review", fontWeight = FontWeight.Bold, color = AppColors.espresso)
            }

            when {
                !hayConexion -> AvisoResena("You're offline. Your review will be saved on this phone and posted when you reconnect.")
                !conSesion -> AvisoResena("Sign in to post it. Your review stays saved on this phone until then.")
            }

            Spacer(Modifier.height(20.dp))
            Text(placeName, fontWeight = FontWeight.SemiBold, color = AppColors.espresso)
            Text(
                "How was your experience?",
                color = AppColors.muted,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(16.dp))
            Row {
                repeat(5) { index ->
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "${index + 1} stars",
                        tint = if (index < rating) AppColors.tomato else AppColors.border,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(end = 4.dp)
                            .clickable { rating = index + 1 },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                // El backend acepta hasta 500 caracteres; cortar aqui evita que la reseña se rechace despues.
                value = reviewText,
                onValueChange = { reviewText = it.take(MAX_CARACTERES) },
                placeholder = { Text("Tell other students what to expect — taste, price, wait time...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.tomato,
                    unfocusedBorderColor = AppColors.border,
                ),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    if (borradorGuardado) "Draft saved on this phone" else "",
                    fontSize = 11.sp,
                    color = AppColors.muted,
                    modifier = Modifier.weight(1f),
                )
                Text("${reviewText.length}/$MAX_CARACTERES", fontSize = 11.sp, color = AppColors.muted)
            }
        }
    }
}

@Composable
private fun AvisoResena(texto: String) {
    Row(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .background(AppColors.tomatoLight, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = AppColors.tomato, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.tomato)
    }
}

private const val MAX_CARACTERES = 500
