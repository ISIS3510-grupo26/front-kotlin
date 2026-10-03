package com.example.frontend.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.frontend.model.Spot
import com.example.frontend.ui.theme.AppColors

@Composable
fun ListaSitios(
    spots: List<Spot>,
    mensajeVacio: String,
    alMarcarGuardado: (String) -> Unit,
    alAbrirSitio: (Spot) -> Unit,
    modifier: Modifier = Modifier,
    // Contenido opcional que se desplaza junto con la lista (p. ej. la seccion "Trending now").
    encabezado: (@Composable () -> Unit)? = null,
) {
    if (spots.isEmpty() && encabezado == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(mensajeVacio, color = AppColors.muted)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (encabezado != null) {
            item(key = "encabezado") { encabezado() }
        }
        if (spots.isEmpty()) {
            item(key = "vacio") {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text(mensajeVacio, color = AppColors.muted)
                }
            }
        }
        items(spots, key = Spot::id) { spot ->
            TarjetaSitio(
                spot = spot,
                alMarcarGuardado = { alMarcarGuardado(spot.id) },
                onClick = { alAbrirSitio(spot) },
            )
        }
    }
}
