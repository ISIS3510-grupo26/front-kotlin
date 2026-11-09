package com.example.frontend

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.example.frontend.data.Dependencias
import okio.Path.Companion.toOkioPath

class CampusBitesApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        Dependencias.iniciar(this)
    }

    // Un solo ImageLoader para toda la app (Coil lo pide la primera vez que se pinta una foto).
    // Dos niveles de cache: en memoria las fotos ya decodificadas (volver al feed no las vuelve a leer)
    // y en disco los archivos descargados (se ven sin internet y no se bajan otra vez al reabrir la app).
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                // 15% de la memoria de la app: alcanza para las ~30 fotos del catalogo y su menu.
                MemoryCache.Builder().maxSizePercent(context, 0.15).build()
            }
            .diskCache {
                // 50 MB en cacheDir: cada foto pesa ~40 KB, sobra espacio. Si el sistema necesita
                // espacio puede borrar cacheDir, y las fotos se vuelven a bajar cuando haya red.
                DiskCache.Builder()
                    .directory(cacheDir.resolve("fotos_comida").toOkioPath())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
}
