package com.example.frontend.data.remote

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.frontend.BuildConfig
import java.net.URI

// Android 17 (API 37) bloquea en silencio las conexiones a la red local si la app no tiene este permiso:
// la peticion se queda esperando hasta el timeout y la app cree que no hay red. Le pasa al backend del
// emulador (10.0.2.2) y al del PC en el mismo Wi-Fi (192.168.x.x). Un backend publicado en internet no lo
// necesita, por eso solo se pide cuando API_BASE_URL apunta a una direccion local.
const val PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK"

fun necesitaPermisoRedLocal(context: Context): Boolean =
    Build.VERSION.SDK_INT >= 37 &&
        esDireccionLocal(BuildConfig.API_BASE_URL) &&
        ContextCompat.checkSelfPermission(context, PERMISO_RED_LOCAL) != PackageManager.PERMISSION_GRANTED

// 127.0.0.1 no cuenta: el loopback del propio celular no esta restringido.
fun esDireccionLocal(url: String): Boolean {
    val host = runCatching { URI(url).host }.getOrNull() ?: return false
    val partes = host.split(".").mapNotNull { it.toIntOrNull() }
    if (partes.size == 4) {
        return partes[0] == 10 ||
            (partes[0] == 192 && partes[1] == 168) ||
            (partes[0] == 172 && partes[1] in 16..31) ||
            (partes[0] == 169 && partes[1] == 254)
    }
    return host.endsWith(".local")
}
