package com.example.frontend.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Reseñas que el estudiante publico pero que todavia no llegan al backend: sin red, sin sesion
// o porque el servidor fallo. Antes vivian en memoria y se perdian al cerrar la app.
@Entity(tableName = "resenas_pendientes")
data class ResenaPendienteEntidad(
    @PrimaryKey val id: String,  // uuid local, para poder borrar justo esta despues de enviarla
    val spotId: String,
    val estrellas: Int,
    val texto: String,
    val creadaEn: Long,          // millis UTC
)

@Dao
interface ResenasPendientesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun encolar(resena: ResenaPendienteEntidad)

    // La mas vieja primero: se envian en el orden en que se escribieron.
    @Query("SELECT * FROM resenas_pendientes ORDER BY creadaEn")
    suspend fun todas(): List<ResenaPendienteEntidad>

    // El ViewModel la observa: la reseña aparece en el detalle apenas se publica y desaparece de la
    // cola cuando el backend la acepta, sin tener que volver a consultar.
    @Query("SELECT * FROM resenas_pendientes ORDER BY creadaEn DESC")
    fun observar(): Flow<List<ResenaPendienteEntidad>>

    @Query("DELETE FROM resenas_pendientes WHERE id = :id")
    suspend fun borrar(id: String)
}
