package com.example.frontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.AbiertoCerca
import com.example.frontend.model.ReglasAbiertosCerca
import com.example.frontend.model.textoCierraEn
import com.example.frontend.ui.EstadoAbiertosCerca
import com.example.frontend.ui.components.FotoComida
import com.example.frontend.ui.theme.AppColors
import kotlinx.coroutines.delay

// Sprint 4 (vista nueva de Lucas). BQ5 (tipo 2): "Which restaurants in the user's favorite list are open now
// and within a 15 minute walk?". Lista los favoritos abiertos, cuanto falta para que cierren y a cuantos
// minutos caminando estan. Se abre desde Saved, igual que el filtro "Open • ≤15 min" de la app Flutter.
@Composable
fun PantallaAbiertosCerca(
    conSesion: Boolean,
    estado: EstadoAbiertosCerca,
    pendientesPorSincronizar: Int,
    hayConexion: Boolean,
    fotos: Map<String, String>,
    alCargar: (forzar: Boolean) -> Unit,
    alAbrirSitio: (String) -> Unit,
    alQuitarFavorito: (String) -> Unit,
    alIniciarSesion: () -> Unit,
    alPedirUbicacion: () -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Al entrar se carga (el repositorio decide si sale de memoria, de la red o de Room).
    LaunchedEffect(conSesion) { if (conSesion) alCargar(false) }

    // "closes in X min" se recalcula cada minuto sin volver a pedir nada; si alguno ya cerro, sale de la lista.
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            ahora = System.currentTimeMillis()
        }
    }
    val abiertos = estado.lista.filter { it.cierraEnMillis > ahora }

    Scaffold(modifier = modifier, containerColor = AppColors.cream) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = alVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.espresso)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Open near you", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso)
                    Text(
                        "Saved places open now · ≤ ${ReglasAbiertosCerca.MAX_MINUTOS_CAMINANDO} min walk",
                        fontSize = 12.sp,
                        color = AppColors.muted,
                    )
                }
                if (conSesion) {
                    IconButton(onClick = { alCargar(true) }, enabled = !estado.cargando) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = AppColors.espresso)
                    }
                }
            }

            if (!conSesion) {
                Mensaje(
                    titulo = "Sign in to see which saved places are open",
                    texto = "Your saved places live in your account, so we can check their opening hours for you.",
                    accion = "Sign in",
                    alTocar = alIniciarSesion,
                )
                return@Column
            }

            if (estado.cargando && abiertos.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = AppColors.tomato,
                    trackColor = AppColors.tomatoLight,
                )
            }
            Avisos(estado, pendientesPorSincronizar, hayConexion, alPedirUbicacion)

            when {
                estado.cargando && abiertos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.tomato)
                }
                estado.error != null && abiertos.isEmpty() -> Mensaje(
                    titulo = if (hayConexion) "Something went wrong" else "No opening hours on this phone yet",
                    texto = estado.error,
                    accion = "Retry",
                    alTocar = { alCargar(true) },
                )
                estado.cargadoUnaVez && abiertos.isEmpty() -> Mensaje(
                    titulo = "Nothing open close by",
                    texto = "None of your saved places are open within a ${ReglasAbiertosCerca.MAX_MINUTOS_CAMINANDO}-minute walk right now.",
                    accion = null,
                    alTocar = {},
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(abiertos, key = AbiertoCerca::spotId) { abierto ->
                        TarjetaAbierto(
                            abierto = abierto,
                            foto = fotos[abierto.spotId],
                            ahora = ahora,
                            alAbrir = { alAbrirSitio(abierto.spotId) },
                            alQuitar = { alQuitarFavorito(abierto.spotId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Avisos(estado: EstadoAbiertosCerca, pendientes: Int, hayConexion: Boolean, alPedirUbicacion: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        estado.aviso?.let { Linea(Icons.Filled.CloudOff, it, AppColors.muted) }
        if (pendientes > 0) {
            val texto = if (pendientes == 1) "1 change to your saved places" else "$pendientes changes to your saved places"
            Linea(
                Icons.Filled.Sync,
                if (hayConexion) "$texto is syncing…" else "$texto will sync when you're back online",
                AppColors.muted,
            )
        }
        if (estado.cargadoUnaVez && !estado.conGps) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(13.dp))
                Text("  Using the campus location. ", fontSize = 11.sp, color = AppColors.muted)
                Text(
                    "Use mine",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.tomato,
                    modifier = Modifier.clickable(onClick = alPedirUbicacion),
                )
            }
        }
    }
}

@Composable
private fun Linea(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, color: Color) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 4.dp)) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(13.dp).padding(top = 1.dp))
        Text("  $texto", fontSize = 11.sp, color = color, lineHeight = 14.sp)
    }
}

@Composable
private fun TarjetaAbierto(abierto: AbiertoCerca, foto: String?, ahora: Long, alAbrir: () -> Unit, alQuitar: () -> Unit) {
    val cierraPronto = abierto.cierraEnMillis - ahora < 30 * 60_000L
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
            .clickable(onClick = alAbrir)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FotoComida(
            url = foto,
            emoji = abierto.emoji,
            fondo = AppColors.tomatoLight,
            esquinas = 14.dp,
            descripcion = abierto.nombre,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                abierto.nombre,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.espresso,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(12.dp))
                Text("  ${abierto.minutosCaminando} min walk · ${abierto.metros} m", fontSize = 12.sp, color = AppColors.muted)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .background(if (cierraPronto) AppColors.tomatoLight else AppColors.mintLight, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = if (cierraPronto) AppColors.tomato else AppColors.mint
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                Text(
                    " Until ${abierto.cierraA} · ${textoCierraEn(abierto.cierraEnMillis, ahora)}",
                    maxLines = 1,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
        }
        IconButton(onClick = alQuitar) {
            Icon(Icons.Filled.Favorite, contentDescription = "Remove from saved", tint = AppColors.tomato)
        }
    }
}

@Composable
private fun Mensaje(titulo: String, texto: String, accion: String?, alTocar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🕒", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(texto, fontSize = 13.sp, color = AppColors.muted, textAlign = TextAlign.Center)
        if (accion != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                accion,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
                    .background(AppColors.tomato, RoundedCornerShape(50))
                    .clickable(onClick = alTocar)
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            )
        }
    }
}
