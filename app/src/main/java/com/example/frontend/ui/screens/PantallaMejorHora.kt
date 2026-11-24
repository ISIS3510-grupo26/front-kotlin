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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.EsperaHora
import com.example.frontend.model.MejorHoraSitio
import com.example.frontend.model.ReglasMejorHora
import com.example.frontend.model.Spot
import com.example.frontend.ui.EstadoMejorHora
import com.example.frontend.ui.components.ChipPildora
import com.example.frontend.ui.components.FotoComida
import com.example.frontend.ui.theme.AppColors

// Sprint 4 (vista nueva de Tomas). BQ8 (tipo 2): "At what time of the day is the reported waiting time at each
// restaurant the shortest?". Muestra la hora mas tranquila del campus y, por restaurante, a que hora ir,
// a que hora no, y la proxima hora tranquila de hoy. Se abre desde el detalle de un restaurante
// ("When is it calmer?") o desde Profile. Usa los mismos reportes de espera de la BQ4.
@Composable
fun PantallaMejorHora(
    estado: EstadoMejorHora,
    catalogo: List<Spot>,
    spotInicial: String?,
    alCargar: (ventanaDias: Int, forzar: Boolean) -> Unit,
    alAbrirSitio: (String) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Al entrar se carga la ventana elegida (el repositorio decide si sale de memoria, de la red o de Room).
    LaunchedEffect(Unit) { alCargar(estado.ventanaDias, false) }

    val fotos = remember(catalogo) { catalogo.mapNotNull { s -> s.imageUrl?.let { s.id to it } }.toMap() }
    val resumen = estado.resumen
    // Los restaurantes del catalogo que todavia no tienen reportes en la ventana tambien se listan.
    val sinReportes = remember(catalogo, resumen) {
        val conDatos = resumen?.sitios?.map { it.spotId }?.toSet().orEmpty()
        catalogo.filter { it.id !in conDatos }
    }
    var abierto by rememberSaveable { mutableStateOf(spotInicial) }
    val lista = rememberLazyListState()
    // Si se entro desde un restaurante, la lista baja hasta el.
    LaunchedEffect(resumen, spotInicial) {
        val indice = resumen?.sitios?.indexOfFirst { it.spotId == spotInicial } ?: -1
        if (indice >= 0) lista.animateScrollToItem(indice + 2)
    }

    Scaffold(modifier = modifier, containerColor = AppColors.cream) { innerPadding ->
        LazyColumn(
            state = lista,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Encabezado(
                    ventanaDias = estado.ventanaDias,
                    cargando = estado.cargando,
                    alElegirVentana = { alCargar(it, false) },
                    alRefrescar = { alCargar(estado.ventanaDias, true) },
                    alVolver = alVolver,
                )
            }
            item {
                Column {
                    if (estado.cargando) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = AppColors.tomato,
                            trackColor = AppColors.tomatoLight,
                        )
                    }
                    estado.aviso?.let { AvisoSinRed(it) }
                    estado.error?.let { error ->
                        TarjetaMensaje(error, accion = "Retry") { alCargar(estado.ventanaDias, true) }
                    }
                }
            }
            if (resumen != null) {
                item { TarjetaCampus(resumen.campus, resumen.totalReportes, estado.ventanaDias) }
                items(resumen.sitios, key = { it.spotId }) { sitio ->
                    TarjetaSitioHora(
                        sitio = sitio,
                        foto = fotos[sitio.spotId],
                        expandido = abierto == sitio.spotId,
                        alTocar = { abierto = if (abierto == sitio.spotId) null else sitio.spotId },
                        alAbrirSitio = { alAbrirSitio(sitio.spotId) },
                    )
                }
                items(sinReportes, key = { "sin-" + it.id }) { spot ->
                    TarjetaSinReportes(spot, fotos[spot.id])
                }
            }
        }
    }
}

@Composable
private fun Encabezado(
    ventanaDias: Int,
    cargando: Boolean,
    alElegirVentana: (Int) -> Unit,
    alRefrescar: () -> Unit,
    alVolver: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = alVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.espresso)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Best time to go", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso)
                Text("When the line is shortest, from students' wait reports", fontSize = 12.sp, color = AppColors.muted)
            }
            IconButton(onClick = alRefrescar, enabled = !cargando) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = AppColors.espresso)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipPildora(text = "Last 7 days", selected = ventanaDias == 7, onClick = { alElegirVentana(7) })
            ChipPildora(text = "Last 28 days", selected = ventanaDias == 28, onClick = { alElegirVentana(28) })
        }
    }
}

@Composable
private fun TarjetaCampus(campus: MejorHoraSitio?, totalReportes: Int, ventanaDias: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text("🏫 Whole campus", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.muted)
        val mejor = campus?.mejor
        if (campus == null || mejor == null) {
            Text(
                "Not enough wait reports in the last $ventanaDias days yet. Report the line when you buy food to help other students.",
                fontSize = 13.sp,
                color = AppColors.espresso,
                modifier = Modifier.padding(top = 6.dp),
            )
            return@Column
        }
        Text(
            "Calmest at ${ReglasMejorHora.textoHora(mejor.hora)}",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.espresso,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "${formatearMinutos(mejor.promedioMinutos)} average wait · $totalReportes reports in the last $ventanaDias days",
            fontSize = 12.sp,
            color = AppColors.muted,
        )
        Spacer(Modifier.height(12.dp))
        BarrasPorHora(campus.porHora, mejor, campus.peor)
    }
}

@Composable
private fun TarjetaSitioHora(
    sitio: MejorHoraSitio,
    foto: String?,
    expandido: Boolean,
    alTocar: () -> Unit,
    alAbrirSitio: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, if (expandido) AppColors.tomato else AppColors.border, RoundedCornerShape(18.dp))
            .clickable(onClick = alTocar)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FotoComida(
                url = foto,
                emoji = sitio.emoji,
                fondo = AppColors.tomatoLight,
                esquinas = 12.dp,
                tamanoEmoji = 22.sp,
                descripcion = sitio.nombre,
                modifier = Modifier.size(52.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(sitio.nombre, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso)
                val mejor = sitio.mejor
                val peor = sitio.peor
                if (mejor != null) {
                    Text(
                        "Go at ${ReglasMejorHora.textoHora(mejor.hora)} · ~${formatearMinutos(mejor.promedioMinutos)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D5B),
                    )
                    if (peor != null && peor.hora != mejor.hora) {
                        Text(
                            "Avoid ${ReglasMejorHora.textoHora(peor.hora)} · ~${formatearMinutos(peor.promedioMinutos)}",
                            fontSize = 12.sp,
                            color = AppColors.tomato,
                        )
                    }
                } else {
                    Text("Only ${sitio.reportes} report(s): not enough to recommend an hour", fontSize = 12.sp, color = AppColors.muted)
                }
            }
            Icon(
                if (expandido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expandido) "Hide hours" else "Show hours",
                tint = AppColors.muted,
            )
        }
        if (expandido) {
            Spacer(Modifier.height(12.dp))
            val proxima = sitio.proximaTranquila
            Text(
                when {
                    sitio.mejor == null -> "Hours with reports so far:"
                    proxima != null -> "Next calm hour today: ${ReglasMejorHora.textoHora(proxima.hora)} (~${formatearMinutos(proxima.promedioMinutos)})"
                    else -> "No calm hours left today. Tomorrow, go at ${ReglasMejorHora.textoHora(sitio.mejor.hora)}."
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.espresso,
            )
            Spacer(Modifier.height(10.dp))
            BarrasPorHora(sitio.porHora, sitio.mejor, sitio.peor)
            Spacer(Modifier.height(8.dp))
            Text(
                "${sitio.reportes} reports · see restaurant",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.tomato,
                modifier = Modifier.clickable(onClick = alAbrirSitio).padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun TarjetaSinReportes(spot: Spot, foto: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FotoComida(
            url = foto,
            emoji = spot.emoji,
            fondo = spot.emojiBackground,
            esquinas = 12.dp,
            tamanoEmoji = 22.sp,
            descripcion = spot.name,
            modifier = Modifier.size(44.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(spot.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.espresso)
            Text("No wait reports in this period yet", fontSize = 12.sp, color = AppColors.muted)
        }
    }
}

// Barras de 7 am a 8 pm. Verde = la mejor hora, rojo = la peor, gris = sin reportes.
@Composable
private fun BarrasPorHora(porHora: List<EsperaHora>, mejor: EsperaHora?, peor: EsperaHora?) {
    val horas = 7..20
    val maximo = (porHora.maxOfOrNull { it.promedioMinutos } ?: 1.0).coerceAtLeast(1.0)
    val porNumero = remember(porHora) { porHora.associateBy { it.hora } }
    Row(
        modifier = Modifier.fillMaxWidth().height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (hora in horas) {
            val dato = porNumero[hora]
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fraccion = if (dato == null) 0.04f else (dato.promedioMinutos / maximo).toFloat().coerceIn(0.08f, 1f)
                val color = when {
                    dato == null -> AppColors.border
                    hora == mejor?.hora -> AppColors.mint
                    hora == peor?.hora -> AppColors.tomato
                    else -> AppColors.espresso.copy(alpha = 0.35f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((ALTO_BARRA_DP * fraccion).dp)
                        .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                )
                Text(
                    if (hora % 2 == 1) ReglasMejorHora.textoHora(hora).replace(" ", "") else "",
                    fontSize = 8.sp,
                    color = AppColors.muted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AvisoSinRed(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(14.dp))
        Text("  $texto", fontSize = 12.sp, color = AppColors.muted)
    }
}

@Composable
private fun TarjetaMensaje(texto: String, accion: String, alTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
            modifier = Modifier.width(56.dp).clickable(onClick = alTocar).padding(start = 12.dp),
        )
    }
}

private const val ALTO_BARRA_DP = 76f

private fun formatearMinutos(minutos: Double): String = "${Math.round(minutos)} min"
