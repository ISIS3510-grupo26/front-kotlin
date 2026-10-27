package com.example.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.TiempoEspera
import com.example.frontend.model.textoHaceCuanto
import com.example.frontend.ui.theme.AppColors

private val opcionesMinutos = listOf(5, 10, 15, 20, 30)

// BQ4 en la pagina del restaurante: muestra el ultimo tiempo de espera reportado y deja reportar
// el propio. Funciona sin conexion: el reporte queda guardado y se envia despues.
@Composable
fun TarjetaEspera(
    spotId: String,
    tiempo: TiempoEspera?,
    hayConexion: Boolean,
    alReportar: (minutos: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var reportado by rememberSaveable(spotId) { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(AppColors.tomatoLight, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = AppColors.tomato)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Wait time", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso)
                Text(textoUltimoReporte(tiempo), fontSize = 12.sp, color = AppColors.muted, lineHeight = 16.sp)
            }
            val minutos = tiempo?.minutos
            if (minutos != null) {
                Text(
                    "$minutos min",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (tiempo.esReciente()) AppColors.tomato else AppColors.muted,
                )
            }
        }

        if (tiempo?.pendienteDeEnvio == true) {
            Spacer(Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(14.dp))
                Text(
                    if (hayConexion) "  Sending your report..." else "  Saved on your phone · will sync when you're online",
                    fontSize = 11.sp,
                    color = AppColors.muted,
                )
            }
        }

        Spacer(Modifier.size(12.dp))
        Text(
            if (reportado == null) "How long is the line right now?" else "Thanks! Other students will see your report.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.espresso,
        )
        Spacer(Modifier.size(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(opcionesMinutos) { minutos ->
                ChipPildora(
                    text = if (minutos == opcionesMinutos.last()) "$minutos+ min" else "$minutos min",
                    selected = reportado == minutos,
                    onClick = {
                        reportado = minutos
                        alReportar(minutos)
                    },
                )
            }
        }
    }
}

private fun textoUltimoReporte(tiempo: TiempoEspera?): String {
    val hace = tiempo?.minutosAtras() ?: return "No reports yet. Be the first to tell others."
    val base = "Last reported ${textoHaceCuanto(hace)}"
    return when {
        !tiempo.esReciente() -> "$base · may be outdated"
        tiempo.reportesRecientes > 1 -> "$base · ${tiempo.reportesRecientes} reports in the last 2 h"
        else -> base
    }
}

// Version corta para las tarjetas del feed: solo si el dato es reciente, si no confunde.
@Composable
fun EtiquetaEspera(tiempo: TiempoEspera?, modifier: Modifier = Modifier) {
    val minutos = tiempo?.minutos ?: return
    val hace = tiempo.minutosAtras() ?: return
    if (!tiempo.esReciente()) return
    Row(
        modifier = modifier
            .background(AppColors.mintLight, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Schedule, contentDescription = null, tint = AppColors.mint, modifier = Modifier.size(12.dp))
        Text(
            "  ~$minutos min wait · ${textoHaceCuanto(hace)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D5B),
        )
    }
}
