package com.example.frontend.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Tablas de la base local. El catalogo y el detalle se guardan como el JSON que mando el backend:
// asi la copia local es exactamente la ultima respuesta buena y no hay que mantener otro esquema.

@Entity(tableName = "sitios_guardados")
data class SitioGuardadoEntidad(
    @PrimaryKey val id: String,
    val posicion: Int,          // orden del catalogo
    val json: String,
    val guardadoEn: Long,
)

@Entity(tableName = "detalles_guardados")
data class DetalleGuardadoEntidad(
    @PrimaryKey val id: String,
    val json: String,
    val guardadoEn: Long,
)

// BQ4: ultimo tiempo de espera de cada restaurante, para mostrarlo aunque no haya red.
@Entity(tableName = "tiempos_espera")
data class TiempoEsperaEntidad(
    @PrimaryKey val spotId: String,
    val minutos: Int?,
    val reportadoEn: Long?,     // millis UTC
    val reportesRecientes: Int,
)

// Eventos de telemetria que todavia no llegan al backend. Antes vivian en memoria y se
// perdian si se cerraba la app; ahora sobreviven hasta que haya conexion.
@Entity(tableName = "eventos_pendientes")
data class EventoPendienteEntidad(
    @PrimaryKey val eventId: String,
    val json: String,
    val creadoEn: Long,
)

// Reportes de espera hechos sin conexion (o que fallaron al enviarse).
@Entity(tableName = "reportes_espera_pendientes")
data class ReporteEsperaPendienteEntidad(
    @PrimaryKey val reportId: String,
    val spotId: String,
    val minutos: Int,
    val reportadoEn: Long,
)
