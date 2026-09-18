package com.example.frontend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.FeedFilterType
import com.example.frontend.model.Spot
import com.example.frontend.ui.components.ChipPildora
import com.example.frontend.ui.components.ListaSitios
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

@Composable
fun PantallaParaTi(
    spots: List<Spot>,
    alMarcarGuardado: (String) -> Unit,
    alAbrirSitio: (Spot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = remember { FiltrosFeed(listOf(FeedFilterType.IN_A_RUSH)) }

    Column(modifier = modifier.fillMaxSize()) {
        EncabezadoParaTi(filters)
        ListaSitios(
            spots = filters.filtrar(spots),
            mensajeVacio = "No spots match these filters yet.",
            alMarcarGuardado = alMarcarGuardado,
            alAbrirSitio = alAbrirSitio,
        )
    }
}

@Composable
private fun EncabezadoParaTi(filters: FiltrosFeed) {
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
