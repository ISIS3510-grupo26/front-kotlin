package com.example.frontend.data

import android.os.Trace

// Marca un bloque en el System Trace (Perfetto / Android Studio Profiler) con el prefijo "CB:".
// Asi el antes y el despues de las micro-optimizaciones se ven en el trace con su duracion y su hilo.
// Si nadie esta grabando un trace, Trace.beginSection casi no cuesta nada.
inline fun <T> medir(seccion: String, bloque: () -> T): T {
    Trace.beginSection("CB:$seccion")
    try {
        return bloque()
    } finally {
        Trace.endSection()
    }
}
