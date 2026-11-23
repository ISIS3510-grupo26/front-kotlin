package com.example.frontend.model

import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// BQ5 (tipo 2): "Which restaurants in the user's favorite list are open now and within a 15 minute walk?".
// Un favorito abierto y cerca, como lo muestra la pantalla "Open near you".
data class AbiertoCerca(
    val spotId: String,
    val nombre: String,
    val emoji: String,
    val lat: Double,
    val lng: Double,
    val metros: Int,
    val minutosCaminando: Int,
    val cierraA: String,          // 'HH:MM' hora local, tal cual la manda el backend
    val cierraEnMillis: Long,     // el mismo cierre como instante: sin red sirve para saber si sigue abierto
)

object ReglasAbiertosCerca {
    const val MAX_MINUTOS_CAMINANDO = 15
    // Las mismas constantes que usa el backend (favorites_service.py), para que la cuenta offline
    // de el mismo resultado que la del servidor.
    const val VELOCIDAD_M_POR_MIN = 80.0
    private const val RADIO_TIERRA_M = 6_371_000.0

    // Distancia en linea recta (Haversine) entre dos puntos en grados.
    fun metrosEntre(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = p2 - p1
        val dl = Math.toRadians(lng2 - lng1)
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * RADIO_TIERRA_M * asin(sqrt(a))
    }

    fun minutosCaminando(metros: Double): Int = ceil(metros / VELOCIDAD_M_POR_MIN).toInt().coerceAtLeast(1)

    // 'HH:MM' de cierre -> instante en millis. Si esa hora ya paso hoy, la franja cruza la medianoche y
    // cierra manana (la del viernes 17:00-02:00 cierra el sabado a las 02:00).
    fun cierreEnMillis(cierraA: String, ahoraMillis: Long, desfaseMinutos: Int): Long {
        val partes = cierraA.split(":")
        val minutoCierre = (partes.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (partes.getOrNull(1)?.toIntOrNull() ?: 0)
        val minutoDia = 24 * 60
        val minutoAhora = Math.floorMod(ahoraMillis / 60_000L + desfaseMinutos, minutoDia.toLong()).toInt()
        var faltan = minutoCierre - minutoAhora
        if (faltan <= 0) faltan += minutoDia
        // Se redondea al minuto exacto del cierre (sin los segundos de "ahora").
        return (ahoraMillis / 60_000L) * 60_000L + faltan * 60_000L
    }

    // Conectividad eventual: sin red se rehace la BQ5 con la ultima respuesta guardada. El cierre de cada
    // favorito no cambia, asi que se puede saber cuales siguen abiertos; la distancia se recalcula con la
    // ubicacion de ahora (el GPS funciona sin internet). Lo que abrio despues de guardar no aparece:
    // por eso la pantalla dice de cuando es la copia.
    fun filtrarSinRed(
        guardados: List<AbiertoCerca>,
        favoritos: Set<String>,
        lat: Double,
        lng: Double,
        ahoraMillis: Long,
    ): List<AbiertoCerca> = guardados
        .asSequence()
        .filter { it.spotId in favoritos && it.cierraEnMillis > ahoraMillis }
        .map { guardado ->
            val metros = metrosEntre(lat, lng, guardado.lat, guardado.lng)
            guardado.copy(metros = metros.toInt(), minutosCaminando = minutosCaminando(metros))
        }
        .filter { it.minutosCaminando <= MAX_MINUTOS_CAMINANDO }
        .sortedWith(compareBy<AbiertoCerca> { it.minutosCaminando }.thenBy { it.spotId })
        .toList()
}

// "Closes in 1 h 10 min" / "Closes in 25 min".
fun textoCierraEn(cierraEnMillis: Long, ahoraMillis: Long): String {
    val minutos = ((cierraEnMillis - ahoraMillis) / 60_000L).toInt().coerceAtLeast(0)
    return when {
        minutos < 60 -> "closes in $minutos min"
        minutos % 60 == 0 -> "closes in ${minutos / 60} h"
        else -> "closes in ${minutos / 60} h ${minutos % 60} min"
    }
}
