package com.example.frontend.data.ubicacion

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

enum class OrigenUbicacion { GPS, CAMPUS }

data class Ubicacion(val lat: Double, val lng: Double, val origen: OrigenUbicacion)

// Patron Strategy: "de donde saco la ubicacion" es intercambiable. Quien la pide (BQ3) solo conoce
// esta interfaz; ProveedorUbicacion elige la estrategia en tiempo de ejecucion.
interface FuenteUbicacion {
    suspend fun obtener(): Ubicacion?
}

// Estrategia 1: el sensor de ubicacion del celular (GPS o red), con LocationManager del sistema.
class UbicacionGps(private val context: Context) : FuenteUbicacion {

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun disponible(): Boolean {
        val permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return permiso && proveedor() != null
    }

    private fun proveedor(): String? = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .firstOrNull { manager.isProviderEnabled(it) }

    @SuppressLint("MissingPermission") // disponible() ya revisa el permiso antes de usar el sensor
    override suspend fun obtener(): Ubicacion? {
        if (!disponible()) return null
        val proveedor = proveedor() ?: return null
        // Una lectura de hace menos de 2 minutos sirve: evita prender el GPS (y gastar bateria) otra vez.
        val reciente = manager.getLastKnownLocation(proveedor)
            ?.takeIf { System.currentTimeMillis() - it.time < 2 * 60_000L }
        val lectura = reciente ?: withTimeoutOrNull(8_000L) { lecturaNueva(proveedor) }
        return lectura?.let { Ubicacion(it.latitude, it.longitude, OrigenUbicacion.GPS) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun lecturaNueva(proveedor: String): Location? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val cancelar = CancellationSignal()
            manager.getCurrentLocation(proveedor, cancelar, ContextCompat.getMainExecutor(context)) { cont.resume(it) }
            cont.invokeOnCancellation { cancelar.cancel() }
        } else {
            val oyente = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
            }
            manager.requestLocationUpdates(proveedor, 0L, 0f, oyente, Looper.getMainLooper())
            cont.invokeOnCancellation { manager.removeUpdates(oyente) }
        }
    }
}

// Estrategia 2: el centro del campus. Se usa si el estudiante no dio permiso o tiene el GPS apagado,
// para que la recomendacion siga funcionando (menos precisa) en vez de quedar vacia.
object UbicacionCampus : FuenteUbicacion {
    const val LAT = 4.6018
    const val LNG = -74.0658

    override suspend fun obtener() = Ubicacion(LAT, LNG, OrigenUbicacion.CAMPUS)
}

// Contexto del patron: elige la estrategia segun el estado del celular en ese momento.
class ProveedorUbicacion(
    private val gps: UbicacionGps,
    private val respaldo: FuenteUbicacion = UbicacionCampus,
) {
    private fun estrategia(): FuenteUbicacion = if (gps.disponible()) gps else respaldo

    fun usaGps(): Boolean = gps.disponible()

    // Si el GPS no responde a tiempo (bajo techo, sin senal) tambien se cae al respaldo.
    suspend fun obtener(): Ubicacion = estrategia().obtener() ?: respaldo.obtener()!!
}
