package com.quotely.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos ORDER BY activo DESC, nombre ASC")
    fun observarTodos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE activo = 1 ORDER BY nombre ASC")
    fun observarActivos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ProductoEntity?

    @Query("SELECT * FROM productos WHERE LOWER(codigo) = LOWER(:codigo) LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): ProductoEntity?

    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    @Update
    suspend fun actualizar(producto: ProductoEntity)

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun contar(): Int
}
