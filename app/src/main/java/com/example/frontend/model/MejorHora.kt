package com.example.frontend.model

// BQ8 (tipo 2, Sprint 4): "At what time of the day is the reported waiting time at each restaurant the shortest?"
// Se calcula con los mismos reportes de espera de la BQ4.

data class EsperaHora(val hora: Int, val promedioMinutos: Double, val reportes: Int)

data class MejorHoraSitio(
    val spotId: String,
    val nombre: String,
    val emoji: String,
    val reportes: Int,
    val porHora: List<EsperaHora>,   // solo las horas con reportes, ordenadas
    val mejor: EsperaHora?,          // menor espera promedio (con suficientes reportes)
    val peor: EsperaHora?,           // mayor espera promedio
    val proximaTranquila: EsperaHora?, // desde la hora actual: la primera hora cerca de la mejor (hoy)
)

data class ResumenMejorHora(
    val campus: MejorHoraSitio?,     // todos los restaurantes juntos
    val sitios: List<MejorHoraSitio>,
    val totalReportes: Int,
)

// Las reglas viven aqui y no en la pantalla ni en el backend, porque sin red la app las vuelve a correr
// sobre las filas guardadas en Room. Son las mismas del backend (wait_times_service.best_time_to_go).
// Es trabajo de CPU puro: el ViewModel lo llama en Dispatchers.Default.
object ReglasMejorHora {
    const val CAMPUS = "_campus"
    const val MIN_REPORTES = 2
    // Una hora "tranquila" es la mejor o una que espera a lo sumo 5 min mas que la mejor.
    const val HOLGURA_MINUTOS = 5.0

    data class Fila(val spotId: String, val nombre: String, val emoji: String, val hora: Int, val promedio: Double, val reportes: Int)

    fun armar(filas: List<Fila>, horaActual: Int, minReportes: Int = MIN_REPORTES): ResumenMejorHora {
        val porSitio = LinkedHashMap<String, MutableList<Fila>>()
        for (fila in filas) porSitio.getOrPut(fila.spotId) { ArrayList() }.add(fila)

        var campus: MejorHoraSitio? = null
        val sitios = ArrayList<MejorHoraSitio>(porSitio.size)
        for ((spotId, propias) in porSitio) {
            val sitio = resumir(propias, horaActual, minReportes)
            if (spotId == CAMPUS) campus = sitio else sitios.add(sitio)
        }
        // Primero los que tienen recomendacion, de menor a mayor espera en su mejor hora.
        sitios.sortWith(compareBy<MejorHoraSitio> { it.mejor == null }.thenBy { it.mejor?.promedioMinutos ?: Double.MAX_VALUE })
        return ResumenMejorHora(campus, sitios, campus?.reportes ?: sitios.sumOf { it.reportes })
    }

    private fun resumir(filas: List<Fila>, horaActual: Int, minReportes: Int): MejorHoraSitio {
        val porHora = filas.map { EsperaHora(it.hora, it.promedio, it.reportes) }.sortedBy { it.hora }
        val confiables = porHora.filter { it.reportes >= minReportes }
        // Empates: gana la hora con mas reportes y despues la mas temprana (igual que el backend).
        val mejor = confiables.minWithOrNull(compareBy<EsperaHora> { it.promedioMinutos }.thenByDescending { it.reportes }.thenBy { it.hora })
        val peor = confiables.maxWithOrNull(compareBy<EsperaHora> { it.promedioMinutos }.thenBy { it.reportes }.thenByDescending { it.hora })
        val proxima = mejor?.let { m ->
            confiables.firstOrNull { it.hora >= horaActual && it.promedioMinutos <= m.promedioMinutos + HOLGURA_MINUTOS }
        }
        val primera = filas.first()
        return MejorHoraSitio(primera.spotId, primera.nombre, primera.emoji, porHora.sumOf { it.reportes }, porHora, mejor, peor, proxima)
    }

    fun textoHora(hora: Int): String {
        val h = if (hora % 12 == 0) 12 else hora % 12
        return "$h ${if (hora < 12) "am" else "pm"}"
    }
}
