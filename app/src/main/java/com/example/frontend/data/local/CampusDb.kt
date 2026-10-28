package com.example.frontend.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SitioGuardadoEntidad::class,
        DetalleGuardadoEntidad::class,
        TiempoEsperaEntidad::class,
        EventoPendienteEntidad::class,
        ReporteEsperaPendienteEntidad::class,
        ResenaPendienteEntidad::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class BaseDatosLocal : RoomDatabase() {
    abstract fun catalogoDao(): CatalogoDao
    abstract fun pendientesDao(): PendientesDao
    abstract fun resenasPendientesDao(): ResenasPendientesDao

    companion object {
        fun crear(context: Context): BaseDatosLocal =
            Room.databaseBuilder(context.applicationContext, BaseDatosLocal::class.java, "campusbites.db")
                .addMigrations(MIGRACION_1_2)
                // Todo lo que hay aqui es copia del backend o cola reconstruible: si cambia el esquema se recrea.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        // Version 2 solo agrega la cola de reseñas. Con migracion explicita, quien actualiza la app no pierde
        // los eventos ni los reportes de espera que tenia sin enviar (la destructiva los borraria).
        // El SQL es el mismo que genera Room para ResenaPendienteEntidad.
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `resenas_pendientes` (`id` TEXT NOT NULL, `spotId` TEXT NOT NULL, " +
                        "`estrellas` INTEGER NOT NULL, `texto` TEXT NOT NULL, `creadaEn` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
            }
        }
    }
}
