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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.Recomendacion
import com.example.frontend.model.TiempoEspera
import com.example.frontend.ui.EstadoRecomendaciones
import com.example.frontend.ui.theme.AppColors

// BQ3 en el feed: "Picks near you". Usa el GPS del celular (sensor + contexto), solo muestra lugares
// que el estudiante no ha probado (smart) y responde una BQ tipo 2. Sin sesion invita a entrar.
@Composable
fun SeccionCercanos(
    conSesion: Boolean,
    estado: EstadoRecomendaciones,
    tiemposEspera: Map<String, TiempoEspera>,
    alIniciarSesion: () -> Unit,
    alPedirUbicacion: () -> Unit,
    alAbrirSitio: (spotId: String) -> Unit,
    alReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("📍 Picks near you", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso)
                Text(
                    when {
                        !conSesion -> "Top-rated places nearby that you haven't tried yet"
                        estado.conGps -> "Top-rated, within a 15 min walk, not tried by you"
                        else -> "Using campus location · turn on location for picks around you"
                    },
                    fontSize = 12.sp,
                    color = AppColors.muted,
                )
            }
            if (conSesion && !estado.conGps) {
                Text(
                    "Use location",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.tomato,
                    modifier = Modifier.clickable(onClick = alPedirUbicacion).padding(start = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        when {
            !conSesion -> TarjetaAccion("Sign in so we know which places you've already tried.", "Sign in", alIniciarSesion)
            estado.cargando && estado.lista.isEmpty() -> LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = AppColors.tomato,
                trackColor = AppColors.tomatoLight,
            )
            estado.error != null && estado.lista.isEmpty() -> TarjetaAccion(estado.error, "Retry", alReintentar)
            estado.cargadoUnaVez && estado.lista.isEmpty() ->
                Text("You've tried every top place within 15 min. Nice!", fontSize = 12.sp, color = AppColors.muted)
            else -> {
                if (estado.aviso != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(13.dp))
                        Text("  ${estado.aviso}", fontSize = 11.sp, color = AppColors.muted)
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(estado.lista, key = Recomendacion::id) { pick ->
                        TarjetaCercano(pick, tiemposEspera[pick.id], onClick = { alAbrirSitio(pick.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaCercano(pick: Recomendacion, tiempo: TiempoEspera?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pick.emoji, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Star, contentDescription = null, tint = AppColors.tomato, modifier = Modifier.size(13.dp))
            Text(" ${pick.rating}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.tomato)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            pick.nombre,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.espresso,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.MyLocation, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(11.dp))
            Text("  ${pick.minutosCaminando} min walk · ${pick.metros} m", fontSize = 11.sp, color = AppColors.muted)
        }
        EtiquetaEspera(tiempo = tiempo, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun TarjetaAccion(texto: String, accion: String, alTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.tomatoLight, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(texto, fontSize = 12.sp, color = AppColors.espresso, modifier = Modifier.weight(1f))
        Text(
            accion,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.tomato,
            modifier = Modifier.clickable(onClick = alTocar).padding(start = 12.dp),
        )
    }
}
