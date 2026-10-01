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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.frontend.model.ActividadSitio
import com.example.frontend.model.PopularidadPorHora
import com.example.frontend.model.RankingHora
import com.example.frontend.ui.components.ChipPildora
import com.example.frontend.ui.components.InsigniaRanking
import com.example.frontend.ui.theme.AppColors
import kotlinx.coroutines.CancellationException

private sealed interface EstadoPantalla {
    data object Cargando : EstadoPantalla
    data object Error : EstadoPantalla
    data class Listo(val datos: PopularidadPorHora) : EstadoPantalla
}

// Vista completa de la BQ3: "que restaurantes reciben mas vistas y busquedas en cada hora".
// Arranca en la hora actual del celular (contexto) y deja recorrer las demas horas con chips.
@Composable
fun PantallaPopularidad(
    horaActual: Int,
    cargar: suspend () -> PopularidadPorHora,
    alAbrirSitio: (spotId: String) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var horaElegida by rememberSaveable { mutableStateOf(horaActual) }
    var estado by remember { mutableStateOf<EstadoPantalla>(EstadoPantalla.Cargando) }
    var intento by remember { mutableStateOf(0) }
    val chipsState: LazyListState = rememberLazyListState()

    LaunchedEffect(intento) {
        estado = EstadoPantalla.Cargando
        estado = try {
            EstadoPantalla.Listo(cargar())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            EstadoPantalla.Error
        }
    }
    // Al entrar, los chips arrancan centrados en la hora actual.
    LaunchedEffect(Unit) { chipsState.scrollToItem((horaActual - 2).coerceAtLeast(0)) }

    Scaffold(modifier = modifier, containerColor = AppColors.cream) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = alVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.espresso)
                }
                Column {
                    Text("Popular by hour", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso)
                    Text("Page views and searches per restaurant, this week", fontSize = 12.sp, color = AppColors.muted)
                }
            }
            val listo = estado as? EstadoPantalla.Listo
            val horasConActividad = listo?.datos?.horas?.map { it.hour }?.toSet().orEmpty()
            LazyRow(
                state = chipsState,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items((0 until 24).toList(), key = { it }) { hora ->
                    ChipPildora(
                        text = if (hora == horaActual) "Now · %02d:00".format(hora) else "%02d:00".format(hora),
                        selected = hora == horaElegida,
                        onClick = { horaElegida = hora },
                        count = listo?.datos?.horas?.firstOrNull { it.hour == hora }?.let { it.totalPageViews + it.totalSearches }
                            ?.takeIf { hora in horasConActividad },
                    )
                }
            }
            when (val actual = estado) {
                EstadoPantalla.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.tomato)
                }
                EstadoPantalla.Error -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Couldn't load the ranking. Check your connection.", fontSize = 13.sp, color = AppColors.muted)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { intento++ },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.tomato, contentColor = Color.White),
                    ) { Text("Retry", fontWeight = FontWeight.ExtraBold) }
                }
                is EstadoPantalla.Listo -> {
                    val ranking = actual.datos.horas.firstOrNull { it.hour == horaElegida }
                        ?: RankingHora(horaElegida, 0, 0, emptyList())
                    ListaRanking(ranking = ranking, dias = actual.datos.days, alAbrirSitio = alAbrirSitio)
                }
            }
        }
    }
}

@Composable
private fun ListaRanking(ranking: RankingHora, dias: Int, alAbrirSitio: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "resumen") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.tomatoLight, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🕒", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(ranking.etiquetaHora, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.tomato)
                    Text(
                        "${ranking.totalPageViews} page views · ${ranking.totalSearches} searches in the last $dias days",
                        fontSize = 12.sp,
                        color = AppColors.espresso,
                    )
                }
            }
        }
        if (ranking.sitios.isEmpty()) {
            item(key = "vacio") {
                Text(
                    "No activity at this hour yet.",
                    color = AppColors.muted,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        items(ranking.sitios, key = ActividadSitio::spotId) { sitio ->
            FilaRanking(sitio = sitio, maximo = ranking.sitios.first().total, onClick = { alAbrirSitio(sitio.spotId) })
        }
    }
}

@Composable
private fun FilaRanking(sitio: ActividadSitio, maximo: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InsigniaRanking(rank = sitio.rank, size = 32)
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.size(44.dp).background(AppColors.surface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(sitio.emoji, fontSize = 22.sp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(sitio.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.espresso)
            Text(
                "👀 ${sitio.pageViews} page views · 🔎 ${sitio.searches} searches",
                fontSize = 12.sp,
                color = AppColors.muted,
            )
            Spacer(Modifier.height(6.dp))
            BarraActividad(vistas = sitio.pageViews, busquedas = sitio.searches, maximo = maximo)
        }
        Spacer(Modifier.width(10.dp))
        Text("${sitio.total}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.tomato)
    }
}

// Barra apilada: la parte solida son vistas de pagina, la clara son busquedas.
@Composable
private fun BarraActividad(vistas: Int, busquedas: Int, maximo: Int) {
    val total = (vistas + busquedas).coerceAtLeast(1)
    val ancho = (total.toFloat() / maximo.coerceAtLeast(1)).coerceIn(0.04f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(AppColors.surface, RoundedCornerShape(999.dp)),
    ) {
        Row(modifier = Modifier.fillMaxWidth(ancho).height(6.dp)) {
            if (vistas > 0) {
                Box(Modifier.weight(vistas.toFloat()).height(6.dp).background(AppColors.tomato, RoundedCornerShape(999.dp)))
            }
            if (busquedas > 0) {
                Box(Modifier.weight(busquedas.toFloat()).height(6.dp).background(AppColors.mint, RoundedCornerShape(999.dp)))
            }
        }
    }
}
