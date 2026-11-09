package com.example.frontend.ui.acciones

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.example.frontend.model.Spot
import java.util.Locale

// Botones que abren otra app del celular (mapas, compartir, ajustes). Antes tenian onClick vacio.

// "Get Walking Directions": abre Google Maps (o la app de mapas que haya) en modo caminando.
fun abrirComoLlegar(context: Context, spot: Spot) {
    val lat = spot.latitude
    val lng = spot.longitude
    val destino = if (lat != null && lng != null) {
        String.format(Locale.US, "%.6f,%.6f", lat, lng)
    } else {
        Uri.encode("${spot.name}, ${spot.location}, Universidad de los Andes, Bogota")
    }
    val url = "https://www.google.com/maps/dir/?api=1&destination=$destino&travelmode=walking"
    abrirSinFallar(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No maps app found on this phone.")
}

// Comparte el restaurante por WhatsApp, correo, etc. (el sistema muestra la lista de apps).
fun compartirSitio(context: Context, spot: Spot) {
    val texto = "Let's eat at ${spot.name} (${spot.cuisine}) · ${spot.location} · ⭐ ${spot.rating}. Found it on CampusBites."
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    abrirSinFallar(context, Intent.createChooser(envio, "Share ${spot.name}"), "No app available to share.")
}

// Ajustes de la app en el sistema: ahi se dan o quitan los permisos de ubicacion y red local.
fun abrirAjustesApp(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    abrirSinFallar(context, intent, "Couldn't open the settings.")
}

fun abrirAjustesNotificaciones(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    abrirSinFallar(context, intent, "Couldn't open the notification settings.")
}

fun mostrarAviso(context: Context, texto: String) {
    Toast.makeText(context, texto, Toast.LENGTH_SHORT).show()
}

private fun abrirSinFallar(context: Context, intent: Intent, siFalla: String) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        mostrarAviso(context, siFalla)
    }
}
