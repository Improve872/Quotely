package com.quotely.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CotizacionDao {
    @Query("SELECT * FROM cotizaciones ORDER BY creadoEn DESC")
    fun observarTodas(): Flow<List<CotizacionEntity>>

    @Query("SELECT * FROM cotizaciones WHERE id = :id LIMIT 1")
    fun observarPorId(id: Long): Flow<CotizacionEntity?>

    @Transaction
    @Query("SELECT * FROM cotizaciones WHERE id = :id LIMIT 1")
    fun observarDetalle(id: Long): Flow<CotizacionConItems?>

    @Transaction
    @Query("SELECT * FROM cotizaciones WHERE id = :id LIMIT 1")
    suspend fun obtenerDetalle(id: Long): CotizacionConItems?

    @Query("SELECT * FROM cotizacion_items WHERE cotizacionId = :id ORDER BY orden ASC")
    suspend fun obtenerItems(id: Long): List<CotizacionItemEntity>

    @Query("SELECT folio FROM cotizaciones WHERE folio LIKE :prefijo || '%' ORDER BY LENGTH(folio) DESC, folio DESC LIMIT 1")
    suspend fun ultimoFolio(prefijo: String): String?

    @Insert
    suspend fun insertar(cotizacion: CotizacionEntity): Long

    @Insert
    suspend fun insertarItems(items: List<CotizacionItemEntity>)

    @Update
    suspend fun actualizar(cotizacion: CotizacionEntity)

    @Query("DELETE FROM cotizacion_items WHERE cotizacionId = :id")
    suspend fun borrarItems(id: Long)

    @Query("UPDATE cotizaciones SET estado = :estado, actualizadoEn = :actualizadoEn WHERE id = :id")
    suspend fun actualizarEstado(id: Long, estado: String, actualizadoEn: Long)

    @Query("DELETE FROM cotizaciones WHERE id = :id AND estado = 'BORRADOR'")
    suspend fun eliminarBorrador(id: Long): Int

    @Query("SELECT COUNT(*) FROM cotizaciones")
    suspend fun contar(): Int
}
