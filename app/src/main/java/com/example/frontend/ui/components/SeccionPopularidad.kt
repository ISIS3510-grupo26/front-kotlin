package com.example.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.ActividadSitio
import com.example.frontend.ui.EstadoPopularidad
import com.example.frontend.ui.theme.AppColors

// Seccion "Popular right now" del feed: muestra al usuario la respuesta de la BQ3 para la hora
// actual del celular. Si no hay datos (sin red o sin telemetria) no ocupa espacio.
@Composable
fun SeccionPopularidad(
    estado: EstadoPopularidad,
    alAbrirSitio: (spotId: String) -> Unit,
    alVerTodo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ranking = estado.ahora
    if (ranking == null) {
        if (estado.cargando) {
            LinearProgressIndicator(
                modifier = modifier.fillMaxWidth().height(2.dp),
                color = AppColors.tomato,
                trackColor = AppColors.tomatoLight,
            )
        }
        return
    }
    if (ranking.sitios.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = alVerTodo),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "🔥 Popular right now · ${ranking.etiquetaHora}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.espresso,
                )
                Text(
                    "${ranking.totalPageViews} views · ${ranking.totalSearches} searches at this hour this week",
                    fontSize = 12.sp,
                    color = AppColors.muted,
                )
            }
            Text("By hour", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.tomato)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppColors.tomato, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ranking.sitios, key = ActividadSitio::spotId) { sitio ->
                TarjetaPopular(sitio = sitio, onClick = { alAbrirSitio(sitio.spotId) })
            }
        }
    }
}

@Composable
private fun TarjetaPopular(sitio: ActividadSitio, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InsigniaRanking(rank = sitio.rank)
            Spacer(Modifier.weight(1f))
            Text(sitio.emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            sitio.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.espresso,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "👀 ${sitio.pageViews} · 🔎 ${sitio.searches}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.muted,
        )
    }
}

@Composable
fun InsigniaRanking(rank: Int, size: Int = 26) {
    val (fondo, texto) = when (rank) {
        1 -> AppColors.tomato to Color.White
        2 -> AppColors.espresso to Color.White
        else -> AppColors.tomatoLight to AppColors.tomato
    }
    Box(
        modifier = Modifier.size(size.dp).background(fondo, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("#$rank", fontSize = (size * 0.42).sp, fontWeight = FontWeight.ExtraBold, color = texto)
    }
}
