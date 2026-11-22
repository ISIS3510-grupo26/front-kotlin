package com.example.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil3.compose.AsyncImage

// Foto de un restaurante o de un plato. Coil la busca primero en su cache de memoria, despues en la
// de disco y solo si no esta la descarga. Debajo queda el emoji: se ve mientras carga, si la foto
// no existe o si no hay internet y nunca se descargo.
@Composable
fun FotoComida(
    url: String?,
    emoji: String,
    fondo: Color,
    modifier: Modifier = Modifier,
    esquinas: Dp = 16.dp,
    tamanoEmoji: TextUnit = 28.sp,
    descripcion: String? = null,
) {
    val forma = RoundedCornerShape(esquinas)
    Box(
        modifier = modifier.clip(forma).background(fondo, forma),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = tamanoEmoji)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = descripcion,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
