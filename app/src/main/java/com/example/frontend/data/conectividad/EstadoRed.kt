package com.example.frontend.data.conectividad

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Avisa cuando el celular gana o pierde internet. La app no pregunta cada vez si hay red:
// el sistema le notifica el cambio y el ViewModel reacciona (reintenta lo que habia fallado).
class MonitorConectividad(context: Context) {

    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _hayRed = MutableStateFlow(redActualValida())
    val hayRed: StateFlow<Boolean> = _hayRed.asStateFlow()

    init {
        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capacidades: NetworkCapabilities) {
                _hayRed.value = tieneInternet(capacidades)
            }

            override fun onLost(network: Network) {
                _hayRed.value = false
            }
        })
    }

    private fun redActualValida(): Boolean =
        manager.getNetworkCapabilities(manager.activeNetwork)?.let(::tieneInternet) ?: false

    // No se exige VALIDATED: esa marca dice si la red llega a internet publico, y el backend puede estar
    // en la red local (el emulador o el wifi del laboratorio no la tienen). Si la red existe pero el backend
    // no responde, eso ya lo manejan los timeouts y la copia local.
    private fun tieneInternet(capacidades: NetworkCapabilities) =
        capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
