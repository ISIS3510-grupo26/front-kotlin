package com.example.frontend.data.sesion

import com.example.frontend.data.desdeIso8601
import com.example.frontend.data.remote.AuthTokenDto
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.CredencialesDto
import com.example.frontend.data.remote.TokenActual
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

// Login basico contra /api/v1/auth del backend. La recuperacion de contrasena no va aqui (la hace Lucas).
class SesionRepository(
    private val api: CampusBitesApi,
    private val almacen: AlmacenSesion,
) {
    suspend fun iniciarSesion(email: String, password: String): Sesion =
        abrir(api.iniciarSesion(CredencialesDto(email.trim(), password)))

    suspend fun registrarse(email: String, password: String): Sesion =
        abrir(api.registrarse(CredencialesDto(email.trim(), password)))

    suspend fun cerrarSesion() {
        TokenActual.valor = null
        almacen.borrar()
    }

    // Al abrir la app: la sesion guardada se usa de inmediato, aunque no haya red (conectividad eventual).
    // Si hay red se confirma con /auth/me; si el token ya no sirve, el interceptor la cierra (401).
    suspend fun restaurar(): Sesion? {
        val guardada = almacen.actual() ?: return null
        if (guardada.vencida()) {
            cerrarSesion()
            return null
        }
        TokenActual.valor = guardada.token
        try {
            api.yo()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // Sin red: se mantiene la sesion. Se verificara en la siguiente peticion que llegue al backend.
        } catch (e: HttpException) {
            if (e.code() == 401) return null
        }
        return almacen.actual()
    }

    private suspend fun abrir(dto: AuthTokenDto): Sesion {
        val expira = desdeIso8601(dto.expiresAt) ?: (System.currentTimeMillis() + dto.expiresIn * 1000)
        val sesion = Sesion(dto.userId, dto.email, dto.accessToken, expira)
        almacen.guardar(sesion)
        TokenActual.valor = sesion.token
        return sesion
    }
}

// Mensaje que ve el usuario segun por que fallo el login o el registro.
fun mensajeErrorSesion(e: Throwable, registrando: Boolean): String = when {
    e is IOException -> "You're offline. Signing in needs a connection; you can keep browsing meanwhile."
    e is HttpException && e.code() == 401 -> "Wrong email or password."
    e is HttpException && e.code() == 409 -> "That email already has an account. Try signing in."
    e is HttpException && e.code() == 422 ->
        if (registrando) "Use a valid email and a password of at least 8 characters." else "Check the email format."
    else -> "Something went wrong. Try again."
}
