package com.example.frontend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.FeedFilterType
import com.example.frontend.model.Spot
import com.example.frontend.model.TiempoEspera
import com.example.frontend.data.medir
import com.example.frontend.data.sampleSpots
import com.example.frontend.ui.EstadoPopularidad
import java.text.Normalizer
import com.example.frontend.ui.components.ChipPildora
import com.example.frontend.ui.components.ListaSitios
import com.example.frontend.ui.components.SeccionPopularidad
import com.example.frontend.ui.theme.AppColors

@Stable
class FiltrosFeed(initial: Collection<FeedFilterType>) {
    private val active = mutableStateListOf<FeedFilterType>().apply { addAll(initial) }

    fun estaActivo(filter: FeedFilterType) = filter in active

    fun alternar(filter: FeedFilterType) {
        if (!active.remove(filter)) active.add(filter)
    }

    fun filtrar(spots: List<Spot>) = spots.filter { spot -> active.all(spot::cumpleFiltro) }
}

// Busqueda por nombre, tipo de comida, ubicacion y platos del menu. Cuando hay texto, los chips no aplican:
// el usuario esta buscando algo concreto, no filtrando el feed. Sin tildes: "taqueria" encuentra "Taquería",
// y "taco" encuentra la taqueria por su menu (el del catalogo empaquetado si el backend no mando el detalle).
//
// Micro-optimizacion (Sprint 4, Tomas). Antes, por CADA letra escrita y por CADA restaurante, se:
//  - compilaba un Regex nuevo (Regex("\\p{Mn}+") dentro de sinTildes),
//  - buscaba el menu con un recorrido lineal de sampleSpots (firstOrNull),
//  - armaba el texto con plantillas y joinToString y lo normalizaba (Normalizer + replace + lowercase).
// Como la funcion se llamaba directo en la composicion, todo eso tambien corria en cada recomposicion del
// feed (llega un tiempo de espera, cambia la red...), aunque la consulta no hubiera cambiado.
// Ahora:
//  - el Regex se compila una sola vez (constante),
//  - el texto normalizado de cada restaurante se arma una vez por catalogo (IndiceBusqueda, con remember),
//  - los menus del catalogo empaquetado se leen de un HashMap por id (O(1)) y no con firstOrNull (O(n)),
//  - por letra solo se normaliza la consulta y se hace un contains por restaurante, con un for por indice,
//  - y el resultado se recuerda por (indice, consulta): una recomposicion sin cambios no vuelve a buscar.
private val MARCAS_DE_TILDE = Regex("\\p{Mn}+")
private val menusEmpaquetados: Map<String, List<String>> by lazy {
    HashMap<String, List<String>>(sampleSpots.size * 2).apply {
        for (spot in sampleSpots) put(spot.id, spot.menu.map { it.name })
    }
}

private fun sinTildes(texto: String): String =
    MARCAS_DE_TILDE.replace(Normalizer.normalize(texto, Normalizer.Form.NFD), "").lowercase()

class IndiceBusqueda(private val spots: List<Spot>) {
    private val textos: Array<String> = medir("indexarBusqueda") {
        Array(spots.size) { i ->
            val spot = spots[i]
            val texto = StringBuilder(128).append(spot.name).append(' ').append(spot.cuisine).append(' ').append(spot.location)
            val menu = if (spot.menu.isNotEmpty()) spot.menu.map { it.name } else menusEmpaquetados[spot.id].orEmpty()
            for (plato in menu) texto.append(' ').append(plato)
            sinTildes(texto.toString())
        }
    }

    fun buscar(consulta: String): List<Spot> = medir("buscarSitios") {
        val q = sinTildes(consulta.trim())
        if (q.isEmpty()) return@medir spots
        val resultado = ArrayList<Spot>()
        for (i in textos.indices) if (textos[i].contains(q)) resultado.add(spots[i])
        resultado
    }
}

fun buscarSitios(spots: List<Spot>, consulta: String): List<Spot> = IndiceBusqueda(spots).buscar(consulta)

@Composable
fun PantallaParaTi(
    spots: List<Spot>,
    popularidad: EstadoPopularidad,
    alMarcarGuardado: (String) -> Unit,
    alAbrirSitio: (Spot) -> Unit,
    alAbrirDesdeBusqueda: (Spot) -> Unit,
    alAbrirSitioPorId: (String) -> Unit,
    alVerPopularidad: () -> Unit,
    modifier: Modifier = Modifier,
    tiemposEspera: Map<String, TiempoEspera> = emptyMap(),
    seccionMejorParaTi: @Composable () -> Unit = {},
    seccionCercanos: @Composable () -> Unit = {},
) {
    val filters = remember { FiltrosFeed(listOf(FeedFilterType.IN_A_RUSH)) }
    var consulta by rememberSaveable { mutableStateOf("") }
    val buscando = consulta.isNotBlank()
    // El indice se arma una vez por catalogo y el resultado se recalcula solo si cambia la consulta.
    val indice = remember(spots) { IndiceBusqueda(spots) }
    val resultados = remember(indice, consulta) { if (buscando) indice.buscar(consulta) else emptyList() }

    Column(modifier = modifier.fillMaxSize()) {
        EncabezadoParaTi(filters = filters, consulta = consulta, alCambiarConsulta = { consulta = it })
        if (buscando) {
            ListaSitios(
                spots = resultados,
                mensajeVacio = "No spots match \"${consulta.trim()}\".",
                alMarcarGuardado = alMarcarGuardado,
                // Abrir desde resultados de busqueda cuenta como "busqueda" para la BQ3.
                alAbrirSitio = alAbrirDesdeBusqueda,
                tiemposEspera = tiemposEspera,
            )
        } else {
            ListaSitios(
                spots = filters.filtrar(spots),
                mensajeVacio = "No spots match these filters yet.",
                alMarcarGuardado = alMarcarGuardado,
                alAbrirSitio = alAbrirSitio,
                // "Popular right now" (BQ3) se desplaza con el feed y solo aparece cuando hay datos.
                encabezado = {
                    Column {
                        seccionMejorParaTi()
                        seccionCercanos()
                        SeccionPopularidad(
                            estado = popularidad,
                            alAbrirSitio = alAbrirSitioPorId,
                            alVerTodo = alVerPopularidad,
                        )
                    }
                },
                tiemposEspera = tiemposEspera,
            )
        }
    }
}

@Composable
private fun EncabezadoParaTi(filters: FiltrosFeed, consulta: String, alCambiarConsulta: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            "CampusBites",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = AppColors.tomato,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            "Campus food,\ntailored for you",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.espresso,
            lineHeight = 32.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            "Curated by what students with your taste and schedule love",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.muted,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 12.dp),
        )
        BarraBusqueda(consulta = consulta, alCambiar = alCambiarConsulta)
        if (consulta.isBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(FeedFilterType.entries) { filter ->
                    ChipPildora(
                        text = filter.label,
                        selected = filters.estaActivo(filter),
                        onClick = { filters.alternar(filter) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BarraBusqueda(consulta: String, alCambiar: (String) -> Unit) {
    OutlinedTextField(
        value = consulta,
        onValueChange = alCambiar,
        singleLine = true,
        placeholder = { Text("Search a spot, cuisine or place", fontSize = 14.sp, color = AppColors.muted) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = AppColors.muted) },
        trailingIcon = {
            if (consulta.isNotEmpty()) {
                IconButton(onClick = { alCambiar("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = AppColors.muted)
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppColors.card,
            unfocusedContainerColor = AppColors.card,
            focusedBorderColor = AppColors.tomato,
            unfocusedBorderColor = AppColors.border,
            cursorColor = AppColors.tomato,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
    )
}
