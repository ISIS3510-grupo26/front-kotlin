package com.example.frontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.data.sampleMapSpots
import com.example.frontend.model.MapSpot
import com.example.frontend.ui.theme.AppColors
import androidx.compose.material.icons.filled.MyLocation

@Composable
fun PantallaMapa(
    alAbrirLugar: (MapSpot) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spots = remember { sampleMapSpots }
    var seleccionado by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        EncabezadoMapa()
        BarraBusquedaMapa()
        AreaMapa(
            spots = spots,
            seleccionado = seleccionado,
            alSeleccionar = { seleccionado = it },
            alAbrirLugar = alAbrirLugar,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 0.dp),
        )
    }
}

@Composable
private fun EncabezadoMapa() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Taste Map",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.espresso,
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(AppColors.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Tune, contentDescription = "Filtros", tint = AppColors.espresso)
        }
    }
}

@Composable
private fun BarraBusquedaMapa() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .height(52.dp)
            .background(AppColors.card, RoundedCornerShape(999.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Search food stalls, trucks, campus spots...",
            fontSize = 14.sp,
            color = AppColors.muted,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.MicNone, contentDescription = "Búsqueda por voz", tint = AppColors.muted, modifier = Modifier.size(20.dp))
    }
}


@Composable
private fun AreaMapa(
    spots: List<MapSpot>,
    seleccionado: Int,
    alSeleccionar: (Int) -> Unit,
    alAbrirLugar: (MapSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.background(Color(0xFFD9EDE2))) {
        val w = maxWidth
        val h = maxHeight

        ZonaBloque(width = 120.dp, height = 90.dp, color = Color(0xFFE4D5CC), left = w * 0.05f, top = h * 0.08f)
        ZonaBloque(width = 140.dp, height = 70.dp, color = Color(0xFFF2CFC6), left = w * 0.45f, top = h * 0.05f)
        ZonaBloque(width = 90.dp, height = 110.dp, color = Color(0xFFE4D5CC), left = w * 0.7f, top = h * 0.3f)
        ZonaBloque(width = 100.dp, height = 80.dp, color = Color(0xFFF2CFC6), left = w * 0.08f, top = h * 0.45f)
        ZonaBloque(width = 130.dp, height = 95.dp, color = Color(0xFFE4D5CC), left = w * 0.4f, top = h * 0.55f)
        ZonaBloque(width = 110.dp, height = 75.dp, color = Color(0xFFF2CFC6), left = w * 0.65f, top = h * 0.68f)

        Box(Modifier.offset(x = 0.dp, y = h * 0.27f).width(w).height(14.dp).background(AppColors.cream))
        Box(Modifier.offset(x = 0.dp, y = h * 0.62f).width(w).height(14.dp).background(AppColors.cream))
        Box(Modifier.offset(x = w * 0.32f, y = 0.dp).width(14.dp).height(h).background(AppColors.cream))
        Box(Modifier.offset(x = w * 0.78f, y = 0.dp).width(14.dp).height(h).background(AppColors.cream))

        EtiquetaZona("SCIENCE QUAD", Modifier.align(Alignment.TopStart).padding(16.dp))
        EtiquetaZona("ENGINEERING HUB", Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp))
        EtiquetaZona("STUDENT UNION", Modifier.align(Alignment.BottomStart).padding(bottom = 140.dp, start = 16.dp))
        EtiquetaZona("SPORTS ARENA", Modifier.align(Alignment.BottomEnd).padding(bottom = 110.dp, end = 16.dp))

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(44.dp)
                .background(AppColors.card, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Mi ubicación", tint = AppColors.espresso)
        }

        spots.forEachIndexed { index, spot ->
            Pin(
                spot = spot,
                selected = index == seleccionado,
                modifier = Modifier.offset(x = w * spot.dx - 28.dp, y = h * spot.dy - 24.dp),
                onTap = { alSeleccionar(index) },
            )
        }

        TarjetaLugarSeleccionado(
            spot = spots[seleccionado],
            alAbrirLugar = alAbrirLugar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun ZonaBloque(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, color: Color, left: androidx.compose.ui.unit.Dp, top: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .offset(x = left, y = top)
            .width(width)
            .height(height)
            .background(color, RoundedCornerShape(16.dp)),
    )
}

@Composable
private fun EtiquetaZona(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = AppColors.muted,
    )
}

@Composable
private fun Pin(spot: MapSpot, selected: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (selected) AppColors.tomato else AppColors.espresso
    Column(
        modifier = modifier.clickable { onTap() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .background(color, RoundedCornerShape(999.dp))
                .then(
                    if (selected) Modifier.border(3.dp, Color.White, RoundedCornerShape(999.dp))
                    else Modifier
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(spot.emoji, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text("${spot.affinity}%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        Box(
            modifier = Modifier
                .offset(y = (-3).dp)
                .size(8.dp)
                .rotate(45f)
                .background(color, RectangleShape),
        )
    }
}

@Composable
private fun TarjetaLugarSeleccionado(
    spot: MapSpot,
    alAbrirLugar: (MapSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(AppColors.surface, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(spot.emoji, fontSize = 28.sp)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(22.dp)
                    .background(AppColors.tomato, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("${spot.rank}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    spot.name,
                    maxLines = 1,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.espresso,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.Star, contentDescription = null, tint = AppColors.tomato, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("${spot.rating}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.tomato)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${spot.price} • ${spot.walkLabel} • ${spot.affinity}% Taste Match",
                maxLines = 1,
                fontSize = 12.sp,
                color = AppColors.muted,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                PildoraMeta(label = "Quick Line", background = AppColors.tomatoLight, foreground = AppColors.tomato)
                Spacer(Modifier.width(8.dp))
                PildoraMeta(label = spot.location, background = AppColors.surface, foreground = AppColors.espresso)
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AppColors.tomato, CircleShape)
                .clickable { alAbrirLugar(spot) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Ver detalle", tint = Color.White)
        }
    }
}

@Composable
private fun PildoraMeta(label: String, background: Color, foreground: Color) {
    Text(
        label,
        maxLines = 1,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = foreground,
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}