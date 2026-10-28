package com.example.frontend.data

import com.example.frontend.data.local.Borrador
import com.example.frontend.data.local.BorradorResena
import com.example.frontend.data.local.ResenaPendienteEntidad
import com.example.frontend.data.local.ResenasPendientesDao
import java.util.UUID
import kotlinx.coroutines.flow.Flow

// Todo lo de escribir una reseña: el borrador mientras se escribe (archivo) y la cola de envio (Room).
// Publicar nunca depende de la red: la reseña entra a la cola y SincronizadorPendientes la manda
// cuando haya conexion y sesion, igual que los reportes de espera.
class ResenasRepository(
    private val cola: ResenasPendientesDao = Dependencias.baseDatos.resenasPendientesDao(),
    private val borradores: BorradorResena = BorradorResena(),
    private val alEncolar: () -> Unit = Dependencias::programarSincronizacion,
) {
    val pendientes: Flow<List<ResenaPendienteEntidad>> = cola.observar()

    suspend fun leerBorrador(spotId: String): Borrador? = borradores.leer(spotId)

    // Un borrador vacio no sirve de nada: se borra en vez de guardarlo.
    suspend fun guardarBorrador(spotId: String, estrellas: Int, texto: String) {
        if (estrellas == 0 && texto.isBlank()) {
            borradores.borrar(spotId)
        } else {
            borradores.guardar(Borrador(spotId, estrellas, texto, System.currentTimeMillis()))
        }
    }

    suspend fun publicar(spotId: String, estrellas: Int, texto: String) {
        cola.encolar(ResenaPendienteEntidad(UUID.randomUUID().toString(), spotId, estrellas, texto, System.currentTimeMillis()))
        // Ya esta a salvo en Room: el borrador sobra.
        borradores.borrar(spotId)
        alEncolar()
    }
}
