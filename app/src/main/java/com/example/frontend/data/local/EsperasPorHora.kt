package com.example.frontend.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

// BQ8 "Best time to go" (Sprint 4): la espera promedio de cada restaurante en cada hora del dia, tal como
// la mando el backend la ultima vez. Se guarda fila por fila (restaurante + hora) y no como un JSON entero
// porque sin red la app vuelve a calcular la mejor hora, la peor y la "proxima hora tranquila" desde aqui,
// y porque la pantalla tiene dos ventanas (7 y 28 dias) que se guardan y se reemplazan por separado.
// spotId = "_campus" es el promedio de todos los restaurantes juntos.
@Entity(tableName = "esperas_por_hora", primaryKeys = ["ventanaDias", "spotId", "hora"])
data class EsperaPorHoraEntidad(
    val ventanaDias: Int,
    val spotId: String,
    val nombre: String,
    val emoji: String,
    val hora: Int,
    val promedioMinutos: Double,
    val reportes: Int,
    val guardadoEn: Long,
)

@Dao
interface EsperasPorHoraDao {
    @Query("SELECT * FROM esperas_por_hora WHERE ventanaDias = :ventanaDias ORDER BY spotId, hora")
    suspend fun filas(ventanaDias: Int): List<EsperaPorHoraEntidad>

    @Query("DELETE FROM esperas_por_hora WHERE ventanaDias = :ventanaDias")
    suspend fun borrarVentana(ventanaDias: Int)

    @Insert
    suspend fun insertar(filas: List<EsperaPorHoraEntidad>)

    // En una transaccion: si la app muere a mitad, queda la copia anterior completa y no una mezcla.
    @Transaction
    suspend fun reemplazar(ventanaDias: Int, filas: List<EsperaPorHoraEntidad>) {
        borrarVentana(ventanaDias)
        insertar(filas)
    }
}
