package com.example.frontend.data.remote

import okhttp3.Interceptor
import okhttp3.Response

// Token de la sesion abierta, en memoria. Lo actualiza SesionRepository al iniciar o cerrar sesion.
object TokenActual {
    @Volatile var valor: String? = null

    // Lo asigna Dependencias: si el backend dice 401, la sesion guardada ya no sirve y se borra.
    @Volatile var alExpirar: (() -> Unit)? = null
}

// Agrega "Authorization: Bearer <token>" a todas las peticiones en un solo lugar: los repositorios
// no saben nada de autenticacion. Un 401 de cualquier endpoint cierra la sesion aqui mismo.
object InterceptorSesion : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenActual.valor
        val pedido = chain.request()
        val esLogin = pedido.url.encodedPath.contains("/auth/login") || pedido.url.encodedPath.contains("/auth/signup")
        if (token == null || esLogin) return chain.proceed(pedido)

        val respuesta = chain.proceed(pedido.newBuilder().header("Authorization", "Bearer $token").build())
        if (respuesta.code == 401 && TokenActual.valor == token) {
            TokenActual.valor = null
            TokenActual.alExpirar?.invoke()
        }
        return respuesta
    }
}
