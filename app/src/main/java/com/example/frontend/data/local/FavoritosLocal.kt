package com.example.frontend.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// Cambios de favoritos (guardar o quitar) que todavia no llegan al backend. La llave es usuario + restaurante:
// si el estudiante toca el corazon tres veces sin red, queda una sola fila con lo ultimo que eligio,
// y al volver la conexion se envia una peticion, no tres.
@Entity(tableName = "favoritos_pendientes", primaryKeys = ["userId", "spotId"])
data class FavoritoPendienteEntidad(
    val userId: String,
    val spotId: String,
    val guardar: Boolean,
    val eventId: String,     // id del evento favorite_added (BQ7): si el envio se repite, no cuenta doble
    val cambiadoEn: Long,    // hora del toque: el evento se reporta con esa hora, no con la del envio
)

// BQ5: la ultima respuesta buena de "favoritos abiertos y cerca", con el cierre de cada uno. Sin red se
// vuelve a filtrar con la hora y la ubicacion de ahora (ReglasAbiertosCerca.filtrarSinRed).
@Entity(tableName = "abiertos_cerca")
data class AbiertoCercaEntidad(
    @PrimaryKey val spotId: String,
    val userId: String,
    val nombre: String,
    val emoji: String,
    val lat: Double,
    val lng: Double,
    val metros: Int,
    val minutos: Int,
    val cierraA: String,
    val cierraEnMillis: Long,
    val guardadoEn: Long,
)

@Dao
interface FavoritosDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarCambio(cambio: FavoritoPendienteEntidad)

    @Query("SELECT * FROM favoritos_pendientes WHERE userId = :userId ORDER BY cambiadoEn")
    suspend fun pendientes(userId: String): List<FavoritoPendienteEntidad>

    // La UI muestra "N changes waiting to sync" y lo actualiza sola cuando la cola cambia.
    @Query("SELECT COUNT(*) FROM favoritos_pendientes WHERE userId = :userId")
    fun contarPendientes(userId: String): Flow<Int>

    // Se borra solo si sigue siendo el mismo cambio que se envio: si mientras viajaba la peticion el
    // estudiante volvio a tocar el corazon, la fila nueva (otro eventId) se queda para el siguiente envio.
    @Query("DELETE FROM favoritos_pendientes WHERE userId = :userId AND spotId = :spotId AND eventId = :eventId")
    suspend fun borrarCambio(userId: String, spotId: String, eventId: String)

    @Query("SELECT * FROM abiertos_cerca WHERE userId = :userId")
    suspend fun abiertosGuardados(userId: String): List<AbiertoCercaEntidad>

    @Query("DELETE FROM abiertos_cerca")
    suspend fun borrarAbiertos()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAbiertos(abiertos: List<AbiertoCercaEntidad>)

    // Reemplazo completo en una transaccion: nunca queda una mezcla de la respuesta vieja y la nueva.
    @Transaction
    suspend fun reemplazarAbiertos(abiertos: List<AbiertoCercaEntidad>) {
        borrarAbiertos()
        insertarAbiertos(abiertos)
    }
}
