package com.example.frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.frontend.data.sampleSpots
import com.example.frontend.model.Spot
import com.example.frontend.ui.components.BottomTab
import com.example.frontend.ui.components.BarraInferior
import com.example.frontend.ui.screens.PantallaProximamente
import com.example.frontend.ui.screens.PantallaGuardados
import com.example.frontend.ui.screens.PantallaParaTi
import com.example.frontend.ui.screens.PantallaPerfil
import com.example.frontend.ui.screens.PantallaDetalle
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

// Dueño del estado compartido (lista de spots + tab activo + spot abierto).
@Composable
fun PantallaPrincipal() {
    var currentTab by remember { mutableStateOf(BottomTab.FOR_YOU) }
    var spots by remember { mutableStateOf(sampleSpots) }
    var selectedSpot by remember { mutableStateOf<Spot?>(null) }

    fun marcarGuardado(id: String) {
        spots = spots.map { spot -> if (spot.id == id) spot.copy(isSaved = !spot.isSaved) else spot }
    }

    val abrirSitio: (Spot) -> Unit = { spot -> selectedSpot = spot }

    val detailSpot = selectedSpot
    if (detailSpot != null) {
        PantallaDetalle(
            spot = detailSpot,
            alMarcarGuardado = ::marcarGuardado,
            alVolver = { selectedSpot = null },
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
            BottomTab.MAP -> PantallaProximamente(
                title = "Map",
                icon = Icons.Filled.Map,
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
                modifier = contentModifier,
            )
        }
    }
}
