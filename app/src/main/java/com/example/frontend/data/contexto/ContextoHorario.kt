package com.example.frontend.data.contexto

import java.util.Calendar
import java.util.TimeZone

// Contexto del dispositivo que usa la feature "Trending now": la hora local y la zona horaria
// del celular. Con esto la app pide al backend la franja horaria que corresponde AHORA
// (desayuno, almuerzo, ...) en vez de un ranking global. Es lo que hace a la feature context-aware.
interface ContextoHorario {
    fun horaLocal(): Int
    fun desfaseZonaHorariaMinutos(): Int

    object Dispositivo : ContextoHorario {
        override fun horaLocal(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        override fun desfaseZonaHorariaMinutos(): Int =
            TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
    }
}
