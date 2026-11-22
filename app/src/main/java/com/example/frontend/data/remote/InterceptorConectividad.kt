package com.example.frontend.data.remote

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

// Le cuenta al MonitorConectividad lo que pasa con cada peticion real: si llega cualquier respuesta
// (aunque sea un 404 o un 500) hay conexion; si falla por red (sin internet, timeout, servidor caido)
// se marca sin conexion. Asi el aviso "No internet" sale aunque el sistema crea que el wifi funciona.
object InterceptorConectividad : Interceptor {
    @Volatile var alFallar: (() -> Unit)? = null
    @Volatile var alResponder: (() -> Unit)? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val respuesta = try {
            chain.proceed(chain.request())
        } catch (e: IOException) {
            // Una peticion cancelada (el usuario salio de la pantalla) no dice nada de la red.
            if (!chain.call().isCanceled()) alFallar?.invoke()
            throw e
        }
        alResponder?.invoke()
        return respuesta
    }
}
