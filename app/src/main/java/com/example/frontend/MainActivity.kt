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
import com.example.frontend.ui.components.CampusBottomNavBar
import com.example.frontend.ui.screens.ComingSoonScreen
import com.example.frontend.ui.screens.ForYouScreen
import com.example.frontend.ui.screens.ProfileScreen
import com.example.frontend.ui.screens.SavedPlacesScreen
import com.example.frontend.ui.screens.SpotDetailScreen
import com.example.frontend.ui.theme.FrontEndTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrontEndTheme {
                CampusBitesApp()
            }
        }
    }
}

/**
 * Hosts the four main tabs and owns the single list of [Spot]s shared by
 * the For You feed and Saved Places, so toggling the heart on a card in
 * either screen updates the same underlying saved state. Mirrors the
 * Flutter version's HomeShell: a `selectedSpot` set to non-null acts as a
 * one-level navigation stack for the detail screen.
 */
@Composable
fun CampusBitesApp() {
    var currentTab by remember { mutableStateOf(BottomTab.FOR_YOU) }
    var spots by remember { mutableStateOf(sampleSpots) }
    var selectedSpot by remember { mutableStateOf<Spot?>(null) }

    fun toggleSaved(id: String) {
        spots = spots.map { spot -> if (spot.id == id) spot.copy(isSaved = !spot.isSaved) else spot }
    }

    val openSpot: (Spot) -> Unit = { spot -> selectedSpot = spot }

    val detailSpot = selectedSpot
    if (detailSpot != null) {
        SpotDetailScreen(
            spot = detailSpot,
            onToggleSaved = ::toggleSaved,
            onBack = { selectedSpot = null },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { CampusBottomNavBar(current = currentTab, onSelect = { currentTab = it }) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())

        when (currentTab) {
            BottomTab.FOR_YOU -> ForYouScreen(
                spots = spots,
                onToggleSaved = ::toggleSaved,
                onOpenSpot = openSpot,
                modifier = contentModifier,
            )
            BottomTab.MAP -> ComingSoonScreen(
                title = "Map",
                icon = Icons.Filled.Map,
                modifier = contentModifier,
            )
            BottomTab.SAVED -> SavedPlacesScreen(
                spots = spots,
                onToggleSaved = ::toggleSaved,
                onOpenSpot = openSpot,
                modifier = contentModifier,
            )
            BottomTab.PROFILE -> ProfileScreen(
                savedCount = spots.count { it.isSaved },
                modifier = contentModifier,
            )
        }
    }
}
