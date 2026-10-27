package com.example.frontend.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CatalogoDao {
    @Query("SELECT * FROM sitios_guardados ORDER BY posicion")
    suspend fun sitios(): List<SitioGuardadoEntidad>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSitios(sitios: List<SitioGuardadoEntidad>)

    @Query("DELETE FROM sitios_guardados")
    suspend fun borrarSitios()

    // Reemplazo completo: si un restaurante salio del catalogo, tambien sale de la copia local.
    @Transaction
    suspend fun reemplazarCatalogo(sitios: List<SitioGuardadoEntidad>) {
        borrarSitios()
        insertarSitios(sitios)
    }

    @Query("SELECT * FROM detalles_guardados WHERE id = :id")
    suspend fun detalle(id: String): DetalleGuardadoEntidad?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarDetalle(detalle: DetalleGuardadoEntidad)

    @Query("SELECT * FROM tiempos_espera")
    suspend fun tiemposEspera(): List<TiempoEsperaEntidad>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTiemposEspera(tiempos: List<TiempoEsperaEntidad>)
}

@Dao
interface PendientesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun encolarEvento(evento: EventoPendienteEntidad)

    @Query("SELECT * FROM eventos_pendientes ORDER BY creadoEn LIMIT :limite")
    suspend fun eventos(limite: Int): List<EventoPendienteEntidad>

    @Query("DELETE FROM eventos_pendientes WHERE eventId IN (:ids)")
    suspend fun borrarEventos(ids: List<String>)

    // Tope para que un celular que pasa semanas sin red no llene el almacenamiento.
    @Query("DELETE FROM eventos_pendientes WHERE eventId NOT IN (SELECT eventId FROM eventos_pendientes ORDER BY creadoEn DESC LIMIT :maximo)")
    suspend fun recortarEventos(maximo: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun encolarReporte(reporte: ReporteEsperaPendienteEntidad)

    @Query("SELECT * FROM reportes_espera_pendientes ORDER BY reportadoEn")
    suspend fun reportes(): List<ReporteEsperaPendienteEntidad>

    @Query("DELETE FROM reportes_espera_pendientes WHERE reportId = :reportId")
    suspend fun borrarReporte(reportId: String)
}
