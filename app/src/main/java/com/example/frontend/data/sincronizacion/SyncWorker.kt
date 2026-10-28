package com.example.frontend.data.sincronizacion

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.frontend.data.Dependencias
import java.util.concurrent.TimeUnit

// Garantiza que lo pendiente llegue al backend aunque el usuario cierre la app sin conexion:
// WorkManager lo corre cuando el sistema detecta red, incluso despues de reiniciar el celular.
class TrabajoSincronizacion(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        if (Dependencias.sincronizador.enviarTodo()) Result.success() else Result.retry()

    companion object {
        private const val NOMBRE = "sincronizar-pendientes"

        // APPEND_OR_REPLACE: si ya hay una sincronizacion en curso, esta corre despues y recoge lo nuevo.
        fun programar(context: Context) {
            val pedido = OneTimeWorkRequestBuilder<TrabajoSincronizacion>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, pedido)
        }
    }
}
