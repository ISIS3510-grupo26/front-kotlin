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
        FavoritoPendienteEntidad::class,
        AbiertoCercaEntidad::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class BaseDatosLocal : RoomDatabase() {
    abstract fun catalogoDao(): CatalogoDao
    abstract fun pendientesDao(): PendientesDao
    abstract fun resenasPendientesDao(): ResenasPendientesDao
    abstract fun favoritosDao(): FavoritosDao

    companion object {
        fun crear(context: Context): BaseDatosLocal =
            Room.databaseBuilder(context.applicationContext, BaseDatosLocal::class.java, "campusbites.db")
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
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

        // Version 3 (Sprint 4): cola de favoritos y ultima respuesta de la BQ5. Igual que la 2, solo agrega
        // tablas, asi que quien actualiza conserva sus reseñas, reportes y eventos sin enviar.
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favoritos_pendientes` (`userId` TEXT NOT NULL, `spotId` TEXT NOT NULL, " +
                        "`guardar` INTEGER NOT NULL, `eventId` TEXT NOT NULL, `cambiadoEn` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`userId`, `spotId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `abiertos_cerca` (`spotId` TEXT NOT NULL, `userId` TEXT NOT NULL, " +
                        "`nombre` TEXT NOT NULL, `emoji` TEXT NOT NULL, `lat` REAL NOT NULL, `lng` REAL NOT NULL, " +
                        "`metros` INTEGER NOT NULL, `minutos` INTEGER NOT NULL, `cierraA` TEXT NOT NULL, " +
                        "`cierraEnMillis` INTEGER NOT NULL, `guardadoEn` INTEGER NOT NULL, PRIMARY KEY(`spotId`))",
                )
            }
        }
    }
}
