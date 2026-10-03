package com.example.frontend.data

import com.example.frontend.data.contexto.ContextoHorario
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.SearchesByWeekdayDto
import com.example.frontend.model.BusquedasPorDiaSemana
import com.example.frontend.model.ConteoBusquedaDia

class BusquedasPorDiaRepository(
    private val api: CampusBitesApi,
    private val contexto: ContextoHorario = ContextoHorario.Dispositivo,
) {
    suspend fun cargar(days: Int = 28): BusquedasPorDiaSemana {
        val response = api.busquedasPorDia(
            tzOffsetMinutes = contexto.desfaseZonaHorariaMinutos(),
            days = days,
        )
        return response.toModel()
    }
}

private fun SearchesByWeekdayDto.toModel() = BusquedasPorDiaSemana(
    days = days,
    totalSearches = totalSearches,
    dias = byDay.map { day ->
        ConteoBusquedaDia(
            dayOfWeek = day.dayOfWeek,
            dayName = day.dayName,
            searches = day.searches,
        )
    },
)