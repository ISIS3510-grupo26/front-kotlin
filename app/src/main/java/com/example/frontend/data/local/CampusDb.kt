package com.example.frontend.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SitioGuardadoEntidad::class,
        DetalleGuardadoEntidad::class,
        TiempoEsperaEntidad::class,
        EventoPendienteEntidad::class,
        ReporteEsperaPendienteEntidad::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class BaseDatosLocal : RoomDatabase() {
    abstract fun catalogoDao(): CatalogoDao
    abstract fun pendientesDao(): PendientesDao

    companion object {
        fun crear(context: Context): BaseDatosLocal =
            Room.databaseBuilder(context.applicationContext, BaseDatosLocal::class.java, "campusbites.db")
                // Todo lo que hay aqui es copia del backend o cola reconstruible: si cambia el esquema se recrea.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
