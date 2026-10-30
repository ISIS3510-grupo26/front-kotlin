package com.example.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Schedule
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
import com.example.frontend.model.SitioMejorParaTi
import com.example.frontend.model.textoHaceCuanto
import com.example.frontend.ui.EstadoMejorParaTi
import com.example.frontend.ui.theme.AppColors

// BQ6 en el feed: "Best for you right now". Junta la ultima espera reportada (BQ4) con el perfil de gusto
// que el estudiante guardo en el celular. Reemplaza el porcentaje de Taste Match que venia escrito a mano.
@Composable
fun SeccionMejorParaTi(
    estado: EstadoMejorParaTi,
    alAbrirSitio: (spotId: String) -> Unit,
    alEditarGusto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("✨ Best for you right now", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso)
                Text("Short wait + what matches your taste profile", fontSize = 12.sp, color = AppColors.muted)
            }
            Text(
                "Edit taste",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.tomato,
                modifier = Modifier.clickable(onClick = alEditarGusto).padding(start = 8.dp),
            )
        }
        Spacer(Modifier.height(10.dp))

        when {
            estado.cargando && estado.lista.isEmpty() -> LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = AppColors.tomato,
                trackColor = AppColors.tomatoLight,
            )
            estado.lista.isEmpty() -> Text("No spots to rank yet.", fontSize = 12.sp, color = AppColors.muted)
            else -> {
                if (estado.aviso != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(13.dp))
                        Text("  ${estado.aviso}", fontSize = 11.sp, color = AppColors.muted)
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(estado.lista.take(MAXIMO_EN_FEED), key = { it.candidato.spotId }) { sitio ->
                        TarjetaMejorParaTi(sitio, onClick = { alAbrirSitio(sitio.candidato.spotId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaMejorParaTi(sitio: SitioMejorParaTi, onClick: () -> Unit) {
    val c = sitio.candidato
    Column(
        modifier = Modifier
            .width(158.dp)
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, if (sitio.rank == 1) AppColors.tomato else AppColors.border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.emoji, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            Text(
                "${sitio.compatibilidad}% match",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
                    .background(AppColors.tomato, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            c.nombre,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.espresso,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
        )
        Text(c.cocina, fontSize = 11.sp, color = AppColors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(12.dp))
            Text(
                sitio.esperaConocida?.let { "  ~$it min wait" } ?: "  No recent wait report",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.espresso,
                maxLines = 1,
            )
        }
        // Que tan viejo es el dato: un "3 min" de hace un rato no vale lo mismo que uno de ahora.
        if (sitio.esperaConocida != null) {
            sitio.candidato.minutosAtras?.let {
                Text("reported ${textoHaceCuanto(it)}", fontSize = 10.sp, color = AppColors.muted, maxLines = 1)
            }
        }
    }
}

private const val MAXIMO_EN_FEED = 5
