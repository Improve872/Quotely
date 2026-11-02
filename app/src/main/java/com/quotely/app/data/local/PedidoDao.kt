package com.quotely.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {
    @Query("SELECT * FROM pedidos ORDER BY creadoEn DESC")
    fun observarTodos(): Flow<List<PedidoEntity>>

    @Transaction
    @Query("SELECT * FROM pedidos WHERE id = :id LIMIT 1")
    fun observarDetalle(id: Long): Flow<PedidoConItems?>

    @Transaction
    @Query("SELECT * FROM pedidos WHERE id = :id LIMIT 1")
    suspend fun obtenerDetalle(id: Long): PedidoConItems?

    @Query("SELECT * FROM pedidos WHERE cotizacionId = :cotizacionId LIMIT 1")
    fun observarPorCotizacion(cotizacionId: Long): Flow<PedidoEntity?>

    @Query("SELECT * FROM pedidos WHERE cotizacionId = :cotizacionId LIMIT 1")
    suspend fun buscarPorCotizacion(cotizacionId: Long): PedidoEntity?

    @Query("SELECT folio FROM pedidos WHERE folio LIKE :prefijo || '%' ORDER BY LENGTH(folio) DESC, folio DESC LIMIT 1")
    suspend fun ultimoFolio(prefijo: String): String?

    @Insert
    suspend fun insertar(pedido: PedidoEntity): Long

    @Insert
    suspend fun insertarItems(items: List<PedidoItemEntity>)

    @Update
    suspend fun actualizar(pedido: PedidoEntity)

    @Query("UPDATE pedidos SET estado = :estado, actualizadoEn = :actualizadoEn WHERE id = :id")
    suspend fun actualizarEstado(id: Long, estado: String, actualizadoEn: Long)

    @Query("SELECT COUNT(*) FROM pedidos")
    suspend fun contar(): Int
}
