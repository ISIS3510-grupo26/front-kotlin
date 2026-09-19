package com.example.frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.frontend.data.sampleSpots
import com.example.frontend.model.PeerReview
import com.example.frontend.model.Spot
import com.example.frontend.ui.components.BottomTab
import com.example.frontend.ui.components.BarraInferior
import com.example.frontend.ui.screens.PantallaGuardados
import com.example.frontend.ui.screens.PantallaParaTi
import com.example.frontend.ui.screens.PantallaPerfil
import com.example.frontend.ui.screens.PantallaDetalle
import com.example.frontend.ui.theme.TemaCampusBites
import com.example.frontend.model.TasteProfile
import com.example.frontend.ui.screens.PantallaEscribirResena
import com.example.frontend.ui.screens.PantallaPerfilGusto
import com.example.frontend.ui.screens.PantallaMapa



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

// Dueño del estado compartido (lista de spots + tab activo + spot abierto).
@Composable
fun PantallaPrincipal() {
    var currentTab by remember { mutableStateOf(BottomTab.FOR_YOU) }
    var spots by remember { mutableStateOf(sampleSpots) }
    var selectedSpot by remember { mutableStateOf<Spot?>(null) }
    var perfilGusto by remember { mutableStateOf(TasteProfile()) }
    var editandoGusto by remember { mutableStateOf(false) }
    var escribiendoResena by remember { mutableStateOf(false) }

    fun marcarGuardado(id: String) {
        spots = spots.map { spot -> if (spot.id == id) spot.copy(isSaved = !spot.isSaved) else spot }
    }

    val abrirSitio: (Spot) -> Unit = { spot -> selectedSpot = spot }
    if (editandoGusto) {
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
    val detailSpot = selectedSpot
    if (detailSpot != null && escribiendoResena) {
        PantallaEscribirResena(
            placeName = "${detailSpot.name} · ${detailSpot.location}",
            alPublicar = { estrellas, texto ->
                spots = spots.map { spot ->
                    if (spot.id != detailSpot.id) spot else spot.copy(
                        reviews = listOf(
                            PeerReview(
                                authorName = "Julian Bierez",
                                initials = "JB",
                                program = "Engineering, Sem 6",
                                stars = estrellas,
                                text = texto,
                                dinedAgo = "Dined today",
                                helpfulCount = 0,
                            )
                        ) + spot.reviews,
                        totalReviews = spot.totalReviews + 1,
                    )
                }
                selectedSpot = spots.firstOrNull { it.id == detailSpot.id }
                escribiendoResena = false
            },
            alVolver = { escribiendoResena = false },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    if (detailSpot != null) {
        PantallaDetalle(
            spot = detailSpot,
            alMarcarGuardado = ::marcarGuardado,
            alVolver = { selectedSpot = null },
            alEscribirResena = { escribiendoResena = true },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BarraInferior(current = currentTab, alElegir = { currentTab = it }) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())

        when (currentTab) {
            BottomTab.FOR_YOU -> PantallaParaTi(
                spots = spots,
                alMarcarGuardado = ::marcarGuardado,
                alAbrirSitio = abrirSitio,
                modifier = contentModifier,
            )
            BottomTab.MAP -> PantallaMapa(
                alAbrirLugar = { mapSpot ->
                    spots.firstOrNull { it.name == mapSpot.name }?.let(abrirSitio)
                },
                modifier = contentModifier,
            )
            BottomTab.SAVED -> PantallaGuardados(
                spots = spots,
                alMarcarGuardado = ::marcarGuardado,
                alAbrirSitio = abrirSitio,
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
