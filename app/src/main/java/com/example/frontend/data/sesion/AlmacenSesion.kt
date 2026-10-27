package com.example.frontend.data.sesion

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class Sesion(
    val userId: String,
    val email: String,
    val token: String,
    val expiraEn: Long,   // millis UTC; el backend da 7 dias de vigencia
) {
    fun vencida(ahora: Long = System.currentTimeMillis()) = ahora >= expiraEn
}

private val Context.datosSesion by preferencesDataStore(name = "sesion")

// La sesion se guarda en DataStore (clave-valor): son 4 datos sueltos que se leen al abrir la app,
// no una tabla. Asi el usuario sigue logueado aunque cierre la app o no tenga red.
class AlmacenSesion(private val context: Context) {

    private object Claves {
        val userId = stringPreferencesKey("user_id")
        val email = stringPreferencesKey("email")
        val token = stringPreferencesKey("token")
        val expiraEn = longPreferencesKey("expira_en")
    }

    val sesion: Flow<Sesion?> = context.datosSesion.data.map { datos ->
        val userId = datos[Claves.userId] ?: return@map null
        val token = datos[Claves.token] ?: return@map null
        Sesion(userId, datos[Claves.email].orEmpty(), token, datos[Claves.expiraEn] ?: 0L)
    }

    suspend fun actual(): Sesion? = sesion.first()

    suspend fun guardar(sesion: Sesion) {
        context.datosSesion.edit { datos ->
            datos[Claves.userId] = sesion.userId
            datos[Claves.email] = sesion.email
            datos[Claves.token] = sesion.token
            datos[Claves.expiraEn] = sesion.expiraEn
        }
    }

    suspend fun borrar() {
        context.datosSesion.edit { it.clear() }
    }
}
