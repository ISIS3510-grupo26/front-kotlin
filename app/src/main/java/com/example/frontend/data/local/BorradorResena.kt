package com.example.frontend.data.local

import com.example.frontend.data.Dependencias
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Borrador(val spotId: String, val estrellas: Int, val texto: String, val guardadoEn: Long)

// Borrador de la reseña en un archivo, uno por restaurante. Si el estudiante sale de la pantalla,
// se queda sin bateria o el sistema mata la app a mitad de escribir, al volver encuentra su texto.
// Es un archivo y no Room porque es un solo documento que se reescribe completo cada vez.
class BorradorResena(carpeta: File = Dependencias.carpetaArchivos) {

    private val gson = Gson()
    private val directorio = File(carpeta, "borradores_resena")

    suspend fun leer(spotId: String): Borrador? = withContext(Dispatchers.IO) {
        val archivo = archivo(spotId)
        if (!archivo.exists()) return@withContext null
        try {
            gson.fromJson(archivo.readText(), Borrador::class.java)
        } catch (_: Exception) {
            null // un archivo a medio escribir no puede tumbar la pantalla
        }
    }

    // Se escribe en un temporal y luego se renombra: si la app muere a mitad, queda el borrador anterior entero.
    suspend fun guardar(borrador: Borrador) = withContext(Dispatchers.IO) {
        directorio.mkdirs()
        val temporal = File(directorio, "${nombre(borrador.spotId)}.tmp")
        temporal.writeText(gson.toJson(borrador))
        if (!temporal.renameTo(archivo(borrador.spotId))) {
            archivo(borrador.spotId).writeText(gson.toJson(borrador))
            temporal.delete()
        }
    }

    suspend fun borrar(spotId: String) = withContext(Dispatchers.IO) {
        archivo(spotId).delete()
    }

    private fun archivo(spotId: String) = File(directorio, "${nombre(spotId)}.json")

    // Los ids son slugs ("nitro-coffee"), pero igual se limpian antes de usarlos como nombre de archivo.
    private fun nombre(spotId: String) = spotId.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
