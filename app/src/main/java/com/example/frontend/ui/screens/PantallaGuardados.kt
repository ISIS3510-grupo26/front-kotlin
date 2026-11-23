package com.example.frontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.Spot
import com.example.frontend.model.SpotCategory
import com.example.frontend.model.TiempoEspera
import com.example.frontend.ui.components.ChipPildora
import com.example.frontend.ui.components.ListaSitios
import com.example.frontend.ui.theme.AppColors

// null representa "All Saved".
private val categoryTabs: List<SpotCategory?> = listOf(null) + SpotCategory.entries

@Composable
fun PantallaGuardados(
    spots: List<Spot>,
    alMarcarGuardado: (String) -> Unit,
    alAbrirSitio: (Spot) -> Unit,
    modifier: Modifier = Modifier,
    tiemposEspera: Map<String, TiempoEspera> = emptyMap(),
    alVerAbiertosCerca: () -> Unit = {},
) {
    var selected by rememberSaveable { mutableStateOf<SpotCategory?>(null) }

    val favorites = spots.filter { it.isSaved }
    val countByCategory = favorites.groupingBy { it.category }.eachCount()
    val visible = selected?.let { category -> favorites.filter { it.category == category } } ?: favorites

    Column(modifier = modifier.fillMaxSize()) {
        EncabezadoGuardados(
            total = favorites.size,
            contarPor = { category -> category?.let { countByCategory[it] ?: 0 } ?: favorites.size },
            selected = selected,
            alElegir = { selected = it },
        )
        if (favorites.isNotEmpty()) BotonAbiertosCerca(alVerAbiertosCerca)
        ListaSitios(
            spots = visible,
            mensajeVacio = "No saved places in this category yet.",
            alMarcarGuardado = alMarcarGuardado,
            alAbrirSitio = alAbrirSitio,
            tiemposEspera = tiemposEspera,
        )
    }
}

@Composable
private fun EncabezadoGuardados(
    total: Int,
    contarPor: (SpotCategory?) -> Int,
    selected: SpotCategory?,
    alElegir: (SpotCategory?) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            "Saved Places",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.espresso,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            "Your favorite campus spots • $total places saved",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.muted,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 10.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(categoryTabs) { category ->
                ChipPildora(
                    text = category?.label ?: "All Saved",
                    count = contarPor(category),
                    selected = selected == category,
                    onClick = { alElegir(category) },
                )
            }
        }
    }
}

// Sprint 4: entrada a "Open near you" (BQ5). En Flutter es el filtro "Open • ≤15 min" de esta misma pantalla.
@Composable
private fun BotonAbiertosCerca(alTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp)
            .background(AppColors.mintLight, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.mint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = alTocar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🕒", fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text("Open near you", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.espresso)
            Text("Which of these are open now, within a 15 min walk", fontSize = 12.sp, color = AppColors.muted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppColors.mint)
    }
}
