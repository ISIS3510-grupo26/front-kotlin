package com.example.frontend.model

// BQ4: ultimo tiempo de espera reportado para un restaurante.
data class TiempoEspera(
    val spotId: String,
    val minutos: Int?,               // null = nadie ha reportado todavia
    val reportadoEn: Long?,          // millis UTC
    val reportesRecientes: Int,
    val pendienteDeEnvio: Boolean = false, // lo reporto este celular sin conexion y aun no llega al backend
) {
    fun minutosAtras(ahora: Long = System.currentTimeMillis()): Int? =
        reportadoEn?.let { ((ahora - it) / 60_000L).toInt().coerceAtLeast(0) }

    // Despues de 2 horas un tiempo de espera ya no dice nada de la fila actual.
    fun esReciente(ahora: Long = System.currentTimeMillis()): Boolean =
        minutosAtras(ahora)?.let { it <= MINUTOS_RECIENTE } ?: false

    companion object {
        const val MINUTOS_RECIENTE = 120
    }
}

fun textoHaceCuanto(minutos: Int): String = when {
    minutos < 1 -> "just now"
    minutos < 60 -> "$minutos min ago"
    minutos < 24 * 60 -> "${minutos / 60} h ago"
    else -> "${minutos / (24 * 60)} d ago"
}
