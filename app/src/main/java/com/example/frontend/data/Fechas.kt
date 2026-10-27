package com.example.frontend.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// java.time requiere API 26; minSdk es 24.
private fun formatoUtc(patron: String) =
    SimpleDateFormat(patron, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

fun aIso8601(millis: Long = System.currentTimeMillis()): String =
    formatoUtc("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(Date(millis))

// El backend siempre responde en UTC ("...Z" o "+00:00"); los segundos bastan para mostrar "hace X min".
fun desdeIso8601(texto: String?): Long? {
    if (texto == null || texto.length < 19) return null
    return try {
        formatoUtc("yyyy-MM-dd'T'HH:mm:ss").parse(texto.substring(0, 19))?.time
    } catch (_: Exception) {
        null
    }
}
