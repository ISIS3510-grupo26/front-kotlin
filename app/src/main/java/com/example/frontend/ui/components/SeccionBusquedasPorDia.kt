package com.example.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.model.BusquedasPorDiaSemana
import com.example.frontend.ui.theme.AppColors
import kotlinx.coroutines.CancellationException

private sealed interface EstadoBusquedasDia {
    data object Cargando : EstadoBusquedasDia
    data object Error : EstadoBusquedasDia
    data class Listo(val reporte: BusquedasPorDiaSemana) : EstadoBusquedasDia
}

@Composable
fun SeccionBusquedasPorDia(
    cargar: suspend () -> BusquedasPorDiaSemana,
    modifier: Modifier = Modifier,
) {
    var estado by remember { mutableStateOf<EstadoBusquedasDia>(EstadoBusquedasDia.Cargando) }
    var intento by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(intento) {
        estado = EstadoBusquedasDia.Cargando
        estado = try {
            EstadoBusquedasDia.Listo(cargar())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            EstadoBusquedasDia.Error
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.card, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Restaurant searches by weekday",
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.espresso,
        )

        when (val actual = estado) {
            EstadoBusquedasDia.Cargando -> LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.tomato,
                trackColor = AppColors.tomatoLight,
            )

            EstadoBusquedasDia.Error -> {
                Text("Couldn't load weekday searches. Check your connection.", color = AppColors.muted, fontSize = 12.sp)
                Button(
                    onClick = { intento++ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.tomato,
                        contentColor = Color.White,
                    ),
                ) { Text("Retry", fontWeight = FontWeight.ExtraBold) }
            }

            is EstadoBusquedasDia.Listo -> {
                val reporte = actual.reporte
                Text(
                    "${reporte.totalSearches} restaurant selections from search results in the last ${reporte.days} days",
                    fontSize = 12.sp,
                    color = AppColors.muted,
                )

                if (reporte.totalSearches == 0) {
                    Text("No search selections recorded yet.", color = AppColors.muted, fontSize = 12.sp)
                } else {
                    val maximo = reporte.dias.maxOfOrNull { it.searches }?.coerceAtLeast(1) ?: 1
                    reporte.dias.forEach { dia ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                dia.dayName.take(3),
                                modifier = Modifier.width(34.dp),
                                fontSize = 12.sp,
                                color = AppColors.espresso,
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .background(AppColors.surface, RoundedCornerShape(999.dp)),
                            ) {
                                if (dia.searches > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(dia.searches.toFloat() / maximo)
                                            .height(8.dp)
                                            .background(AppColors.mint, RoundedCornerShape(999.dp)),
                                    )
                                }
                            }
                            Text(
                                dia.searches.toString(),
                                modifier = Modifier.width(36.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.espresso,
                            )
                        }
                    }
                    val highest = reporte.dias.maxOfOrNull { it.searches } ?: 0
                    val busiestDays = reporte.dias
                        .filter { it.searches == highest }
                        .joinToString { it.dayName }
                    Text("Highest: $busiestDays ($highest)", fontSize = 12.sp, color = AppColors.tomato)
                }
            }
        }
    }
}