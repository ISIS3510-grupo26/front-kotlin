package com.example.frontend.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.frontend.data.ubicacion.UbicacionCampus
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.data.pinesDelMapa
import com.example.frontend.model.MapSpot
import com.example.frontend.model.Spot
import com.example.frontend.model.TiempoEspera
import com.example.frontend.ui.theme.AppColors
import androidx.compose.material.icons.filled.MyLocation

// Mapa real de Uniandes (OpenStreetMap) con un pin en las coordenadas de cada restaurante. Los pines salen
// del catalogo que ya tiene el ViewModel (red, Room o empaquetado), asi que se dibujan igual sin conexion;
// sin red solo se ven las teselas que ya estaban en cache. El aviso offline lo pone MainActivity.
@Composable
fun PantallaMapa(
    spots: List<Spot>,
    tiemposEspera: Map<String, TiempoEspera>,
    alAbrirSitio: (spotId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pines = remember(spots, tiemposEspera) { pinesDelMapa(spots, tiemposEspera) }
    // Se recuerda el id y no la posicion: si cambia el orden (otro perfil de gusto), sigue el mismo restaurante.
    var seleccionado by rememberSaveable { mutableStateOf<String?>(null) }
    val actual = pines.firstOrNull { it.spotId == seleccionado } ?: pines.firstOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        EncabezadoMapa()
        BarraBusquedaMapa()
        AreaMapa(
            spots = pines,
            seleccionado = actual,
            alSeleccionar = { seleccionado = it.spotId },
            alAbrirLugar = { alAbrirSitio(it.spotId) },
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
    seleccionado: MapSpot?,
    alSeleccionar: (MapSpot) -> Unit,
    alAbrirLugar: (MapSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapa = remember { crearMapa(context) }
    // La camara se encuadra una sola vez: si se moviera con cada cambio de pines, el estudiante perderia
    // el zoom que eligio cada vez que llega un tiempo de espera nuevo.
    var encuadrado by remember { mutableStateOf(false) }

    // osmdroid necesita enterarse del ciclo de vida para pausar la descarga de teselas y liberar memoria.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapa) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observador)
        onDispose {
            lifecycle.removeObserver(observador)
            mapa.onDetach()
        }
    }

    // clipToBounds: sin esto el MapView pinta teselas por fuera de su area, encima de la barra de busqueda.
    Box(modifier = modifier.clipToBounds().background(Color(0xFFD9EDE2))) {
        AndroidView(
            factory = { mapa },
            update = { vista ->
                dibujarPines(vista, spots, seleccionado?.spotId, alSeleccionar)
                if (!encuadrado && spots.isNotEmpty()) {
                    encuadrado = true
                    vista.post { encuadrar(vista, spots) }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(44.dp)
                .background(AppColors.card, CircleShape)
                .clickable { encuadrar(mapa, spots) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Centrar en el campus", tint = AppColors.espresso)
        }

        if (seleccionado == null) {
            Text(
                "No spots with a location to show yet.",
                fontSize = 13.sp,
                color = AppColors.muted,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(AppColors.card, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        } else {
            TarjetaLugarSeleccionado(
                spot = seleccionado,
                alAbrirLugar = alAbrirLugar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}

private fun crearMapa(context: Context): MapView {
    // OpenStreetMap exige identificar la app en cada descarga y bloquea (403) a las que no lo hacen bien:
    // con el nombre del paquete (com.example.frontend) devolvia "Access blocked" en vez del mapa.
    // El cache va en almacenamiento interno: asi no se piden permisos de almacenamiento y lo ya visto
    // del campus se sigue viendo sin red.
    Configuration.getInstance().apply {
        userAgentValue = "CampusBites/1.0 (Uniandes ISIS3510)"
        osmdroidBasePath = File(context.filesDir, "osmdroid")
        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    }
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        // Sin esto las calles y los nombres se ven diminutos en pantallas de alta densidad.
        isTilesScaledToDpi = true
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        minZoomLevel = 14.0
        maxZoomLevel = 20.0
        controller.setZoom(ZOOM_CAMPUS)
        controller.setCenter(GeoPoint(UbicacionCampus.LAT, UbicacionCampus.LNG))
        // La licencia de OpenStreetMap exige mostrar el credito sobre el mapa.
        overlays.add(CopyrightOverlay(context))
    }
}

// Encuadra todos los restaurantes. La caja se estira hacia el sur porque la tarjeta del lugar
// seleccionado tapa la parte de abajo del mapa; asi los pines quedan en la parte visible.
private fun encuadrar(mapa: MapView, spots: List<MapSpot>) {
    if (spots.isEmpty() || mapa.width == 0 || mapa.height == 0) {
        mapa.controller.animateTo(GeoPoint(UbicacionCampus.LAT, UbicacionCampus.LNG), ZOOM_CAMPUS, 400L)
        return
    }
    val norte = spots.maxOf { it.lat }
    val sur = spots.minOf { it.lat }
    val este = spots.maxOf { it.lng }
    val oeste = spots.minOf { it.lng }
    // Con un solo restaurante la caja mide 0 y el zoom se iria al maximo.
    val alto = (norte - sur).coerceAtLeast(0.002)
    val ancho = (este - oeste).coerceAtLeast(0.002)
    val caja = BoundingBox(norte + alto * 0.25, este + ancho * 0.15, sur - alto * 0.9, oeste - ancho * 0.15)
    mapa.zoomToBoundingBox(caja, true, 0)
}

private fun dibujarPines(mapa: MapView, spots: List<MapSpot>, seleccionadoId: String?, alSeleccionar: (MapSpot) -> Unit) {
    mapa.overlays.removeAll { it is Marker }
    // El seleccionado se agrega de ultimo para que quede encima si dos pines se tocan.
    spots.sortedBy { it.spotId == seleccionadoId }.forEach { spot ->
        val seleccionado = spot.spotId == seleccionadoId
        mapa.overlays.add(
            Marker(mapa).apply {
                position = GeoPoint(spot.lat, spot.lng)
                // La punta de abajo del pin cae justo en la coordenada del restaurante.
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = iconoPin(mapa.context, spot, seleccionado)
                title = spot.name
                setOnMarkerClickListener { _, _ ->
                    alSeleccionar(spot)
                    true
                }
            },
        )
    }
    mapa.invalidate()
}

// El mismo pin que antes (emoji + % de compatibilidad en una pastilla) pero dibujado como imagen,
// porque los marcadores de osmdroid son vistas de Android y no Compose.
private fun iconoPin(context: Context, spot: MapSpot, seleccionado: Boolean): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val texto = "${spot.emoji} ${spot.affinity}%"
    val pintura = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 13 * d
        typeface = Typeface.DEFAULT_BOLD
    }
    val borde = if (seleccionado) 3 * d else 0f
    val anchoPastilla = pintura.measureText(texto) + 20 * d + 2 * borde
    val altoPastilla = 32 * d + 2 * borde
    val punta = 7 * d
    val bitmap = Bitmap.createBitmap(anchoPastilla.toInt(), (altoPastilla + punta).toInt(), Bitmap.Config.ARGB_8888)
    val lienzo = Canvas(bitmap)
    val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = (if (seleccionado) AppColors.tomato else AppColors.espresso).toArgb()
    }
    if (seleccionado) {
        val blanco = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        lienzo.drawRoundRect(RectF(0f, 0f, anchoPastilla, altoPastilla), altoPastilla / 2, altoPastilla / 2, blanco)
    }
    val interior = RectF(borde, borde, anchoPastilla - borde, altoPastilla - borde)
    lienzo.drawRoundRect(interior, interior.height() / 2, interior.height() / 2, relleno)
    val triangulo = Path().apply {
        moveTo(anchoPastilla / 2 - punta, altoPastilla - borde - 1)
        lineTo(anchoPastilla / 2 + punta, altoPastilla - borde - 1)
        lineTo(anchoPastilla / 2, altoPastilla + punta)
        close()
    }
    lienzo.drawPath(triangulo, relleno)
    val baseTexto = interior.centerY() - (pintura.descent() + pintura.ascent()) / 2
    lienzo.drawText(texto, borde + 10 * d, baseTexto, pintura)
    return BitmapDrawable(context.resources, bitmap)
}

private const val ZOOM_CAMPUS = 17.0

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
                PildoraMeta(
                    label = spot.minutosEspera?.let { "~$it min wait" } ?: "No recent wait report",
                    background = AppColors.tomatoLight,
                    foreground = AppColors.tomato,
                )
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