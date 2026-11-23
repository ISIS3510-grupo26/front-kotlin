package com.example.frontend.data

import com.example.frontend.data.gusto.AlmacenGusto
import com.example.frontend.data.local.FavoritoPendienteEntidad
import com.example.frontend.data.local.FavoritosDao
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.telemetry.TelemetriaCargas
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

// Sprint 4: los favoritos dejan de vivir solo en el celular y se guardan en la cuenta, igual que en la app
// Flutter. El backend los necesita para la BQ5 (cuales estan abiertos y cerca) y la BQ7 (cuantos usuarios
// guardan favoritos cada mes). En el celular siguen en DataStore, asi que el corazon funciona sin red.
class FavoritosRepository(
    private val api: CampusBitesApi,
    private val dao: FavoritosDao,
    private val almacenGusto: AlmacenGusto,
    private val telemetria: TelemetriaCargas,
    private val alEncolar: () -> Unit,
) {
    // El ViewModel (al abrir "Open near you") y el sincronizador (WorkManager o al volver la red) pueden
    // enviar la cola al mismo tiempo. Con el Mutex el segundo espera y encuentra la cola ya vacia.
    private val envio = Mutex()

    fun contarPendientes(userId: String): Flow<Int> = dao.contarPendientes(userId)

    // El toque del corazon: se guarda primero en Room (nunca se pierde) y despues se pide el envio.
    // Con red sale de inmediato; sin red lo manda WorkManager cuando vuelva la conexion.
    suspend fun cambiar(userId: String, spotId: String, guardar: Boolean) {
        dao.guardarCambio(FavoritoPendienteEntidad(userId, spotId, guardar, UUID.randomUUID().toString(), System.currentTimeMillis()))
        alEncolar()
    }

    // Manda la cola en orden. Devuelve false si algo quedo pendiente (sin red o servidor caido): asi
    // WorkManager sabe que tiene que reintentar.
    suspend fun enviarPendientes(userId: String): Boolean = envio.withLock {
        for (cambio in dao.pendientes(userId)) {
            try {
                val respuesta = if (cambio.guardar) api.guardarFavorito(userId, cambio.spotId)
                else api.quitarFavorito(userId, cambio.spotId)
                if (!respuesta.isSuccessful) throw HttpException(respuesta)
                // BQ7: el evento sale solo cuando el servidor ya tiene el favorito.
                if (cambio.guardar) telemetria.registrarFavoritoGuardado(userId, cambio.spotId, cambio.eventId, cambio.cambiadoEn)
                dao.borrarCambio(userId, cambio.spotId, cambio.eventId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                // 5xx y 401 (sesion vencida) se arreglan solos: se reintenta despues. Otro 4xx (el restaurante ya
                // no existe, usuario de otra sesion) da lo mismo al reenviarlo, asi que sale de la cola.
                if (e.code() >= 500 || e.code() == 401) return@withLock false
                dao.borrarCambio(userId, cambio.spotId, cambio.eventId)
            } catch (e: IOException) {
                return@withLock false   // sigue sin red: los demas esperan su turno
            }
        }
        true
    }

    // Al entrar y al volver la red. Devuelve el conjunto de favoritos que debe mostrar el celular:
    // lo del servidor mas los cambios que todavia no llegan. Lanza IOException si no hay red.
    // `enPantalla` son los corazones que el estudiante esta viendo (los de DataStore o, si nunca toco
    // ninguno, los del diseño): si es su primera vez en este celular, esos son los que se suben.
    suspend fun sincronizar(userId: String, enPantalla: Set<String>): Set<String> {
        var servidor = idsDelServidor(userId)
        if (!almacenGusto.yaSubioFavoritos(userId)) {
            // Primera vez de este usuario en este celular: lo que guardo antes de tener cuenta se sube.
            val locales = enPantalla
            val yaEnCola = dao.pendientes(userId).map { it.spotId }.toSet()
            for (spotId in locales - servidor - yaEnCola) {
                dao.guardarCambio(FavoritoPendienteEntidad(userId, spotId, true, UUID.randomUUID().toString(), System.currentTimeMillis()))
            }
            almacenGusto.marcarFavoritosSubidos(userId)
        }
        if (dao.pendientes(userId).isNotEmpty() && enviarPendientes(userId)) servidor = idsDelServidor(userId)
        return aplicarPendientes(servidor, dao.pendientes(userId))
    }

    private suspend fun idsDelServidor(userId: String): Set<String> = api.favoritos(userId).map { it.id }.toSet()
}

// Lo que el estudiante hizo sin red gana sobre lo que dice el servidor: es mas nuevo.
fun aplicarPendientes(servidor: Set<String>, pendientes: List<FavoritoPendienteEntidad>): Set<String> =
    pendientes.fold(servidor) { ids, cambio -> if (cambio.guardar) ids + cambio.spotId else ids - cambio.spotId }
