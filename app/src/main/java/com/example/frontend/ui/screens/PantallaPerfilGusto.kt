package com.example.frontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.frontend.model.TasteProfile
import com.example.frontend.ui.components.ChipSeleccionable
import com.example.frontend.ui.components.TarjetaCocina
import com.example.frontend.ui.components.TarjetaPresupuesto
import com.example.frontend.ui.theme.AppColors

@Composable
fun PantallaPerfilGusto(
    perfilInicial: TasteProfile,
    alGuardar: (TasteProfile) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFaculty by remember { mutableStateOf(perfilInicial.faculty) }
    var selectedBudget by remember { mutableStateOf(perfilInicial.budget) }
    var selectedDietary by remember { mutableStateOf(perfilInicial.dietary) }
    var selectedCuisines by remember { mutableStateOf(perfilInicial.cuisines) }
    var selectedSpice by remember { mutableStateOf(perfilInicial.spice) }

    Scaffold(
        modifier = modifier,
        containerColor = AppColors.cream,
        bottomBar = {
            Surface(color = AppColors.cream) {
                Button(
                    onClick = {
                        alGuardar(
                            TasteProfile(
                                faculty = selectedFaculty,
                                budget = selectedBudget,
                                dietary = selectedDietary,
                                cuisines = selectedCuisines,
                                spice = selectedSpice,
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.tomato),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(52.dp),
                ) {
                    Text("Calculate My Taste Matches", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = alVolver) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.espresso,
                    )
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Taste Profile", fontWeight = FontWeight.Bold, color = AppColors.espresso)
                    Text("Step 2 of 3", color = AppColors.muted, style = MaterialTheme.typography.bodySmall)
                }
                Text("66%", color = AppColors.tomato, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (index < 2) AppColors.tomato else AppColors.border,
                                RoundedCornerShape(2.dp),
                            )
                    )
                    if (index != 2) Spacer(Modifier.width(4.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(24.dp), color = AppColors.mintLight) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AppColors.mint,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Verified Student Access • student@uniandes.edu.co",
                        color = AppColors.mint,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "What flavors match your campus rhythm?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.espresso,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "We match you with places real peers in your faculty love — never generic ads.",
                color = AppColors.muted,
            )

            EncabezadoSeccion("Faculty & Major Community", "Select 1")
            Spacer(Modifier.height(10.dp))
            Row {
                ChipSeleccionable("Engineering & Tech", selectedFaculty == "Engineering & Tech") {
                    selectedFaculty = "Engineering & Tech"
                }
                Spacer(Modifier.width(8.dp))
                ChipSeleccionable("Design & Arts", selectedFaculty == "Design & Arts") {
                    selectedFaculty = "Design & Arts"
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                ChipSeleccionable("Medicine", selectedFaculty == "Medicine") { selectedFaculty = "Medicine" }
                Spacer(Modifier.width(8.dp))
                ChipSeleccionable("Business", selectedFaculty == "Business") { selectedFaculty = "Business" }
            }

            EncabezadoSeccion("Lunch Budget Range", "Student tiers")
            Spacer(Modifier.height(10.dp))
            TarjetaPresupuesto(
                "$", "Budget-friendly", "< \$14.000 COP • Daily go-to",
                selectedBudget == "Budget-friendly",
            ) { selectedBudget = "Budget-friendly" }
            Spacer(Modifier.height(10.dp))
            TarjetaPresupuesto(
                "$$", "Moderate", "\$15.000 – \$25.000 COP • Balanced meal",
                selectedBudget == "Moderate",
            ) { selectedBudget = "Moderate" }
            Spacer(Modifier.height(10.dp))
            TarjetaPresupuesto(
                "$$$", "Treat yourself", "> \$25.000 COP • Post-exam reward",
                selectedBudget == "Treat yourself",
            ) { selectedBudget = "Treat yourself" }

            EncabezadoSeccion("Dietary Preferences", "Multi-select")
            Spacer(Modifier.height(10.dp))
            Row {
                ChipSeleccionable("Vegetarian", selectedDietary.contains("Vegetarian")) {
                    selectedDietary = alternar(selectedDietary, "Vegetarian")
                }
                Spacer(Modifier.width(8.dp))
                ChipSeleccionable("Vegan", selectedDietary.contains("Vegan")) {
                    selectedDietary = alternar(selectedDietary, "Vegan")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                ChipSeleccionable("Gluten-Free", selectedDietary.contains("Gluten-Free")) {
                    selectedDietary = alternar(selectedDietary, "Gluten-Free")
                }
                Spacer(Modifier.width(8.dp))
                ChipSeleccionable("Halal", selectedDietary.contains("Halal")) {
                    selectedDietary = alternar(selectedDietary, "Halal")
                }
            }

            EncabezadoSeccion("Favorite Campus Cuisines", "Pick favorites")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                TarjetaCocina(
                    "🍔", "Artisan Burgers", selectedCuisines.contains("Artisan Burgers"),
                    { selectedCuisines = alternar(selectedCuisines, "Artisan Burgers") }, Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                TarjetaCocina(
                    "🥗", "Crepas & Bowls", selectedCuisines.contains("Crepas & Bowls"),
                    { selectedCuisines = alternar(selectedCuisines, "Crepas & Bowls") }, Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                TarjetaCocina(
                    "☕", "Specialty Coffee", selectedCuisines.contains("Specialty Coffee"),
                    { selectedCuisines = alternar(selectedCuisines, "Specialty Coffee") }, Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                TarjetaCocina(
                    "🍜", "Asian Wok", selectedCuisines.contains("Asian Wok"),
                    { selectedCuisines = alternar(selectedCuisines, "Asian Wok") }, Modifier.weight(1f),
                )
            }

            EncabezadoSeccion("Spice Tolerance", "Flavor heat")
            Spacer(Modifier.height(10.dp))
            Row {
                listOf("Mild", "Medium Heat", "Fiery").forEach { level ->
                    ChipSeleccionable(level, selectedSpice == level) { selectedSpice = level }
                    Spacer(Modifier.width(8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EncabezadoSeccion(title: String, hint: String) {
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontWeight = FontWeight.SemiBold, color = AppColors.espresso)
        Text(hint, color = AppColors.muted, style = MaterialTheme.typography.bodySmall)
    }
}

private fun alternar(set: Set<String>, value: String): Set<String> =
    if (set.contains(value)) set - value else set + value
