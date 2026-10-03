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
import com.example.frontend.ui.EstadoPopularidad
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

// Busqueda por nombre, tipo de comida o ubicacion. Cuando hay texto, los chips no aplican:
// el usuario esta buscando algo concreto, no filtrando el feed.
fun buscarSitios(spots: List<Spot>, consulta: String): List<Spot> {
    val q = consulta.trim()
    if (q.isEmpty()) return spots
    return spots.filter {
        it.name.contains(q, ignoreCase = true) ||
            it.cuisine.contains(q, ignoreCase = true) ||
            it.location.contains(q, ignoreCase = true)
    }
}

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
) {
    val filters = remember { FiltrosFeed(listOf(FeedFilterType.IN_A_RUSH)) }
    var consulta by rememberSaveable { mutableStateOf("") }
    val buscando = consulta.isNotBlank()

    Column(modifier = modifier.fillMaxSize()) {
        EncabezadoParaTi(filters = filters, consulta = consulta, alCambiarConsulta = { consulta = it })
        if (buscando) {
            ListaSitios(
                spots = buscarSitios(spots, consulta),
                mensajeVacio = "No spots match \"${consulta.trim()}\".",
                alMarcarGuardado = alMarcarGuardado,
                // Abrir desde resultados de busqueda cuenta como "busqueda" para la BQ3.
                alAbrirSitio = alAbrirDesdeBusqueda,
            )
        } else {
            ListaSitios(
                spots = filters.filtrar(spots),
                mensajeVacio = "No spots match these filters yet.",
                alMarcarGuardado = alMarcarGuardado,
                alAbrirSitio = alAbrirSitio,
                // "Popular right now" (BQ3) se desplaza con el feed y solo aparece cuando hay datos.
                encabezado = {
                    SeccionPopularidad(
                        estado = popularidad,
                        alAbrirSitio = alAbrirSitioPorId,
                        alVerTodo = alVerPopularidad,
                    )
                },
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
