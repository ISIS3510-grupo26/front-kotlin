package com.example.frontend.data

import android.content.Context
import com.example.frontend.data.conectividad.MonitorConectividad
import com.example.frontend.data.local.BaseDatosLocal
import com.example.frontend.data.remote.ApiClient
import com.example.frontend.data.remote.CampusBitesApi
import com.example.frontend.data.remote.TokenActual
import com.example.frontend.data.sesion.AlmacenSesion
import com.example.frontend.data.sincronizacion.SincronizadorPendientes
import com.example.frontend.data.sincronizacion.TrabajoSincronizacion
import com.example.frontend.data.ubicacion.ProveedorUbicacion
import com.example.frontend.data.ubicacion.UbicacionGps
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Objetos que viven lo mismo que la app y que comparten el ViewModel y el WorkManager.
// Se crean una sola vez en CampusBitesApp.onCreate().
object Dependencias {
    lateinit var baseDatos: BaseDatosLocal
        private set
    lateinit var monitorConectividad: MonitorConectividad
        private set
    lateinit var sincronizador: SincronizadorPendientes
        private set
    lateinit var almacenSesion: AlmacenSesion
        private set
    lateinit var proveedorUbicacion: ProveedorUbicacion
        private set
    lateinit var carpetaArchivos: File
        private set
    private lateinit var appContext: Context

    // Hilo de IO que vive con la app: el envio no se cancela si el usuario cambia de pantalla.
    private val scopeApp = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val api: CampusBitesApi get() = ApiClient.api

    fun iniciar(context: Context) {
        appContext = context.applicationContext
        baseDatos = BaseDatosLocal.crear(appContext)
        monitorConectividad = MonitorConectividad(appContext)
        sincronizador = SincronizadorPendientes(api, baseDatos.pendientesDao())
        almacenSesion = AlmacenSesion(appContext)
        proveedorUbicacion = ProveedorUbicacion(UbicacionGps(appContext))
        carpetaArchivos = appContext.filesDir
        // Si el backend responde 401 (token vencido o contrasena cambiada), se borra la sesion guardada.
        TokenActual.alExpirar = { scopeApp.launch { almacenSesion.borrar() } }
        // Si la app se cerro con pendientes, se intentan enviar apenas haya red.
        programarSincronizacion()
    }

    // Dos caminos: con la app abierta y red disponible se envia ya; WorkManager queda como respaldo
    // para cuando la app se cierra antes de recuperar la conexion.
    fun programarSincronizacion() {
        TrabajoSincronizacion.programar(appContext)
        if (monitorConectividad.hayRed.value) {
            scopeApp.launch { sincronizador.enviarTodo() }
        }
    }
}
