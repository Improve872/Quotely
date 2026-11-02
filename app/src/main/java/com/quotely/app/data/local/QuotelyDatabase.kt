package com.quotely.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ResponsableEntity::class,
        ProductoEntity::class,
        CotizacionEntity::class,
        CotizacionItemEntity::class,
        PedidoEntity::class,
        PedidoItemEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class QuotelyDatabase : RoomDatabase() {
    abstract fun responsableDao(): ResponsableDao
    abstract fun productoDao(): ProductoDao
    abstract fun cotizacionDao(): CotizacionDao
    abstract fun pedidoDao(): PedidoDao

    companion object {
        private const val NOMBRE = "quotely.db"

        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE pedidos ADD COLUMN tipoDescuentoGlobal TEXT NOT NULL DEFAULT 'NINGUNO'"
                )
                db.execSQL(
                    "ALTER TABLE pedidos ADD COLUMN valorDescuentoGlobal REAL NOT NULL DEFAULT 0"
                )
            }
        }

        @Volatile
        private var instancia: QuotelyDatabase? = null

        fun obtener(context: Context): QuotelyDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuotelyDatabase::class.java,
                    NOMBRE
                )
                    .addMigrations(MIGRACION_1_2)
                    .build()
                    .also { instancia = it }
            }
    }
}
