package com.example.frontend.data.gusto

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.frontend.model.TasteProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.datosGusto by preferencesDataStore(name = "gusto")

// Perfil de gusto y favoritos en DataStore. Antes vivian en un remember de Compose y en el ViewModel,
// asi que al cerrar la app el estudiante perdia lo que habia elegido. Son pocos valores sueltos
// (textos y conjuntos de textos) que se leen enteros al abrir la app: no hace falta una tabla de Room.
// Nada de esto pasa por la red, por eso la pantalla Taste Profile funciona igual sin conexion.
class AlmacenGusto(private val context: Context) {

    private object Claves {
        val facultad = stringPreferencesKey("facultad")
        val presupuesto = stringPreferencesKey("presupuesto")
        val dieta = stringSetPreferencesKey("dieta")
        val cocinas = stringSetPreferencesKey("cocinas")
        val picante = stringPreferencesKey("picante")
        val favoritos = stringSetPreferencesKey("favoritos")
    }

    // Lo que no se ha guardado nunca sale con los valores por defecto de TasteProfile.
    val perfil: Flow<TasteProfile> = context.datosGusto.data.map { it.aPerfil() }

    // null = el estudiante todavia no ha tocado ningun corazon; el ViewModel usa entonces los del diseño.
    // Un conjunto vacio si cuenta: quiere decir que los quito todos.
    val favoritos: Flow<Set<String>?> = context.datosGusto.data.map { it[Claves.favoritos] }

    suspend fun perfilActual(): TasteProfile = perfil.first()

    suspend fun guardarPerfil(perfil: TasteProfile) {
        context.datosGusto.edit { datos ->
            datos[Claves.facultad] = perfil.faculty
            datos[Claves.presupuesto] = perfil.budget
            datos[Claves.dieta] = perfil.dietary
            datos[Claves.cocinas] = perfil.cuisines
            datos[Claves.picante] = perfil.spice
        }
    }

    suspend fun guardarFavoritos(ids: Set<String>) {
        context.datosGusto.edit { it[Claves.favoritos] = ids }
    }

    private fun Preferences.aPerfil(): TasteProfile {
        val defecto = TasteProfile()
        return TasteProfile(
            faculty = this[Claves.facultad] ?: defecto.faculty,
            budget = this[Claves.presupuesto] ?: defecto.budget,
            dietary = this[Claves.dieta] ?: defecto.dietary,
            cuisines = this[Claves.cocinas] ?: defecto.cuisines,
            spice = this[Claves.picante] ?: defecto.spice,
        )
    }
}
