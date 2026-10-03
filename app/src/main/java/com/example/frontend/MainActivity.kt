package com.example.frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.frontend.model.TasteProfile
import com.example.frontend.ui.CampusBitesViewModel
import com.example.frontend.ui.components.BarraInferior
import com.example.frontend.ui.components.BottomTab
import com.example.frontend.ui.screens.PantallaDetalle
import com.example.frontend.ui.screens.PantallaEscribirResena
import com.example.frontend.ui.screens.PantallaGuardados
import com.example.frontend.ui.screens.PantallaMapa
import com.example.frontend.ui.screens.PantallaParaTi
import com.example.frontend.ui.screens.PantallaPerfil
import com.example.frontend.ui.screens.PantallaPerfilGusto
import com.example.frontend.ui.screens.PantallaPopularidad
import com.example.frontend.data.contexto.ContextoHorario
import com.example.frontend.ui.theme.AppColors
import com.example.frontend.ui.theme.TemaCampusBites

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaCampusBites {
                PantallaPrincipal()
            }
        }
    }
}

// La UI observa el estado del ViewModel (MVVM); los datos vienen del backend vía SpotsRepository.
@Composable
fun PantallaPrincipal(viewModel: CampusBitesViewModel = viewModel()) {
    var currentTab by remember { mutableStateOf(BottomTab.FOR_YOU) }
    var perfilGusto by remember { mutableStateOf(TasteProfile()) }
    var editandoGusto by remember { mutableStateOf(false) }
    var escribiendoResena by remember { mutableStateOf(false) }
    var viendoPopularidad by remember { mutableStateOf(false) }

    val spots = viewModel.spots
    val detalle = viewModel.detalle

    // Abrir por id sirve tanto para las tarjetas del feed como para el ranking "Popular right now".
    val abrirPorId: (String) -> Unit = { id -> viewModel.buscarSitio(id)?.let(viewModel::abrirSitio) }

    if (editandoGusto) {
        BackHandler { editandoGusto = false }
        PantallaPerfilGusto(
            perfilInicial = perfilGusto,
            alGuardar = { nuevo ->
                perfilGusto = nuevo
                editandoGusto = false
            },
            alVolver = { editandoGusto = false },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    if (detalle != null && escribiendoResena) {
        BackHandler { escribiendoResena = false }
        PantallaEscribirResena(
            placeName = "${detalle.spot.name} · ${detalle.spot.location}",
            alPublicar = { estrellas, texto ->
                viewModel.agregarResena(detalle.spot.id, estrellas, texto)
                escribiendoResena = false
            },
            alVolver = { escribiendoResena = false },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    if (detalle != null) {
        BackHandler { viewModel.cerrarDetalle() }
        PantallaDetalle(
            spot = detalle.spot,
            alMarcarGuardado = viewModel::marcarGuardado,
            alVolver = viewModel::cerrarDetalle,
            alEscribirResena = { escribiendoResena = true },
            modifier = Modifier.fillMaxSize(),
            cargando = detalle.cargando,
            error = detalle.error,
            alReintentar = viewModel::reintentarDetalle,
        )
        return
    }
    if (viendoPopularidad) {
        BackHandler { viendoPopularidad = false }
        PantallaPopularidad(
            horaActual = ContextoHorario.Dispositivo.horaLocal(),
            cargar = viewModel::popularidadPorHoras,
            alAbrirSitio = abrirPorId,
            cargarBusquedasPorDia = viewModel::busquedasPorDiaSemana,
            alVolver = { viendoPopularidad = false },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BarraInferior(current = currentTab, alElegir = { currentTab = it }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (viewModel.modoRespaldo) {
                AvisoSinConexion(alReintentar = viewModel::cargarSitios)
            }
            val contentModifier = Modifier.fillMaxSize()

            if (viewModel.cargandoLista && spots.isEmpty()) {
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.tomato)
                }
                return@Column
            }

            when (currentTab) {
                BottomTab.FOR_YOU -> PantallaParaTi(
                    spots = spots,
                    popularidad = viewModel.popularidad,
                    alMarcarGuardado = viewModel::marcarGuardado,
                    alAbrirSitio = viewModel::abrirSitio,
                    alAbrirDesdeBusqueda = viewModel::abrirDesdeBusqueda,
                    alAbrirSitioPorId = abrirPorId,
                    alVerPopularidad = { viendoPopularidad = true },
                    modifier = contentModifier,
                )
                BottomTab.MAP -> PantallaMapa(
                    alAbrirLugar = { mapSpot ->
                        spots.firstOrNull { it.name == mapSpot.name }?.let(viewModel::abrirSitio)
                    },
                    modifier = contentModifier,
                )
                BottomTab.SAVED -> PantallaGuardados(
                    spots = spots,
                    alMarcarGuardado = viewModel::marcarGuardado,
                    alAbrirSitio = viewModel::abrirSitio,
                    modifier = contentModifier,
                )
                BottomTab.PROFILE -> PantallaPerfil(
                    savedCount = spots.count { it.isSaved },
                    perfil = perfilGusto,
                    alEditarGusto = { editandoGusto = true },
                    modifier = contentModifier,
                )
            }
        }
    }
}

@Composable
private fun AvisoSinConexion(alReintentar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.tomatoLight)
            .clickable(onClick = alReintentar)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Offline: showing saved catalog. Tap to retry.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.tomato,
        )
    }
}
