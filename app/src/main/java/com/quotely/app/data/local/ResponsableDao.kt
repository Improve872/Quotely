package com.quotely.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ResponsableDao {
    @Query("SELECT * FROM responsables ORDER BY activo DESC, nombre ASC")
    fun observarTodos(): Flow<List<ResponsableEntity>>

    @Query("SELECT * FROM responsables WHERE id = :id LIMIT 1")
    fun observarPorId(id: Long): Flow<ResponsableEntity?>

    @Query("SELECT * FROM responsables WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ResponsableEntity?

    @Query("SELECT * FROM responsables WHERE LOWER(usuario) = LOWER(:usuario) LIMIT 1")
    suspend fun buscarPorUsuario(usuario: String): ResponsableEntity?

    @Insert
    suspend fun insertar(responsable: ResponsableEntity): Long

    @Update
    suspend fun actualizar(responsable: ResponsableEntity)

    @Query("SELECT COUNT(*) FROM responsables")
    suspend fun contar(): Int
}
